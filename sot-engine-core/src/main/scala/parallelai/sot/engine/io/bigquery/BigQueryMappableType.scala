package parallelai.sot.engine.io.bigquery

import com.google.common.io.BaseEncoding
import com.google.protobuf.ByteString
import com.trueaccord.scalapb.GeneratedEnum
import org.joda.time._
import org.joda.time.format.{DateTimeFormat, DateTimeFormatterBuilder}
import shapeless.datatype.mappable.{BaseMappableType, MappableType, ToMappable}
import shapeless.labelled.FieldType
import shapeless.{::, HList, Lazy, Witness}

import scala.collection.JavaConverters._

trait BaseBigQueryMappableTypeBQ[V] extends MappableTypeBQ[BigQueryMap, V] {
  def from(value: Any): V

  def to(value: V): Any

  override def get(m: BigQueryMap, key: String): Option[V] =
    Option(m.get(key)).map(from)

  override def getAll(m: BigQueryMap, key: String): Seq[V] =
    if (m.containsKey(key))
      m.get(key).asInstanceOf[java.util.List[Any]].asScala.map(from)
    else
      Nil

  override def put(key: String, value: V, tail: BigQueryMap): BigQueryMap = {
    tail.put(key, to(value))
    tail
  }

  override def put(key: String, value: Option[V], tail: BigQueryMap): BigQueryMap = {
    value.foreach(v => tail.put(key, to(v)))
    tail
  }

  override def put(key: String, values: Seq[V], tail: BigQueryMap): BigQueryMap = {
    tail.put(key, values.map(to).asJava)
    tail
  }
}

trait BigQueryMappableType {

  implicit def nestedHListToMappable[K <: Symbol, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], mbt: BaseMappableTypeBQ[M],
   toH: Lazy[ToMappableBQ[H, M]], toT: Lazy[ToMappableBQ[T, M]])
  : ToMappableBQ[FieldType[K, H] :: T, M] = new ToMappableBQ[FieldType[K, H] :: T, M] {
    override def apply(l: FieldType[K, H] :: T): M =
      mbt.put(wit.value.name, toH.value(l.head), toT.value(l.tail))
  }

  implicit def nestedHListOptionToMappable[K <: Symbol, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], mbt: BaseMappableTypeBQ[M],
   toH: Lazy[ToMappableBQ[H, M]], toT: Lazy[ToMappableBQ[T, M]])
  : ToMappableBQ[FieldType[K, Option[H]] :: T, M] = new ToMappableBQ[FieldType[K, Option[H]] :: T, M] {
    override def apply(l: FieldType[K, Option[H]] :: T): M =
      mbt.put(wit.value.name, l.head.map(h => toH.value.apply(h)), toT.value(l.tail))
  }

  implicit def nestedHListListToMappable[K <: Symbol, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], mbt: BaseMappableTypeBQ[M],
   toH: Lazy[ToMappableBQ[H, M]], toT: Lazy[ToMappableBQ[T, M]])
  : ToMappableBQ[FieldType[K, List[H]] :: T, M] = new ToMappableBQ[FieldType[K, List[H]] :: T, M] {
    override def apply(l: FieldType[K, List[H]] :: T): M =
      mbt.put(wit.value.name, l.head.map(h => toH.value.apply(h)), toT.value(l.tail))
  }

  implicit val bigQueryBaseMappableType = new BaseMappableTypeBQ[BigQueryMap] {
    override def base: BigQueryMap = new java.util.LinkedHashMap[String, Any]()

    override def get(m: BigQueryMap, key: String): Option[BigQueryMap] =
      Option(m.get(key)).map(_.asInstanceOf[BigQueryMap])

    override def getAll(m: BigQueryMap, key: String): Seq[BigQueryMap] =
      Option(m.get(key)).toSeq
        .flatMap(_.asInstanceOf[java.util.List[BigQueryMap]].asScala)

    override def put(key: String, value: BigQueryMap, tail: BigQueryMap): BigQueryMap = {
      tail.put(key, value)
      tail
    }

    override def put(key: String, value: Option[BigQueryMap], tail: BigQueryMap): BigQueryMap = {
      value.foreach(v => tail.put(key, v))
      tail
    }

    override def put(key: String, values: Seq[BigQueryMap], tail: BigQueryMap): BigQueryMap = {
      tail.put(key, values.asJava)
      tail
    }
  }

  private def at[T](fromFn: Any => T, toFn: T => Any) = new BaseBigQueryMappableTypeBQ[T] {
    override def from(value: Any): T = fromFn(value)

    override def to(value: T): Any = toFn(value)
  }

  private def id[T](x: T): Any = x.asInstanceOf[Any]

  implicit val booleanBigQueryMappableType = at[Boolean](_.toString.toBoolean, id)
  implicit val intBigQueryMappableType = at[Int](_.toString.toInt, id)
  implicit val longBigQueryMappableType = at[Long](_.toString.toLong, id)
  implicit val floatBigQueryMappableType = at[Float](_.toString.toFloat, id)
  implicit val doubleBigQueryMappableType = at[Double](_.toString.toDouble, id)
  implicit val stringBigQueryMappableType = at[String](_.toString, id)
  implicit val byteArrayBigQueryMappableType = at[Array[Byte]](
    x => BaseEncoding.base64().decode(x.toString),
    x => BaseEncoding.base64().encode(x))

  //TODO: check if this is correct
  implicit val byteStringBigQueryMappableType = at[ByteString](
    x => ByteString.copyFromUtf8(x.toString),
    x => x.toByteArray
  )
  import TimestampConverter._

  implicit val timestampBigQueryMappableType = at[Instant](toInstant, fromInstant)
  implicit val localDateBigQueryMappableType = at[LocalDate](toLocalDate, fromLocalDate)
  implicit val localTimeBigQueryMappableType = at[LocalTime](toLocalTime, fromLocalTime)
  implicit val localDateTimeBigQueryMappableType =
    at[LocalDateTime](toLocalDateTime, fromLocalDateTime)

  implicit def emunExtractor[A <: GeneratedEnum] = at[A](toEnum, fromEnum)
}

