package parallelai.sot.executor.builder

import java.io.{File, InputStream}
import java.util.TimeZone
import scala.meta.Lit
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
import parallelai.sot.engine.generic.helper.Helper
import parallelai.sot.engine.generic.row.Row

/**
  * To run this class with a default configuration of application.conf:
  * <pre>
  *   sbt clean compile "sot-executor/runMain parallelai.sot.executor.builder.SOTBuilder --project=bi-crm-poc --runner=DataflowRunner --region=europe-west1 --zone=europe-west2-a --workerMachineType=n1-standard-1 --diskSizeGb=150 --maxNumWorkers=1 --waitToFinish=false"
  * </pre>
  *
  * If there is no application.conf then compilation will fail, but you can supply your own conf as a Java option e.g. -Dconfig.resource=application-ps2ps-test.conf
  * <pre>
  *   sbt -Dconfig.resource=application-ps2ps-test.conf clean compile "sot-executor/runMain parallelai.sot.executor.builder.SOTBuilder --project=bi-crm-poc --runner=DataflowRunner --region=europe-west1 --zone=europe-west2-a --workerMachineType=n1-standard-1 --diskSizeGb=150 --maxNumWorkers=1 --waitToFinish=false"
  * </pre>
  */
@SOTBuilder("application.conf")
object SOTBuilder {
  class Builder extends Serializable() {
    def execute(jobConfig: Config, sotUtils: SOTUtils, sc: ScioContext, args: Args): Unit = {
      val sourceTap = getSource(jobConfig)._2
      val sinkTap = getSink(jobConfig)._2
      inOutSchemaHList.exec(sc, sourceTap, sinkTap, sotUtils)
      val result = sc.close()
      if (args.getOrElse("waitToFinish", "true").toBoolean) sotUtils.waitToFinish(result.internal)
    }
  }

  def loadConfig(): Config = {
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