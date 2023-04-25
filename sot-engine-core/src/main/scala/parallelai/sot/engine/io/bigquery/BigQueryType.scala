package parallelai.sot.engine.io.bigquery

import com.google.api.services.bigquery.model.{TableRow, TableSchema}
import shapeless._

import scala.collection.JavaConverters._

class BigQueryType extends Serializable {

  def toTableRow[L <: HList](a: L)
                            (implicit toL: ToTableRow[L])
  : TableRow = {
    val tr = new TableRow()
    tr.putAll(toL(a))
    tr
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