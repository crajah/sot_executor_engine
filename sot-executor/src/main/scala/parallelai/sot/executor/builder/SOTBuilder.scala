package parallelai.sot.executor.builder

import java.io.File
import java.util.TimeZone

import com.spotify.scio._
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.bigquery.BigQueryType
import com.spotify.scio.values.{SCollection, WindowOptions}
import org.apache.beam.sdk.io.gcp.datastore.DatastoreIO
import org.apache.beam.sdk.options.StreamingOptions
import org.joda.time.{DateTimeZone, Duration, Instant}
import org.joda.time.format.DateTimeFormat
import parallelai.sot.executor.common.{SOTOptions, SOTUtils}
import parallelai.sot.executor.templates._
import parallelai.sot.macros.{SOTBuilder, SOTMacroHelper}
import shapeless._
import syntax.singleton._
import com.google.datastore.v1.{GqlQuery, Query}
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.streaming.ACCUMULATING_FIRED_PANES
import com.typesafe.config.ConfigFactory
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import org.apache.beam.sdk.transforms.windowing.{AfterProcessingTime, Repeatedly}
import org.slf4j.LoggerFactory
import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.executor.model.SOTMacroJsonConfig
import parallelai.sot.executor.utils.AvroUtils
import parallelai.sot.executor.scio.PaiScioContext._

import scala.meta.Lit


/*
TO RUN THE INCEPTOR
sbt "sot-executor/runMain parallelai.sot.executor.example.Injector bi-crm-poc p2pin none"
 */

/*
TO RUN THIS CLASS:
sbt clean compile \
   "sot-executor/runMain parallelai.sot.executor.builder.SOTBuilder \
    --project=bi-crm-poc \
    --runner=DataflowRunner \
    --zone=europe-west2-a"
*/

object Helper {
  def fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    .withZone(DateTimeZone.forTimeZone(TimeZone.getTimeZone("PST")))

}


trait SchemaType {
  type A
  type T <: A
  val m: Manifest[T]
}

object SchemaType {

  type Aux[A0, T0 <: A0] = SchemaType {type A = A0; type T = T0}

  def apply[A0, T0 <: A0]()(implicit ma: Manifest[T0], ev: T0 <:< A0): SchemaType.Aux[A0, T0] = {
    new SchemaType() {
      type T = T0
      type A = A0
      val m = ma
    }
  }
}


@SOTBuilder
object SOTBuilder {

  //  @AvroType.fromSchema("{\"name\":\"Message\",\"doc\":\"A basic schema for storing user records\",\"fields\":[{\"name\":\"user\",\"type\":\"string\",\"doc\":\"Name of the user\"},{\"name\":\"teamName\",\"type\":\"string\",\"doc\":\"Name of the team\"},{\"name\":\"score\",\"type\":\"int\",\"doc\":\"User score\"},{\"name\":\"eventTime\",\"type\":\"long\",\"doc\":\"time when event created\"},{\"name\":\"eventTimeStr\",\"type\":\"string\",\"doc\":\"event time string for debugging\"}],\"type\":\"record\",\"namespace\":\"parallelai.sot.avro\"}")
  //  class Message
  //
  //  case class OutSchemaTest2(teamscores: String, score1x: Int, time: String)
  //
  //  type In = Message
  //  type Out = OutSchemaTest2
  //
  //  def transform(in: SCollection[In]) = {
  //    in.filter(m => m.score > 2).map(m => (m.teamName, m.score.toInt)).sumByKey.
  //      map(m => OutSchemaTest2(m._1, m._2, Helper.fmt.print(Instant.now())))
  //  }
  //
  //  val keyBuilder = (d: OutSchemaTest2) => Left(d.teamscores)
  //
  //  val inArgs = PubSubArgs(topic = "p2pin")
  //  val outArgs = DatastoreArgs("testkindtest2")
  //  val getBuilder = new ScioBuilderPubSubToDatastoreWithSchema(transform, inArgs, outArgs, keyBuilder)



  class Builder extends Serializable {

    private val logger = LoggerFactory.getLogger(this.getClass)

