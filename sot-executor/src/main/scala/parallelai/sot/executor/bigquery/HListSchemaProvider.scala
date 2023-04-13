package parallelai.sot.executor.bigquery

import com.google.api.services.bigquery.model.TableFieldSchema
import shapeless.{::, HList, HNil, Witness}
import shapeless.labelled.FieldType

trait HListSchemaExtractor[A] {

  def apply: String

}

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


object HListSchemaExtractor {

  implicit val booleanExtractor = new HListSchemaExtractor[Boolean] {def apply = "BOOLEAN"}
  implicit val intExtractor = new HListSchemaExtractor[Int] {def apply = "INTEGER"}
  implicit val longExtractor = new HListSchemaExtractor[Long] {def apply = "INTEGER"}
  implicit val floatExtractor = new HListSchemaExtractor[Float] {def apply = "FLOAT"}
  implicit val doubleExtractor = new HListSchemaExtractor[Double] {def apply = "FLOAT"}
  implicit val stringExtractor = new HListSchemaExtractor[String] {def apply = "STRING"}

  implicit val byteStringExtractor = new HListSchemaExtractor[ByteString] {def apply = "BYTES"}
  implicit val byteArrayExtractor = new HListSchemaExtractor[Array[Byte]] {def apply = "BYTES"}

  implicit val instantExtractor = new HListSchemaExtractor[Instant] {def apply = "BYTES"}


}

trait HListSchemaProvider[A <: HList] {

  def apply(a : A): Iterable[TableFieldSchema]

}

object HListSchemaProvider {

  def apply[A <: HList](implicit p: HListSchemaProvider[A]): HListSchemaProvider[A] = p

  implicit object hconsNil extends HListSchemaProvider[HNil] {
    override def apply(a: HNil): Iterable[TableFieldSchema] = Iterable.empty
  }

  implicit def hconsFieldParser[K <: Symbol, V, T <: HList](implicit
                                                            hExtractor: HListSchemaExtractor[V],
                                                            witness: Witness.Aux[K],
                                                            tSchemaProvider: HListSchemaProvider[T]) = new HListSchemaProvider[FieldType[K, V] :: T] {
    def apply(a: FieldType[K, V] :: T): Iterable[TableFieldSchema] = {
      val name = witness.value.name
      val tpeParam = hExtractor.apply
      Iterable(new TableFieldSchema().setMode("REQUIRED").setName(name).setType(tpeParam)) ++ tSchemaProvider(a.tail)
    }
  }

}
