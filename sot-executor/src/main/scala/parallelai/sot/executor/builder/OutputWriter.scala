package parallelai.sot.executor.builder

import com.google.api.services.bigquery.model.{TableFieldSchema, TableRow, TableSchema}
import com.google.protobuf.ByteString
import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import parallelai.sot.executor.bigquery.{BigQueryType, ToTableRow}
import parallelai.sot.executor.model.SOTMacroConfig.{BigQueryTapDefinition, PubSubTapDefinition}
import parallelai.sot.executor.scio.PaiScioContext._
import shapeless.{::, HList, HNil}

import scala.reflect.runtime.universe._
import scala.collection.JavaConverters._
import org.joda.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import shapeless.labelled.FieldType

import scala.reflect.macros.blackbox

private object MacroUtil {

  // Case class helpers for runtime reflection

  def isCaseClass(t: Type): Boolean =
    !t.toString.startsWith("scala.") &&
      List(typeOf[Product], typeOf[Serializable], typeOf[Equals])
        .forall(b => t.baseClasses.contains(b.typeSymbol))

  def isField(s: Symbol): Boolean = s.isPublic && s.isMethod && !s.isSynthetic && !s.isConstructor

  // Case class helpers for macros

  def isCaseClass(c: blackbox.Context)(t: c.Type): Boolean = {
    import c.universe._
    !t.toString.startsWith("scala.") &&
      List(typeOf[Product], typeOf[Serializable], typeOf[Equals])
        .forall(b => t.baseClasses.contains(b.typeSymbol))
  }

  def isField(c: blackbox.Context)(s: c.Symbol): Boolean =
    s.isPublic && s.isMethod && !s.isSynthetic && !s.isConstructor
  def getFields(c: blackbox.Context)(t: c.Type): Iterable[c.Symbol] = {
    import c.universe._
    val fields = t.decls.filter(isField(c))
    // if type was macro generated it should have bigquery tag on it
    if (t.typeSymbol.annotations.exists(_.tree.tpe =:= typeOf[com.spotify.scio.bigquery.types.BigQueryTag])) {
      fields
        .filter { s =>
          try {
            val AnnotatedType(a, _) = s.asMethod.returnType
            a.exists(_.tree.tpe == typeOf[com.spotify.scio.bigquery.types.BigQueryTag])
          } catch {
            case _: MatchError => false
          }
        }
    } else {
      fields
    }
  }

  // Namespace helpers

  val SBQ = "_root_.com.spotify.scio.bigquery"
  val GModel = "_root_.com.google.api.services.bigquery.model"
  val GBQIO = "_root_.org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO"
  val SType = s"$SBQ.types.BigQueryType"
  val SUtil = s"$SBQ.BigQueryUtil"

  def p(c: blackbox.Context, code: String): c.Tree = c.parse(code)
}

object SchemaProvider {

  def schemaOf[T: TypeTag]: TableSchema = {
    val fields = typeOf[T].erasure match {
      case t if MacroUtil.isCaseClass(t) => toFields(t)
      case t => throw new RuntimeException(s"Unsupported type $t")
    }
    val r = new TableSchema().setFields(fields.toList.asJava)
    r
  }

  private def field(mode: String, name: String, tpe: String, desc: Option[String],
                    nested: Iterable[TableFieldSchema]): TableFieldSchema = {
    val s = new TableFieldSchema().setMode(mode).setName(name).setType(tpe)
    if (nested.nonEmpty) {
      s.setFields(nested.toList.asJava)
    }
    desc.foreach(s.setDescription)
    s
  }

  // scalastyle:off cyclomatic.complexity
  private def rawType(tpe: Type): (String, Iterable[TableFieldSchema]) = tpe match {
    case t if t =:= typeOf[Boolean] => ("BOOLEAN", Iterable.empty)
    case t if t =:= typeOf[Int] => ("INTEGER", Iterable.empty)
    case t if t =:= typeOf[Long] => ("INTEGER", Iterable.empty)
    case t if t =:= typeOf[Float] => ("FLOAT", Iterable.empty)
    case t if t =:= typeOf[Double]  => ("FLOAT", Iterable.empty)
    case t if t =:= typeOf[String] => ("STRING", Iterable.empty)

    case t if t =:= typeOf[ByteString] => ("BYTES", Iterable.empty)
    case t if t =:= typeOf[Array[Byte]] => ("BYTES", Iterable.empty)

    case t if t =:= typeOf[Instant] => ("TIMESTAMP", Iterable.empty)
    case t if t =:= typeOf[LocalDate] => ("DATE", Iterable.empty)
    case t if t =:= typeOf[LocalTime] => ("TIME", Iterable.empty)
    case t if t =:= typeOf[LocalDateTime] => ("DATETIME", Iterable.empty)

    case t if MacroUtil.isCaseClass(t) => ("RECORD", toFields(t))
    case _ => throw new RuntimeException(s"Unsupported type: $tpe")
  }
  // scalastyle:on cyclomatic.complexity

