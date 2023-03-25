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
import parallelai.sot.executor.model.{Row, SOTMacroJsonConfig, Transformer}
import parallelai.sot.executor.utils.AvroUtils
import parallelai.sot.executor.scio.PaiScioContext._
import parallelai.sot.macros.SOTMacroHelper._

import scala.meta.Lit
import shapeless._
import record._
import shapeless.labelled.{FieldType, field}
import syntax.singleton._
import shapeless.ops.record._


/*
TO RUN THE INJECTOR
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

@SOTBuilder
object SOTBuilder {

  class Builder extends Serializable() {
    private val logger = LoggerFactory.getLogger(this.getClass)


    def execute(jobConfig: Config, opts: SOTOptions, args: Args, sotUtils: SOTUtils, sc: ScioContext) = {
      val config = opts.as(classOf[GcpOptions])
      val sourceTap = getSource(jobConfig)._2
      val sinkTap = getSink(jobConfig)._2
      val runner = inOutSchemaHList.map(Runner1).head
      runner(sc, sourceTap, sinkTap, config)
      val result = sc.close()
      sotUtils.waitToFinish(result.internal)
    }
  }

  def loadConfig() = {
    val configPath = getClass.getResource("/application.conf").getPath
    val fileName = ConfigFactory.parseFile(new File(configPath)).getString("json.file.name")
    SOTMacroJsonConfig(fileName)
  }

  val genericBuilder = new Builder()

  def main(cmdArg: Array[String]): Unit = {
    val parsedArgs = ScioContext.parseArguments[SOTOptions](cmdArg)
    val opts = parsedArgs._1
    val args = parsedArgs._2
    opts.as(classOf[StreamingOptions]).setStreaming(true)
    val sotUtils = new SOTUtils(opts)
    val sc = ScioContext(opts)
    val builder = genericBuilder
    val jobConfig = loadConfig()
    builder.execute(jobConfig, opts, args, sotUtils, sc)
  }
}