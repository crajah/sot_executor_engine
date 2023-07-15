package parallelai.sot.executor.builder

import org.apache.beam.sdk.options.PipelineOptions
import parallelai.sot.engine.config.SchemaResourcePath

//// SOT
import parallelai.sot.engine.config.gcp.SOTUtils
import parallelai.sot.macros.SOTBuilder
import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.executor.model.SOTMacroJsonConfig
import com.spotify.scio.sot.PaiScioContext._
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

//// Windowing
import org.joda.time.Duration
import com.spotify.scio.values.WindowOptions
import org.apache.beam.sdk.values.WindowingStrategy.AccumulationMode
import org.apache.beam.sdk.transforms.windowing.Window.ClosingBehavior
import org.apache.beam.sdk.transforms.windowing.TimestampCombiner
import org.apache.beam.sdk.transforms.windowing._

import parallelai.sot.engine.io.bigquery._
import parallelai.sot.engine.io.datastore._

//// SOT Nats
import parallelai.sot.executor.SOTNats._

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
object SOTBuilder {
  @AvroType.fromSchema("{\"type\":\"record\",\"name\":\"Features\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"userId\",\"type\":\"string\"},{\"name\":\"depositAmount\",\"type\":\"float\"},{\"name\":\"depositTime\",\"type\":\"long\",\"doc\":\"time when event created\"},{\"name\":\"depositTimeStr\",\"type\":\"string\",\"doc\":\"event time string for debugging\"}]}")
  class Features
  @AvroType.fromSchema("{\"type\":\"record\",\"name\":\"Deposits\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"userId\",\"type\":\"string\"},{\"name\":\"nDeposits\",\"type\":\"int\"}]}")
  class Deposits
  object conf {
    val jobConfig = SOTMacroJsonConfig(SchemaResourcePath().value)
    val sourceTaps = getSources(jobConfig)
    val sinkTaps = getSinks(jobConfig)
    val sources = TapDef[parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, com.spotify.scio.avro.types.AvroType.HasAvroAnnotation, Features](conf.sourceTaps(0)._3) :: HNil
    val sinks = SchemalessTapDef[parallelai.sot.executor.model.SOTMacroConfig.BigQueryTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, parallelai.sot.engine.io.utils.annotations.Schemaless](conf.sinkTaps(0)._3) :: HNil
  }
  import parallelai.sot.engine.Project, parallelai.sot.engine.io.datastore.{ Datastore, Kind }
  val datastore1 = Datastore(Project("bi-crm-poc"), Kind("dataflowwrite9"))
  class Job extends Serializable {
    def execute(sotUtils: SOTUtils, sc: ScioContext, args: Args): Unit = {
      val job = init[HNil].flatMap(sColls => read(sc, TapDef[parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, com.spotify.scio.avro.types.AvroType.HasAvroAnnotation, Features](conf.sourceTaps(0)._3), sotUtils)).
        flatMap(sColls => increment(sColls.at(Nat0()))(m => (m.get('userId), m), m => 1, (row, counter) => Row(('key ->> row.get('userId)) :: ('counter ->> counter) :: HNil))).
        flatMap(sColls => writeSchemaless(sColls.at(Nat1()))(SchemalessTapDef[parallelai.sot.executor.model.SOTMacroConfig.BigQueryTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, parallelai.sot.engine.io.utils.annotations.Schemaless](conf.sinkTaps(0)._3), sotUtils))
      job.run(HNil)._1
      val result = sc.close()
      if (args.getOrElse("waitToFinish", "true").toBoolean) sotUtils.waitToFinish(result.internal)
    }
  }
  def main(cmdArg: Array[String]): Unit = {
    val (sotOptions, sotArgs) = executionContext(cmdArg)
    execute(new Job(), sotOptions, sotArgs)
  }
  def executionContext(cmdArg: Array[String]): (SOTOptions, Args) = ScioContext.parseArguments[SOTOptions](cmdArg)
  def execute[P <: PipelineOptions](job: Job, pipelineOptions: P, args: Args): Unit = {
    val sotUtils = new SOTUtils(pipelineOptions)
    val sc = ScioContext(pipelineOptions)
    job.execute(sotUtils, sc, args)
  }
}