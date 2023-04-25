package parallelai.sot.engine.io.bigtable

import com.google.protobuf.ByteString
import com.spotify.scio.bigtable._
import shapeless.Poly1

object MapMutation extends Poly1 with Serializable {

  implicit def caseInt = at[(String, String, Int)] {
    case (famName, colName, v) =>
      val col = ByteString.copyFromUtf8(colName)
      Mutations.newSetCell(famName, col, ByteString.copyFromUtf8(v.toString))
  }

  implicit def caseString = at[(String, String, String)] {
    case (famName, colName, v) =>
      val col = ByteString.copyFromUtf8(colName)
      Mutations.newSetCell(famName, col, ByteString.copyFromUtf8(v))
  }

  implicit def caseLong = at[(String, String, Long)] {
    case (famName, colName, v) =>
      val col = ByteString.copyFromUtf8(colName)
      Mutations.newSetCell(famName, col, ByteString.copyFromUtf8(v.toString))
  }

  implicit def caseDouble = at[(String, String, Double)] {
    case (famName, colName, v) =>
      val col = ByteString.copyFromUtf8(colName)
      Mutations.newSetCell(famName, col, ByteString.copyFromUtf8(v.toString))
  }

//  def fromRow(r: Row): String = {
//    val FAMILY_NAME: String = "count"
//    val COLUMN_QUALIFIER: ByteString = ByteString.copyFromUtf8("long")
//    r.getValue(FAMILY_NAME, COLUMN_QUALIFIER).get.toStringUtf8.toLong
//  }

}