private object TimestampConverter {

  // FIXME: verify that these match BigQuery specification
  // TIMESTAMP
  // YYYY-[M]M-[D]D[ [H]H:[M]M:[S]S[.DDDDDD]][time zone]
  private val timestampPrinter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS ZZZ")
  private val timestampParser = new DateTimeFormatterBuilder()
    .append(DateTimeFormat.forPattern("yyyy-MM-dd"))
    .appendOptional(new DateTimeFormatterBuilder()
      .append(DateTimeFormat.forPattern(" HH:mm:ss").getParser)
      .appendOptional(DateTimeFormat.forPattern(".SSSSSS").getParser)
      .toParser)
    .appendOptional(new DateTimeFormatterBuilder()
      .append(null, Array(" ZZZ", "ZZ").map(p => DateTimeFormat.forPattern(p).getParser))
      .toParser)
    .toFormatter
    .withZoneUTC()

  // DATE
  // YYYY-[M]M-[D]D
  private val datePrinter = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC()
  private val dateParser = datePrinter

  // TIME
  // [H]H:[M]M:[S]S[.DDDDDD]
  private val timePrinter = DateTimeFormat.forPattern("HH:mm:ss.SSSSSS").withZoneUTC()
  private val timeParser = new DateTimeFormatterBuilder()
    .append(DateTimeFormat.forPattern("HH:mm:ss").getParser)
    .appendOptional(DateTimeFormat.forPattern(".SSSSSS").getParser)
    .toFormatter
    .withZoneUTC()

  // DATETIME
  // YYYY-[M]M-[D]D[ [H]H:[M]M:[S]S[.DDDDDD]]
  private val datetimePrinter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
  private val datetimeParser = new DateTimeFormatterBuilder()
    .append(DateTimeFormat.forPattern("yyyy-MM-dd"))
    .appendOptional(new DateTimeFormatterBuilder()
      .append(DateTimeFormat.forPattern(" HH:mm:ss").getParser)
      .appendOptional(DateTimeFormat.forPattern(".SSSSSS").getParser)
      .toParser)
    .toFormatter
    .withZoneUTC()

  def toInstant(v: Any): Instant = timestampParser.parseDateTime(v.toString).toInstant

  def fromInstant(i: Instant): Any = timestampPrinter.print(i)

  //TODO: How to convert back to Enum?
  def toEnum[A <: GeneratedEnum](v: Any): A = ???

  def fromEnum[A <: GeneratedEnum](i: A): Any = i.name

  def toLocalDate(v: Any): LocalDate = dateParser.parseLocalDate(v.toString)

  def fromLocalDate(d: LocalDate): Any = datePrinter.print(d)

  def toLocalTime(v: Any): LocalTime = timeParser.parseLocalTime(v.toString)

  def fromLocalTime(t: LocalTime): Any = timePrinter.print(t)

  def toLocalDateTime(v: Any): LocalDateTime = datetimeParser.parseLocalDateTime(v.toString)

  def fromLocalDateTime(dt: LocalDateTime): Any = datetimePrinter.print(dt)

}