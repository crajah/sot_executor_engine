package parallelai.sot.engine.io

import com.google.api.services.bigquery.model.{TableRow, TableSchema}
import parallelai.sot.engine.io.datatype.{CanNest, FromMappable, ToMappable}
import shapeless._

import scala.collection.JavaConverters._

package object bigquery extends BigQueryMappableType with Serializable {
  type BigQueryMap = java.util.Map[String, Any]
  type FromTableRow[L <: HList] = FromMappable[L, BigQueryMap]
  type ToTableRow[L <: HList] = ToMappable[L, BigQueryMap]

  implicit object BigQueryCanNest extends CanNest[BigQueryMap]

  implicit class ConvertBQ[A <: HList](a: A) extends Serializable {

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