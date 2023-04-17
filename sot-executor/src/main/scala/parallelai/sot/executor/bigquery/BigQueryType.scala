package parallelai.sot.executor.bigquery

import com.google.api.services.bigquery.model.TableRow
import shapeless._

class BigQueryType[A] extends Serializable {
  def fromTableRow[L <: HList](m: TableRow)
                              (implicit gen: LabelledGeneric.Aux[A, L], fromL: FromTableRow[L])
  : Option[A] = fromL(m.asInstanceOf[BigQueryMap]).map(gen.from)
  def toTableRow[L <: HList](a: A)
                            (implicit gen: LabelledGeneric.Aux[A, L], toL: ToTableRow[L])
  : TableRow = {
    val tr = new TableRow()
    tr.putAll(toL(gen.to(a)))
    tr
  }
}

object BigQueryType {
  def apply[A]: BigQueryType[A] = new BigQueryType[A]

}