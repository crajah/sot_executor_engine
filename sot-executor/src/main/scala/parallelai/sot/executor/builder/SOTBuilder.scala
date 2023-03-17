package parallelai.sot.executor.builder

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
import parallelai.sot.macros.SOTBuilder
import shapeless._
import syntax.singleton._
import com.google.datastore.v1.{GqlQuery, Query}
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.streaming.ACCUMULATING_FIRED_PANES
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import org.apache.beam.sdk.transforms.windowing.{AfterProcessingTime, Repeatedly}
import org.slf4j.LoggerFactory
import parallelai.sot.executor.builder.SOTBuilder.{readInput, transform, writeOutput}
import parallelai.sot.executor.utils.AvroUtils
import parallelai.sot.executor.scio.PaiScioContext._


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



  class Builder[In <: HasAvroAnnotation : Manifest, Out <: HasAnnotation : Manifest](transform: SCollection[In] => SCollection[Out], inArgs: PubSubArgs, outArgs: BigQueryArgs) extends Serializable() {
    private val logger = LoggerFactory.getLogger(this.getClass)

    def execute(opts: SOTOptions, args: Args, exampleUtils: SOTUtils, sc: ScioContext) = {
      val allowedLateness = Duration.standardMinutes(args.int("allowedLateness", 120))
      val project = opts.as(classOf[GcpOptions]).getProject
      val scIn = readInput(sc)
      val in = scIn.withGlobalWindow(WindowOptions(trigger = Repeatedly.forever(AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.standardMinutes(2))), accumulationMode = ACCUMULATING_FIRED_PANES, allowedLateness = allowedLateness))
      val out = writeOutput(transform(in))
      val result = sc.close()
      exampleUtils.waitToFinish(result.internal)
    }
  }

  val genericBuilder = new Builder(transform, inArgs, outArgs)

  def main(cmdArg: Array[String]): Unit = {
    val parsedArgs = ScioContext.parseArguments[SOTOptions](cmdArg)
    val opts = parsedArgs._1
    val args = parsedArgs._2
    opts.as(classOf[StreamingOptions]).setStreaming(true)
    val exampleUtils = new SOTUtils(opts)
    val sc = ScioContext(opts)
    val builder = genericBuilder
    builder.execute(opts, args, exampleUtils, sc)
  }
}

