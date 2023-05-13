package parallelai.sot.engine.io

import com.google.api.services.bigquery.model.{TableRow, TableSchema}
import shapeless._
import shapeless.datatype.mappable.{CanNest, FromMappable}

import scala.collection.JavaConverters._

package object bigquery extends BigQueryMappableType with Serializable {
  type BigQueryMap = java.util.Map[String, Any]
  type FromTableRow[L <: HList] = FromMappable[L, BigQueryMap]
  type ToTableRow[L <: HList] = ToMappableBQ[L, BigQueryMap]

  implicit object BigQueryCanNest extends CanNest[BigQueryMap]

  implicit class Convert[A <: HList](a: A) extends Serializable {

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