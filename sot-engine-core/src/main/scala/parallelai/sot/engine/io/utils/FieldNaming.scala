package parallelai.sot.engine.io.utils

import parallelai.sot.executor.model.SOTMacroConfig.{BigQueryTapDefinition, DatastoreTapDefinition}

trait FieldNaming[TAP] extends Serializable {
  def apply(field: String): String
}
object FieldNaming {

  val field = "_(\\d+)".r

  def renameTupleUnderscore(name: String): String = {
    name match {
      case field(numbers) => "v" + numbers.mkString("")
      case _ => name
    }
  }

  implicit def fieldCleanseBQ: FieldNaming[BigQueryTapDefinition] = new FieldNaming[BigQueryTapDefinition] {
    override def apply(field: String): String = FieldNaming.renameTupleUnderscore(field)
  }

  implicit def fieldCleanseDatastore: FieldNaming[DatastoreTapDefinition] = new FieldNaming[DatastoreTapDefinition] {
    override def apply(field: String): String = field
  }

}
