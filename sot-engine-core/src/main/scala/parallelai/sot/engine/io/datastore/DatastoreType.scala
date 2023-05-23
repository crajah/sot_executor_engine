package parallelai.sot.engine.io.datastore

import com.google.datastore.v1.client.DatastoreHelper.makeValue
import com.google.datastore.v1.{Entity, Value}
import com.google.protobuf.{ByteString, Timestamp}
import org.joda.time.{DateTimeConstants, Instant}
import shapeless._
import shapeless.labelled.FieldType

class DatastoreType[A] extends Serializable {
  def fromEntityBuilder[L <: HList](m: Entity.Builder)
                                   (implicit
                                    gen: LabelledGeneric.Aux[A, L],
                                    fromL: FromEntity[L]): Option[A] =
    fromL(m).map(gen.from)

  def fromEntity[L <: HList](m: Entity)
                            (implicit
                             gen: LabelledGeneric.Aux[A, L],
                             fromL: FromEntity[L]) : Option[A] =
    fromL(m.toBuilder).map(gen.from)

  def toEntityBuilder[L <: HList](a: A)
                                 (implicit
                                  gen: LabelledGeneric.Aux[A, L],
                                  toL: ToEntity[L]): Entity.Builder =
    toL(gen.to(a))

  def toEntity[L <: HList](a: A)
                          (implicit
                           gen: LabelledGeneric.Aux[A, L],
                           toL: ToEntity[L]): Entity =
    toL(gen.to(a)).build()
}

object DatastoreType {
  def apply[A]: DatastoreType[A] = new DatastoreType[A]

  def at[V](fromFn: Value => V, toFn: V => Value): BaseDatastoreMappableType[V] =
    new BaseDatastoreMappableType[V] {
      override def from(value: Value): V = fromFn(value)
      override def to(value: V): Value = toFn(value)
    }

  def toEntityBuilder[L <: HList](a: L)
                                 (implicit
                                  toL: ToEntity[L]): Entity.Builder =
    toL(a)

  def toEntity[L <: HList](a: L)
                          (implicit
                           toL: ToEntity[L]): Entity =
    toL(a).build()
}

trait DatastoreMappableTypes {

  implicit def nestedHListToMappable[K <: Symbol, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], mbt: BaseMappableType[M],
   toH: Lazy[ToMappable[H, M]], toT: Lazy[ToMappable[T, M]]): ToMappable[FieldType[K, H] :: T, M] =
    new ToMappable[FieldType[K, H] :: T, M] {
    override def apply(l: FieldType[K, H] :: T): M =
      mbt.put(wit.value.name, toH.value(l.head), toT.value(l.tail))
  }

  implicit def nestedHListOptionToMappable[K <: Symbol, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], mbt: BaseMappableType[M],
   toH: Lazy[ToMappable[H, M]], toT: Lazy[ToMappable[T, M]]): ToMappable[FieldType[K, Option[H]] :: T, M] =
    new ToMappable[FieldType[K, Option[H]] :: T, M] {
    override def apply(l: FieldType[K, Option[H]] :: T): M =
      mbt.put(wit.value.name, l.head.map(h => toH.value.apply(h)), toT.value(l.tail))
  }

  implicit def nestedHListListToMappable[K <: Symbol, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], mbt: BaseMappableType[M],
   toH: Lazy[ToMappable[H, M]], toT: Lazy[ToMappable[T, M]]): ToMappable[FieldType[K, List[H]] :: T, M] =
    new ToMappable[FieldType[K, List[H]] :: T, M] {
    override def apply(l: FieldType[K, List[H]] :: T): M =
      mbt.put(wit.value.name, l.head.map(h => toH.value.apply(h)), toT.value(l.tail))
  }

  import DatastoreType.at

  implicit val booleanEntityMappableType = at[Boolean](_.getBooleanValue, makeValue(_).build())
  implicit val intDatastoreMappableType = at[Int](_.getIntegerValue.toInt, makeValue(_).build())
  implicit val longEntityMappableType = at[Long](_.getIntegerValue, makeValue(_).build())
  implicit val floatEntityMappableType = at[Float](_.getDoubleValue.toFloat, makeValue(_).build())
  implicit val doubleEntityMappableType = at[Double](_.getDoubleValue, makeValue(_).build())
  implicit val stringEntityMappableType = at[String](_.getStringValue, makeValue(_).build())
  implicit val byteStringEntityMappableType = at[ByteString](_.getBlobValue, makeValue(_).build())
  implicit val byteArrayEntityMappableType = at[Array[Byte]](_.getBlobValue.toByteArray, v => makeValue(ByteString.copyFrom(v)).build())
  implicit val timestampEntityMappableType = at[Instant](toInstant, fromInstant)

  private def toInstant(v: Value): Instant = {
    val t = v.getTimestampValue
    new Instant(t.getSeconds * DateTimeConstants.MILLIS_PER_SECOND + t.getNanos / 1000000)
  }

  private def fromInstant(i: Instant): Value = {
    val t = Timestamp.newBuilder()
      .setSeconds(i.getMillis / DateTimeConstants.MILLIS_PER_SECOND)
      .setNanos((i.getMillis % 1000).toInt * 1000000)
    Value.newBuilder().setTimestampValue(t).build()
  }
}
