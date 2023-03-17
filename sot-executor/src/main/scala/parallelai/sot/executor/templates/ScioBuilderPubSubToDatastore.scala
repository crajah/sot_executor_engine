package parallelai.sot.executor.templates

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import com.google.datastore.v1.{Entity}
import com.google.datastore.v1.client.DatastoreHelper.makeKey
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.streaming.ACCUMULATING_FIRED_PANES
import com.spotify.scio.values.{SCollection, WindowOptions}
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import org.apache.beam.sdk.transforms.windowing.{AfterProcessingTime, Repeatedly}
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import parallelai.sot.executor.common.{SOTOptions, SOTUtils}
import parallelai.sot.executor.utils.AvroUtils
import shapeless.{::, HList, Witness}
import com.spotify.scio._

import parallelai.sot.executor.datastore._

import shapeless._, labelled._

class ScioBuilderPubSubToDatastore[In <: HasAvroAnnotation : Manifest, Key <: Symbol, Value <: AnyRef, Y <: HList]
(transform: SCollection[In] => SCollection[FieldType[Key, Value] :: Y], inArgs: PubSubArgs, outArgs: DatastoreArgs)
  extends Serializable {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val avroT = AvroType[In]

  def dataStoreT: DatastoreType[Any] = DatastoreType[Any]

  def execute(opts: SOTOptions, args: Args, exampleUtils: SOTUtils, sc: ScioContext)
             (implicit toL: ToEntity[Y],
              mappable: MappableType[Entity.Builder, Value],
              witness: Witness.Aux[Key]) = {

    def getFieldName(value: FieldType[Key, Value])(implicit witness: Witness.Aux[Key]): Key = witness.value

    def getFieldValue(value: FieldType[Key, Value]): Value = value

    val allowedLateness = Duration.standardMinutes(args.int("allowedLateness", 120))
    val projectId = opts.as(classOf[GcpOptions]).getProject

    val schema = avroT.schema
    val fromGenericRecord = avroT.fromGenericRecord

    val schemaString = schema.toString

    val scIn = sc.pubsubTopic[Array[Byte]](s"projects/${projectId}/topics/${inArgs.topic}", timestampAttribute = "timestamp_ms")
      .map(f => fromGenericRecord(AvroUtils.decodeAvro(f, schemaString)))

    val in = scIn
      .withGlobalWindow(WindowOptions(
        trigger = Repeatedly.forever(AfterProcessingTime.pastFirstElementInPane()
          .plusDelayOf(Duration.standardMinutes(2))),
        accumulationMode = ACCUMULATING_FIRED_PANES,
        allowedLateness = allowedLateness)
      )

    val out = transform(in).map { rec =>
      val key :: value = rec
      val entity = dataStoreT.toEntityBuilder(value)
      val kind = getFieldName(key).name
      val keyEntity = makeKey(kind, getFieldValue(key))
      entity.setKey(keyEntity)
      entity.build()
    }.saveAsDatastore(projectId)

    val result = sc.close()
    exampleUtils.waitToFinish(result.internal)

  }

}