    def execute(outArgs: BigQueryArgs, jobConfig: Config,
                opts: SOTOptions, args: Args, sotUtils: SOTUtils, sc: ScioContext) = {

      val config = opts.as(classOf[GcpOptions])
      val source = getSource(jobConfig)
      val sourceTap = source._1
      val schemaIn = source._2
      val scIn = schemaIn.definition match {
        case d: AvroDefinition => {
          val caseType = avroSchemaTypes(d.name)
          sourceTap match {
            case tap: PubSubTapDefinition => {
              Reader[PubSubTapDefinition, GcpOptions, caseType.A, caseType.T].read(sc, tap, config)
            }
            case _ => throw new Exception(s"Unexpected type: ${sourceTap.getClass.getName}")
          }
        }
        case _ => throw new Exception(s"Unexpected schema definition: ${schemaIn.getClass}")
      }

      val allowedLateness = Duration.standardMinutes(args.int("allowedLateness", 120))
      val in = scIn.withGlobalWindow(WindowOptions(trigger = Repeatedly.forever(
        AfterProcessingTime.pastFirstElementInPane().
          plusDelayOf(Duration.standardMinutes(2))),
        accumulationMode = ACCUMULATING_FIRED_PANES,
        allowedLateness = allowedLateness)
      )

      val sColl = transform(in)

      val sink = getSink(jobConfig)
      val sinkTap = sink._1
      val schemaOut = sink._2

      schemaOut.definition match {
        case d: AvroDefinition => {
          val caseType = avroSchemaTypes(d.name)
          sinkTap match {
            case tap: PubSubTapDefinition => {
              Writer[PubSubTapDefinition, GcpOptions, caseType.A, caseType.T].write(sColl, tap, config)
            }
            case _ => throw new Exception(s"Unexpected type: ${sourceTap.getClass.getName}" + schemaOut.definition)
          }
        }
        case d: BigQueryTapDefinition => {
          val caseType = bigquerySchemaTypes(d.name)
          sinkTap match {
            case tap: BigQueryTapDefinition => {
              Writer[BigQueryTapDefinition, GcpOptions, caseType.A, caseType.T].write(sColl, tap, config)
            }
            case _ => throw new Exception(s"Unexpected type: ${sourceTap.getClass.getName}")
          }
        }
        case _ => throw new Exception(s"Unexpected schema definition: ${schemaOut.getClass}" + schemaOut.definition)
      }


      val result = sc.close()
      sotUtils.waitToFinish(result.internal)
    }
  }


  def getSource(jobConfig: Config): (TapDefinition, Schema) = {
    val source = jobConfig.parseDAG().getSourceVertices().head
    val sourceOp = SOTMacroHelper.getOp(source, jobConfig.steps).asInstanceOf[SourceOp]
    (SOTMacroHelper.getTap(sourceOp.tap, jobConfig.taps), SOTMacroHelper.getSchema(sourceOp.schema, jobConfig.schemas))
  }


  def getSink(jobConfig: Config) = {
    val sink = jobConfig.parseDAG().getSinkVertices().head
    val sinkOp = SOTMacroHelper.getOp(sink, jobConfig.steps).asInstanceOf[SinkOp]
    (SOTMacroHelper.getTap(sinkOp.tap, jobConfig.taps), SOTMacroHelper.getSchema(sinkOp.schema.get, jobConfig.schemas))
  }


  val source = getClass.getResource("/application.conf").getPath
  val fileName = ConfigFactory.parseFile(new File(source)).getString("json.file.name")
  val jobConfig = SOTMacroJsonConfig(fileName)

  val genericBuilder = new Builder

  def main(cmdArg: Array[String]): Unit = {
    val parsedArgs = ScioContext.parseArguments[SOTOptions](cmdArg)
    val opts = parsedArgs._1
    val args = parsedArgs._2
    opts.as(classOf[StreamingOptions]).setStreaming(true)
    val sotUtils = new SOTUtils(opts)
    val sc = ScioContext(opts)
    val builder = genericBuilder
    builder.execute(outArgs, jobConfig, opts, args, sotUtils, sc)
  }
}

