package parallelai.sot.executor.builder

import scala.collection.immutable
import scalaz.Scalaz.init
import grizzled.slf4j.Logging
import shapeless._
import org.apache.beam.sdk.options.PipelineOptions
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.{Args, ScioContext}
import parallelai.sot.engine.config.SchemaResourcePath
import parallelai.sot.engine.config.gcp.{SOTOptions, SOTUtils}
import parallelai.sot.engine.generic.row.Row
import parallelai.sot.engine.io.TapDef
import parallelai.sot.engine.io.datastore._
import parallelai.sot.engine.runner.SCollectionStateMonad._
import parallelai.sot.engine.{Project, projectId}
import parallelai.sot.executor.model.SOTMacroConfig.{PubSubTapDefinition, TapDefinition}
import parallelai.sot.executor.model.{SOTMacroConfig, SOTMacroJsonConfig}
import parallelai.sot.macros.SOTMacroHelper._

/**
  * <pre>
  *   sbt -Dconfig.resource=application.datastore.test.conf clean compile "sot-executor/test:runMain parallelai.sot.executor.builder.DatastoreSOTBuilder --project=bi-crm-poc --runner=DirectRunner --region=europe-west1 --zone=europe-west2-a --workerMachineType=n1-standard-1 --diskSizeGb=150 --maxNumWorkers=1 --waitToFinish=false"
  * </pre>
  */
object DatastoreSOTBuilder extends Logging {
  val datastore = Datastore(Project(projectId), Kind("testkind1"))

  @AvroType.toSchema
  case class Message(user: String, teamName: String, score: Int, eventTime: Long, eventTimeStr: String)

  @AvroType.toSchema
  case class MessageExtended(user: String, teamName: String, score: Int, eventTime: Long, eventTimeStr: String, count: Int)

  implicit val messageGen = LabelledGeneric[Message]

  implicit val messageExtendedGen = LabelledGeneric[MessageExtended]

  object Config {
    val jobConfig: SOTMacroConfig.Config =
      SOTMacroJsonConfig(SchemaResourcePath().value)

    val sourceTap: List[(String, Option[SOTMacroConfig.Schema], TapDefinition)] =
      getSources(jobConfig)

    val sinkTaps: immutable.Seq[(String, Option[SOTMacroConfig.Schema], TapDefinition)] =
      getSinks(jobConfig)

    val source: TapDef[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, Message] =
      TapDef[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, Message](sourceTap.head._3)

    val sinks: TapDef[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, MessageExtended] :: HNil =
      TapDef[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, MessageExtended](sinkTaps.head._3) :: HNil

    /*val sinks =
      SchemalessTapDef[BigQueryTapDefinition, SOTUtils, com.google.api.client.json.GenericJson](sinkTaps.head._2) :: HNil*/
  }

  object Builder extends Serializable {
    import Config._

    def execute(sotUtils: SOTUtils, sc: ScioContext, args: Args): Unit = {
      val job = init[HNil] flatMap { _ =>
        read(sc, source, sotUtils)
      } flatMap { sColls =>
        map(sColls.at(Nat._0)) { m =>
          // TODO - For testing only
          val incomingMessage = Row.to[Message].from(m.hList)
          datastore.put("blah", incomingMessage)

          datastore.get[Message, m.L]("blah").map { persistedM =>
            info(s"Persisted Message = $persistedM")
            val updatedM = m.append('count, persistedM.score)

            val messageExtended = Row.to[MessageExtended].from(updatedM.hList)
            info(s"Message Extended = $messageExtended")

            updatedM
          } get
        }
      } flatMap { sColls =>
        write(sColls.at(Nat._1))(sinks.head, sotUtils)
      }

      job.run(HNil)
      val result = sc.close()

      if (args.getOrElse("waitToFinish", "true").toBoolean) sotUtils.waitToFinish(result.internal)
    }
  }

  def main(cmdArg: Array[String]): Unit = {
    val (sotOptions, sotArgs) = ScioContext.parseArguments[SOTOptions](cmdArg)
    execute(sotOptions, sotArgs)
  }

  def execute[P <: PipelineOptions](pipelineOptions: P, args: Args): Unit =
    Builder.execute(new SOTUtils(pipelineOptions), ScioContext(pipelineOptions), args)
}