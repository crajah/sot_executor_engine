package parallelai.sot.executor.templates

import com.spotify.scio.{Args, ScioContext}
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.streaming.ACCUMULATING_FIRED_PANES
import com.spotify.scio.values.{SCollection, WindowOptions}
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import org.apache.beam.sdk.transforms.windowing.{AfterProcessingTime, Repeatedly}
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import parallelai.sot.executor.common.{SOTOptions, SOTUtils}
import parallelai.sot.executor.utils.AvroUtils

class ScioBuilderPubSubToPubSub[In <: HasAvroAnnotation : Manifest, Out <: HasAvroAnnotation : Manifest]
(transform: SCollection[In] => SCollection[Out], inArgs: PubSubArgs, outArgs: PubSubArgs)
  extends Serializable {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val avroTIn = AvroType[In]
  val avroTOut = AvroType[Out]

  def execute(opts: SOTOptions, args: Args, exampleUtils: SOTUtils, sc: ScioContext) = {

    val project = opts.as(classOf[GcpOptions]).getProject

    val schemaIn = avroTIn.schema
    val fromGenericRecordIn = avroTIn.fromGenericRecord
    val schemaStringIn = schemaIn.toString

    val schemaOut = avroTOut.schema
    val toGenericRecordOut = avroTOut.toGenericRecord
    val schemaStringOut = schemaOut.toString

    val scIn = sc.pubsubTopic[Array[Byte]](s"projects/${project}/topics/${inArgs.topic}", timestampAttribute = "timestamp_ms")
      .map(f => fromGenericRecordIn(AvroUtils.decodeAvro(f, schemaStringIn)))

    val out = transform(scIn).map(r => AvroUtils.encodeAvro(toGenericRecordOut(r), schemaStringOut)).saveAsPubsub(s"projects/${project}/topics/${outArgs.topic}")

    val result = sc.close()
    exampleUtils.waitToFinish(result.internal, true)
  }

}
