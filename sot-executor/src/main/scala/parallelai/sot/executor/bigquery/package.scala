package parallelai.sot.executor

import com.google.api.services.bigquery.model.{TableRow, TableSchema}
import shapeless._
import shapeless.datatype.mappable.{CanNest, FromMappable, ToMappable}
import scala.collection.JavaConverters._

package object bigquery extends BigQueryMappableType {
  type BigQueryMap = java.util.Map[String, Any]
  type FromTableRow[L <: HList] = FromMappable[L, BigQueryMap]
  type ToTableRow[L <: HList] = ToMappable[L, BigQueryMap]

  implicit object BigQueryCanNest extends CanNest[BigQueryMap]

  implicit class Convert[A <: HList](a: A) {

    def convert(implicit hListSchemaProvider: HListSchemaProvider[A]): TableSchema = {
      new TableSchema().setFields(hListSchemaProvider.getSchema.toList.asJava)
    }

    def toTableRow(implicit toL: ToTableRow[A]): TableRow = {
      val tr = new TableRow()
      tr.putAll(toL(a))
      tr
    }

    def getSchema(implicit hListSchemaProvider: HListSchemaProvider[A]): TableSchema = {
      new TableSchema().setFields(hListSchemaProvider.getSchema.toList.asJava)
    }

  }

}