package parallelai.sot.executor.builder

import java.io.{File, InputStream}
import java.util.TimeZone
import scala.meta.Lit
import parallelai.sot.engine.config.SchemaResourcePath
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
import parallelai.sot.engine.generic.row.Syntax._

import parallelai.sot.engine.generic.row.Nested

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
  * NOTE That application configurations can also be set/overridden via system and environment properties.
  */
object SOTBuilder {

  @AvroType.fromSchema("""
    |  {"type":"record","name":"Features","namespace":"parallelai.sot.avro","fields":[
    |  {"name": "features", "type": {"type": "array", "items": "float"}},
    |  {"name":"id","type":"string"}
    |  ]}
    """.stripMargin)
  class Features
  @BigQueryType.fromSchema("{\"type\":\"bigquerydefinition\",\"name\":\"BigQueryRow\",\"fields\":[{\"mode\":\"REPEATED\",\"name\":\"prediction\",\"type\":\"FLOAT\"}]}")
  class BigQueryRow

  import org.tensorflow._
  import com.spotify.scio.sot.tensorflow._

  val inOutSchemaHList = Runner[parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, com.spotify.scio.avro.types.AvroType.HasAvroAnnotation, Features, com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation, BigQueryRow, parallelai.sot.executor.model.SOTMacroConfig.BigQueryTapDefinition]
  implicit def genericTransformation: Transformer[Features, BigQueryRow, Nothing] = new Transformer[Features, BigQueryRow, Nothing] {
    import shapeless.record._

    type Out = (Option[Nothing], SCollection[BigQueryRow])
    def transform(rowIn: SCollection[Features]): Out = {
      val converter = Row.to[BigQueryRow]
      val in = rowIn.map(r => Row(r))
      val trans = in.predict("lb-tf-models", "inception_v1", Seq("InceptionV1/Logits/Predictions/Reshape_1"))
      {e => Map("input" -> Tensor.create(Array(e.get('features).grouped(672).map(_.toArray.grouped(3).toArray).toArray)))}
      {o => o.map{
        case (_, t) =>
          val v = Array.ofDim[Float](1,1001)
          t.copyTo(v)
          val res = v.apply(0).map(_.toDouble).toList
          new Row('prediction ->> res :: HNil)
      }.head}

      (None, trans.map(r => converter.from(r.hl)))
    }
  }
  class Builder extends Serializable() {
    def execute(jobConfig: Config, sotUtils: SOTUtils, sc: ScioContext, args: Args): Unit = {
      val sourceTap = getSource(jobConfig)._2
      val sinkTap = getSink(jobConfig)._2
      inOutSchemaHList.exec(sc, sourceTap, sinkTap, sotUtils)
      val result = sc.close()
      if (args.getOrElse("waitToFinish", "true").toBoolean) sotUtils.waitToFinish(result.internal)
    }
  }
  val genericBuilder = new Builder()
  def main(cmdArg: Array[String]): Unit = {
    val (sotOptions, sotArgs) = ScioContext.parseArguments[SOTOptions](cmdArg)
    val sotUtils = new SOTUtils(sotOptions)
    val sc = ScioContext(sotOptions)
    val builder = genericBuilder
    val jobConfig = SOTMacroJsonConfig(SchemaResourcePath().value)
    builder.execute(jobConfig, sotUtils, sc, sotArgs)
  }
}