  private def toField(f: (Symbol, Option[String])): TableFieldSchema = {
    val (symbol, desc) = f
    val name = symbol.name.toString
    val tpe = symbol.asMethod.returnType

    val (mode, valType) = tpe match {
      case t if t.erasure =:= typeOf[Option[_]].erasure => ("NULLABLE", tpe.typeArgs.head)
      case t if t.erasure =:= typeOf[List[_]].erasure => ("REPEATED", tpe.typeArgs.head)
      case _ => ("REQUIRED", tpe)
    }
    val (tpeParam, nestedParam) = rawType(valType)
    field(mode, name, tpeParam, desc, nestedParam)
  }

  private def toFields(t: Type): Iterable[TableFieldSchema] = getFields(t).map(toField)

  private def getFields(t: Type): Iterable[(Symbol, Option[String])] =
    t.decls.filter(MacroUtil.isField) zip fieldDescs(t)

  private def fieldDescs(t: Type): Iterable[Option[String]] = {
    val tpe = "com.spotify.scio.bigquery.types.description"
    t.typeSymbol.asClass.primaryConstructor.typeSignature.paramLists.head.map {
      _.annotations
        .find(_.tree.tpe.toString == tpe)
        .map { a =>
          val q"new $t($v)" = a.tree
          val Literal(Constant(s)) = v
          s.toString
        }
    }
  }

}

trait OutputWriter[T, C, A] {
  def write[Out <: A : Manifest](sCollection: SCollection[Out], tap: T, config: C): Unit
}

object OutputWriter {

  def apply[T, C, A](implicit outputWriter: OutputWriter[T, C, A]) = outputWriter

  implicit class ToTableRowCC[L <: HList](a: L) {
    def toTableRowSC(implicit toL: ToTableRow[L])
    : TableRow = {
      val tr = new TableRow()
      tr.putAll(toL(a))
      tr
    }
  }

  implicit def bigQueryWriter = new OutputWriter[BigQueryTapDefinition, GcpOptions, HasAnnotation] {
    def write[Out <: HasAnnotation : Manifest]
    (sCollection: SCollection[Out], tap: BigQueryTapDefinition, config: GcpOptions): Unit = {
      sCollection.saveAsTypedBigQuery(s"${tap.dataset}.${tap.table}")
    }
  }

//  implicit def bigQueryWriter = new OutputWriter[BigQueryTapDefinition, GcpOptions, HList] {
//    def write[Out <: HasAnnotation : Manifest]
//    (sCollection: SCollection[Out], tap: BigQueryTapDefinition, config: GcpOptions): Unit = {

//      val t = BigQueryType[Out]
//      val r = t.toTableRow(City("New York", "NYC", 40.730610, -73.935242))
//      val c = t.fromTableRow(r)
//
//      val bqt = BigQueryType[Out]
//      bqt.schema
//
//      import scala.concurrent.ExecutionContext.Implicits.global
//      sCollection
//        .map(m => m.toTableRowSC)
//        .saveAsBigQuery(
//          "table",
//          bqt.schema,
//          null,
//          bull,
//          bqt.tableDescription.orNull)
//        .map(_.map(bqt.fromTableRow))

//      sCollection.saveAsTypedBigQuery(s"${tap.dataset}.${tap.table}")
//    }
//  }

  implicit def pubSubWriter = new OutputWriter[PubSubTapDefinition, GcpOptions, HasAvroAnnotation] {
    def write[Out <: HasAvroAnnotation : Manifest]
    (sCollection: SCollection[Out], tap: PubSubTapDefinition, config: GcpOptions): Unit = {
      sCollection.saveAsPubsub(tap.topic)
    }
  }

}

trait Writer[TAP, CONFIG, ANNO, TOUT] {
  def write(sc: SCollection[TOUT], tap: TAP, config: CONFIG)(implicit m: Manifest[TOUT]): Unit
}

object Writer {
  def apply[TAP, CONFIG, ANNO, TOUT](implicit reader: Writer[TAP, CONFIG, ANNO, TOUT]) = reader

  implicit def pubSubAvroWrtier[T0 <: HasAvroAnnotation]: Writer[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] = new Writer[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] {
    def write(sCollection: SCollection[T0], tap: PubSubTapDefinition, config: GcpOptions)(implicit m: Manifest[T0]): Unit = {
      sCollection.saveAsTypedPubSub(config.getProject, tap.topic)
    }
  }

  implicit def bigqueryWriter[T0 <: HasAnnotation]: Writer[BigQueryTapDefinition, GcpOptions, HasAnnotation, T0] = new Writer[BigQueryTapDefinition, GcpOptions, HasAnnotation, T0] {
    def write(sCollection: SCollection[T0], tap: BigQueryTapDefinition, config: GcpOptions)(implicit m: Manifest[T0]): Unit = {
      sCollection.saveAsTypedBigQuery(s"${tap.dataset}.${tap.table}")
    }
  }
}
