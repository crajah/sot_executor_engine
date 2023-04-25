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

class ScioBuilderBigQueryToPubSub[In <: HasAnnotation : Manifest, Out <: HasAvroAnnotation : Manifest]
(transform: SCollection[In] => SCollection[Out], inArgs: BigQueryArgs, outArgs: PubSubArgs)
  extends Serializable {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val avroT = AvroType[Out]

  def execute(opts: SOTOptions, args: Args, exampleUtils: SOTUtils, sc: ScioContext) = {

    val allowedLateness = Duration.standardMinutes(args.int("allowedLateness", 120))
    val project = opts.as(classOf[GcpOptions]).getProject

    val schema = avroT.schema
    val toGenericRecord = avroT.toGenericRecord

    val schemaString = schema.toString

    val scIn = sc.typedBigQuery[In](s"${inArgs.dataset}.${inArgs.table}")

    val in = scIn
      .withGlobalWindow(WindowOptions(
        trigger = Repeatedly.forever(AfterProcessingTime.pastFirstElementInPane()
          .plusDelayOf(Duration.standardMinutes(2))),
        accumulationMode = ACCUMULATING_FIRED_PANES,
        allowedLateness = allowedLateness)
      )

    val out = transform(in).map(r => AvroUtils.encodeAvro(toGenericRecord(r), schemaString)).saveAsPubsub(s"projects/${project}/topics/${outArgs.topic}")

    val result = sc.close()
    exampleUtils.waitToFinish(result.internal)
  }

}
