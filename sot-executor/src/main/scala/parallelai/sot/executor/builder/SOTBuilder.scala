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

object SOTBuilder {

  @AvroType.fromSchema("{\"type\":\"record\",\"name\":\"Message\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\",\"doc\":\"Name of the user\"},{\"name\":\"teamName\",\"type\":\"string\",\"doc\":\"Name of the team\"},{\"name\":\"score\",\"type\":\"long\",\"doc\":\"User score\"},{\"name\":\"eventTime\",\"type\":\"long\",\"doc\":\"time when event created\"},{\"name\":\"eventTimeStr\",\"type\":\"string\",\"doc\":\"event time string for debugging\"}]}")
  class Message

  @BigQueryType.fromSchema("{\"type\":\"bigquerydefinition\",\"name\":\"BigQueryRow\",\"fields\":[{\"mode\":\"REQUIRED\",\"name\":\"user\",\"type\":\"STRING\"},{\"mode\":\"REQUIRED\",\"name\":\"teamName\",\"type\":\"STRING\"},{\"mode\":\"REQUIRED\",\"name\":\"score\",\"type\":\"INTEGER\"},{\"mode\":\"REQUIRED\",\"name\":\"eventTime\",\"type\":\"INTEGER\"},{\"mode\":\"REQUIRED\",\"name\":\"eventTimeStr\",\"type\":\"STRING\"},{\"mode\":\"REQUIRED\",\"name\":\"score2\",\"type\":\"FLOAT\"},{\"mode\":\"REQUIRED\",\"name\":\"processingTime\",\"type\":\"STRING\"}]}")
  class BigQueryRow

  object conf {
    val jobConfig = SOTMacroJsonConfig(SchemaResourcePath().value)
    val sourceTaps = getSources(jobConfig)
    val sinkTaps = getSinks(jobConfig)
    val sources = TapDef[parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, com.spotify.scio.avro.types.AvroType.HasAvroAnnotation, Message](conf.sourceTaps(0)._3) :: HNil
    val sinks = TapDef[parallelai.sot.executor.model.SOTMacroConfig.BigQueryTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation, BigQueryRow](conf.sinkTaps(0)._3) :: HNil
  }

  class Job extends Serializable {
    def execute(sotUtils: SOTUtils, sc: ScioContext, args: Args): Unit = {
      val job = init[HNil].flatMap(sColls => read(sc, TapDef[parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, com.spotify.scio.avro.types.AvroType.HasAvroAnnotation, Message](conf.sourceTaps(0)._3), sotUtils)).flatMap(sColls => filter(sColls.at(Nat0()))(m => m.get('score) > 2)).flatMap(sColls => map(sColls.at(Nat1()))(m => m.append('score2, m.get('score) * 0.23d))).flatMap(sColls => map(sColls.at(Nat2()))(m => m.append('processingTime, Helper.fmt.print(Helper.Instant.now())))).flatMap(sColls => write(sColls.at(Nat3()))(TapDef[parallelai.sot.executor.model.SOTMacroConfig.BigQueryTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation, BigQueryRow](conf.sinkTaps(0)._3), sotUtils))
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