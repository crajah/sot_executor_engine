package parallelai.sot.engine.io.bigquery

import com.google.api.services.bigquery.model.TableSchema
import shapeless._

import scala.collection.JavaConverters._

object BigQueryType {

  def at[T](fromFn: Any => T, toFn: T => Any) = new BaseBigQueryMappableType[T] {
    override def from(value: Any): T = fromFn(value)

    override def to(value: T): Any = toFn(value)
  }

}

class BigQuerySchemaProvider[A <: HList] {
  def getSchema(implicit hListSchemaProvider: HListSchemaProvider[A]): TableSchema = {
    new TableSchema().setFields(hListSchemaProvider.getSchema.toList.asJava)
  }
}

object BigQuerySchemaProvider {
  def apply[A <: HList]: BigQuerySchemaProvider[A] = new BigQuerySchemaProvider[A]
}