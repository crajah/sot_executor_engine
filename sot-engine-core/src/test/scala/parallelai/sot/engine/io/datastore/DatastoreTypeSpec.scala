//package parallelai.sot.engine.io.datastore
//
//import java.net.URI
//import shapeless._
//import shapeless.datatype.record._
//import org.scalacheck.Prop.{all, forAll}
//import org.scalacheck.ScalacheckShapeless._
//import org.scalacheck._
//import com.google.datastore.v1.client.DatastoreHelper._
//
//object DatastoreTypeTest extends Properties("DatastoreType") {
//
//  import parallelai.sot.engine.io.datastore.Records._
//  import parallelai.sot.engine.io.utils.SerializableUtils._
//
//  implicit def compareByteArrays(x: Array[Byte], y: Array[Byte]) = java.util.Arrays.equals(x, y)
//
//  implicit def compareIntArrays(x: Array[Int], y: Array[Int]) = java.util.Arrays.equals(x, y)
//
//  def roundTrip[A, L <: HList](m: A)
//                              (implicit
//                               gen: LabelledGeneric.Aux[A, L],
//                               fromL: FromEntity[L],
//                               toL: ToEntity[L],
//                               mr: MatchRecord[L]): Prop = {
//    val t = ensureSerializable(DatastoreType[A])
//    val rm = RecordMatcher[A]
//    all(
//      t.fromEntityBuilder(t.toEntityBuilder(m)).exists(rm(_, m)),
//      t.fromEntity(t.toEntity(m)).exists(rm(_, m)),
//      t.fromEntityBuilder(t.toEntityBuilder(m)).exists(rm(_, m)),
//      t.fromEntity(t.toEntity(m)).exists(rm(_, m))
//    )
//  }
//
//  property("required") = forAll { m: Required => roundTrip(m) }
//  property("optional") = forAll { m: Optional => roundTrip(m) }
//  property("repeated") = forAll { m: Repeated => roundTrip(m) }
//  property("mixed") = forAll { m: Mixed => roundTrip(m) }
//  property("nested") = forAll { m: Nested => roundTrip(m) }
//  property("seqs") = forAll { m: Seqs => roundTrip(m) }
//
//  implicit val uriDatastoreType = DatastoreType.at[URI](
//    v => URI.create(v.getStringValue), u => makeValue(u.toString).build())
//  property("custom") = forAll { m: Custom => roundTrip(m) }
//
//}
