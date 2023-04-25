package parallelai.sot.executor.builder

import java.io.{File, InputStream}
import java.util.TimeZone

import com.spotify.scio._
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.bigquery.BigQueryType
import com.spotify.scio.values.{SCollection, WindowOptions}
import org.apache.beam.sdk.io.gcp.datastore.DatastoreIO
import org.apache.beam.sdk.options.{PipelineOptions, StreamingOptions}
import org.joda.time.{DateTimeZone, Duration, Instant}
import org.joda.time.format.DateTimeFormat
import parallelai.sot.engine.config.gcp.SOTUtils
import parallelai.sot.macros.SOTBuilder
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
import parallelai.sot.engine.runner.scio.PaiScioContext._
import parallelai.sot.macros.SOTMacroHelper._
import com.trueaccord.scalapb.GeneratedMessage
import parallelai.sot.engine.config.gcp.{SOTOptions, SOTUtils}
import parallelai.sot.engine.serialization.avro.AvroUtils
import parallelai.sot.engine.runner.Reader
import parallelai.sot.engine.runner.Transformer
import parallelai.sot.engine.runner.Writer
import parallelai.sot.engine.runner.Runner
import parallelai.sot.engine.generic.row.Row
import parallelai.sot.engine.generic.row.Row._
import parallelai.sot.engine.generic.row.DeepRec._
import parallelai.sot.engine.generic.row.DeepRec
import parallelai.sot.engine.generic.row.DeepRec.ToCcPartiallyApplied
import parallelai.sot.engine.generic.helper.Helper
import scala.meta.Lit


/*
TO RUN THE INJECTOR
sbt "sot-executor/runMain parallelai.sot.executor.example.Injector bi-crm-poc p2pin none avro"
 */

/*
TO RUN THIS CLASS:
sbt clean compile \
   "sot-executor/runMain parallelai.sot.executor.builder.SOTBuilder \
    --project=bi-crm-poc \
    --runner=DataflowRunner \
    --region=europe-west1 \
    --zone=europe-west2-a \
    --workerMachineType=n1-standard-1 \
    --diskSizeGb=150 \
    --maxNumWorkers=1 \
    --waitToFinish=true"
*/
object SOTBuilder {
  @AvroType.fromSchema("{\"type\":\"record\",\"name\":\"Message\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\",\"doc\":\"Name of the user\"},{\"name\":\"teamName\",\"type\":\"string\",\"doc\":\"Name of the team\"},{\"name\":\"score\",\"type\":\"long\",\"doc\":\"User score\"},{\"name\":\"eventTime\",\"type\":\"long\",\"doc\":\"time when event created\"},{\"name\":\"eventTimeStr\",\"type\":\"string\",\"doc\":\"event time string for debugging\"}]}")
  class Message
  @BigQueryType.fromSchema("{\"type\":\"bigquerydefinition\",\"name\":\"BigQueryRow\",\"fields\":[{\"mode\":\"REQUIRED\",\"name\":\"user\",\"type\":\"STRING\"},{\"mode\":\"REQUIRED\",\"name\":\"teamName\",\"type\":\"STRING\"},{\"mode\":\"REQUIRED\",\"name\":\"score\",\"type\":\"INTEGER\"},{\"mode\":\"REQUIRED\",\"name\":\"eventTime\",\"type\":\"INTEGER\"},{\"mode\":\"REQUIRED\",\"name\":\"eventTimeStr\",\"type\":\"STRING\"},{\"mode\":\"REQUIRED\",\"name\":\"score2\",\"type\":\"FLOAT\"},{\"mode\":\"REQUIRED\",\"name\":\"processingTime\",\"type\":\"STRING\"}]}")
  class BigQueryRow
  val inOutSchemaHList = Runner[parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, com.spotify.scio.avro.types.AvroType.HasAvroAnnotation, Message, com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation, BigQueryRow, parallelai.sot.executor.model.SOTMacroConfig.BigQueryTapDefinition]
  implicit def genericTransformation: Transformer[Message, BigQueryRow, Nothing] = new Transformer[Message, BigQueryRow, Nothing] {
    import shapeless.record._
    import parallelai.sot.engine.generic.row.DeepRec._
    type Out = (Option[Nothing], SCollection[BigQueryRow])
    def transform(rowIn: SCollection[Message]): Out = {
      val converter = Row.to[BigQueryRow]
      val in = rowIn.map(r => Row(r))
      val trans = in.filter(m => m.get('score) > 2).map(m => m.append('score2, m.get('score) * 0.23d)).map(m => m.append('processingTime, Helper.fmt.print(Instant.now())))
      (None, trans.map(r => converter.from(r.hl)))
    }
  }
  class Builder extends Serializable() {
    private val logger = LoggerFactory.getLogger(this.getClass)
    def execute(jobConfig: Config, sotUtils: SOTUtils, sc: ScioContext, args: Args) = {
      val sourceTap = getSource(jobConfig)._2
      val sinkTap = getSink(jobConfig)._2
      val runner = inOutSchemaHList.exec(sc, sourceTap, sinkTap, sotUtils)
      val result = sc.close()
      if (args.getOrElse("waitToFinish", "true").toBoolean) sotUtils.waitToFinish(result.internal)
    }
  }
  def loadConfig() = {
    val configPath = getClass.getResource("/application.conf")
    val fileName = ConfigFactory.parseURL(configPath).getString("json.file.name")
    SOTMacroJsonConfig(fileName)
  }
  val genericBuilder = new Builder()
  def main(cmdArg: Array[String]): Unit = {
    val parsedArgs = ScioContext.parseArguments[SOTOptions](cmdArg)
    val opts = parsedArgs._1
    val args = parsedArgs._2
    val sotUtils = new SOTUtils(opts)
    val sc = ScioContext(opts)
    val builder = genericBuilder
    val jobConfig = loadConfig()
    builder.execute(jobConfig, sotUtils, sc, args)
  }
}
