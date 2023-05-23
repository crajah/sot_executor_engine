package parallelai.sot.executor.builder

import parallelai.sot.engine.config.SchemaResourcePath

//// SOT
import parallelai.sot.engine.config.gcp.SOTUtils
import parallelai.sot.macros.SOTBuilder
import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.executor.model.SOTMacroJsonConfig
import parallelai.sot.engine.runner.scio.PaiScioContext._
import parallelai.sot.macros.SOTMacroHelper._
import parallelai.sot.engine.config.gcp.{SOTOptions, SOTUtils}
import parallelai.sot.engine.io.{SchemalessTapDef, TapDef}

//// JSON parser
import io.circe.generic.auto._
import io.circe.parser._

//// SCIO
import com.spotify.scio._

//// Shapeless
import shapeless._
import syntax.singleton._
import shapeless.record._

//// Tensorflow
import org.tensorflow._

//// Annotations
import parallelai.sot.engine.io.utils.annotations._
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.bigquery.BigQueryType
import com.trueaccord.scalapb.GeneratedMessage

//// Row
import parallelai.sot.engine.generic.row.Row
import parallelai.sot.engine.generic.row.Syntax._
import parallelai.sot.engine.generic.row.Nested

//// Helper functions
import parallelai.sot.engine.generic.helper.Helper

//// Runner
import parallelai.sot.engine.runner.Reader
import parallelai.sot.engine.runner.Writer
import parallelai.sot.engine.runner.SCollectionStateMonad._
import scalaz.Scalaz.init

//// Windowing imports
import org.joda.time.Duration
import com.spotify.scio.values.WindowOptions
import org.apache.beam.sdk.values.WindowingStrategy.AccumulationMode
import org.apache.beam.sdk.transforms.windowing.Window.ClosingBehavior
import org.apache.beam.sdk.transforms.windowing.TimestampCombiner
import org.apache.beam.sdk.transforms.windowing._

import parallelai.sot.engine.io.bigquery._
import parallelai.sot.engine.io.datastore._

/**
  * To run this class with a default configuration of application.conf:
  * <pre>
  * sbt clean compile "sot-executor/runMain parallelai.sot.executor.builder.SOTBuilder --project=bi-crm-poc --runner=DataflowRunner --region=europe-west1 --zone=europe-west2-a --workerMachineType=n1-standard-1 --diskSizeGb=150 --maxNumWorkers=1 --waitToFinish=false"
  * </pre>
  *
  * If there is no application.conf then compilation will fail, but you can supply your own conf as a Java option e.g. -Dconfig.resource=application-ps2ps-test.conf
  * <pre>
  * sbt -Dconfig.resource=application-ps2ps-test.conf clean compile "sot-executor/runMain parallelai.sot.executor.builder.SOTBuilder --project=bi-crm-poc --runner=DataflowRunner --region=europe-west1 --zone=europe-west2-a --workerMachineType=n1-standard-1 --diskSizeGb=150 --maxNumWorkers=1 --waitToFinish=false"
  * </pre>
  * NOTE That application configurations can also be set/overridden via system and environment properties.
  */
@SOTBuilder
object SOTBuilder {

  object conf {
    val jobConfig = SOTMacroJsonConfig(SchemaResourcePath().value)
    val sourceTap = getSource(jobConfig)._2
    val sinkTaps = getSinks(jobConfig)
  }

  val genericBuilder = new Builder()
  def main(cmdArg: Array[String]): Unit = {
    val (sotOptions, sotArgs) = ScioContext.parseArguments[SOTOptions](cmdArg)
    val sotUtils = new SOTUtils(sotOptions)
    val sc = ScioContext(sotOptions)
    val builder = genericBuilder
    builder.execute(sotUtils, sc, sotArgs)
  }
}