package parallelai.sot.executor.bigquery

import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.protobuf.ByteString
import org.joda.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import shapeless.{::, HList, HNil, Lazy, Witness}
import shapeless.labelled.FieldType

import scala.collection.JavaConverters._

trait HListSchemaExtractor[A] {

  def apply: String

}


object HListSchemaExtractor {

  implicit val booleanExtractor = new HListSchemaExtractor[Boolean] {def apply = "BOOLEAN"}
  implicit val intExtractor = new HListSchemaExtractor[Int] {def apply = "INTEGER"}
  implicit val longExtractor = new HListSchemaExtractor[Long] {def apply = "INTEGER"}
  implicit val floatExtractor = new HListSchemaExtractor[Float] {def apply = "FLOAT"}
  implicit val doubleExtractor = new HListSchemaExtractor[Double] {def apply = "FLOAT"}
  implicit val stringExtractor = new HListSchemaExtractor[String] {def apply = "STRING"}

  implicit val byteStringExtractor = new HListSchemaExtractor[ByteString] {def apply = "BYTES"}
  implicit val byteArrayExtractor = new HListSchemaExtractor[Array[Byte]] {def apply = "BYTES"}

  implicit val instantExtractor = new HListSchemaExtractor[Instant] {def apply = "TIMESTAMP"}
  implicit val localDateExtractor = new HListSchemaExtractor[LocalDate] {def apply = "DATE"}
  implicit val localTimeExtractor = new HListSchemaExtractor[LocalTime] {def apply = "TIME"}
  implicit val localDateTimeExtractor = new HListSchemaExtractor[LocalDateTime] {def apply = "DATETIME"}

}

trait HListSchemaProvider[A <: HList] {

  def getSchema: Iterable[TableFieldSchema]

}

trait LowPrioritySchemaProvider {

  def apply[A <: HList](implicit p: HListSchemaProvider[A]): HListSchemaProvider[A] = p

  implicit def optionProductParser[K <: Symbol, V <: HList, T <: HList](implicit
                                                                  hNestedProvider: HListSchemaProvider[V],
                                                                  witness: Witness.Aux[K],
                                                                  tSchemaProvider: HListSchemaProvider[T]) =
    new HListSchemaProvider[FieldType[K, Option[V]] :: T] {
      def getSchema: Iterable[TableFieldSchema] = {
        val name = witness.value.name
        val nestedFields = hNestedProvider.getSchema
        val nestedSchema = new TableFieldSchema()
          .setMode("NULLABLE")
          .setName(name)
          .setType("RECORD")
          .setFields(nestedFields.toList.asJava)
        Iterable(nestedSchema) ++ tSchemaProvider.getSchema
      }
    }

  implicit def listProductParser[K <: Symbol, V <: HList, T <: HList](implicit
                                                                        hNestedProvider: HListSchemaProvider[V],
                                                                        witness: Witness.Aux[K],
                                                                        tSchemaProvider: HListSchemaProvider[T]) =
    new HListSchemaProvider[FieldType[K, List[V]] :: T] {
      def getSchema: Iterable[TableFieldSchema] = {
        val name = witness.value.name
        val nestedFields = hNestedProvider.getSchema
        val nestedSchema = new TableFieldSchema()
          .setMode("REPEATED")
          .setName(name)
          .setType("RECORD")
          .setFields(nestedFields.toList.asJava)
        Iterable(nestedSchema) ++ tSchemaProvider.getSchema
      }
    }

  implicit def productParser[K <: Symbol, V <: HList, T <: HList](implicit
                                                                  hNestedProvider: HListSchemaProvider[V],
                                                                  witness: Witness.Aux[K],
                                                                  tSchemaProvider: HListSchemaProvider[T]) =
    new HListSchemaProvider[FieldType[K, V] :: T] {
      def getSchema: Iterable[TableFieldSchema] = {
        val name = witness.value.name
        val nestedFields = hNestedProvider.getSchema
        val nestedSchema = new TableFieldSchema()
          .setMode("REQUIRED")
          .setName(name)
          .setType("RECORD")
          .setFields(nestedFields.toList.asJava)
        Iterable(nestedSchema) ++ tSchemaProvider.getSchema
      }
    }

}

object HListSchemaProvider extends LowPrioritySchemaProvider {

  implicit object hnilParser extends HListSchemaProvider[HNil] {
    override def getSchema: Iterable[TableFieldSchema] = Iterable.empty
  }

  implicit def requiredFieldParser[K <: Symbol, V, T <: HList](implicit
                                                       hExtractor: HListSchemaExtractor[V],
                                                       witness: Witness.Aux[K],
                                                       tSchemaProvider: HListSchemaProvider[T]) =
    new HListSchemaProvider[FieldType[K, V] :: T] {
      def getSchema: Iterable[TableFieldSchema] = {
        val name = witness.value.name
        val tpeParam = hExtractor.apply
        Iterable(new TableFieldSchema().setMode("REQUIRED").setName(name).setType(tpeParam)) ++ tSchemaProvider.getSchema
      }
    }

  implicit def listFieldParser[K <: Symbol, V, T <: HList](implicit
                                                               hExtractor: HListSchemaExtractor[V],
                                                               witness: Witness.Aux[K],
                                                               tSchemaProvider: HListSchemaProvider[T]) =
    new HListSchemaProvider[FieldType[K, List[V]] :: T] {
      def getSchema: Iterable[TableFieldSchema] = {
        val name = witness.value.name
        val tpeParam = hExtractor.apply
        Iterable(new TableFieldSchema().setMode("REPEATED").setName(name).setType(tpeParam)) ++ tSchemaProvider.getSchema
      }
    }

  implicit def optionalFieldParser[K <: Symbol, V, T <: HList](implicit
                                                           hExtractor: HListSchemaExtractor[V],
                                                           witness: Witness.Aux[K],
                                                           tSchemaProvider: HListSchemaProvider[T]) =
    new HListSchemaProvider[FieldType[K, Option[V]] :: T] {
      def getSchema: Iterable[TableFieldSchema] = {
        val name = witness.value.name
        val tpeParam = hExtractor.apply
        Iterable(new TableFieldSchema().setMode("NULLABLE").setName(name).setType(tpeParam)) ++ tSchemaProvider.getSchema
      }
    }

}
