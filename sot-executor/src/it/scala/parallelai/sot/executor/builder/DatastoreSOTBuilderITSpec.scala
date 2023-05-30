package parallelai.sot.executor.builder

import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.apache.beam.sdk.io.gcp.pubsub.PubsubOptions
import org.scalatest.{MustMatchers, WordSpec}
import com.dimafeng.testcontainers.MultipleContainers
import com.google.api.services.pubsub.Pubsub
import com.google.api.services.pubsub.model._
import com.google.common.collect.ImmutableMap
import com.sksamuel.avro4s._
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.engine.ProjectFixture
import parallelai.sot.engine.avro.AvroFixture
import parallelai.sot.engine.io.datastore._
import parallelai.sot.engine.io.pubsub.PubsubContainerFixture
import parallelai.sot.engine.system._
import parallelai.sot.executor.builder.DatastoreSOTBuilder._

/**
  * curl http://localhost:8085/v1/projects/bi-crm-poc/topics
  */
class DatastoreSOTBuilderITSpec extends WordSpec with MustMatchers with AvroFixture with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with PubsubContainerFixture {
  spec =>

  override val container = MultipleContainers(datastoreContainer, pubsubContainer)

  implicit val messageSchema: SchemaFor[Message] = SchemaFor[Message]
  implicit val messageRecord: ToRecord[Message] = ToRecord[Message]

  implicit val messageExtendedSchema: SchemaFor[MessageExtended] = SchemaFor[MessageExtended]
  implicit val messageExtendedRecord: FromRecord[MessageExtended] = FromRecord[MessageExtended]

  "Datastore SOT application" should {
    "execute its job by consuming a message from Pubsub, processing said message and sending that new message back onto Pubsub" in {
      withSystemProperties("config.resource" -> "application.datastore.test.conf") {
        val pubsub: Pubsub = pubsubClient()

        val topic: Topic = newTopic("p2pin", pubsub)
        val outTopic: Topic = newTopic("p2pout", pubsub)
        val subscription: Subscription = newSubscription("p2pout", pubsub)

        Future {
          val builder = DatastoreSOTBuilder

          val (sotOptions, sotArgs) = builder.executionContext(Array(s"--project=${project.id}", "--runner=DirectRunner"))
          sotOptions.as(classOf[PubsubOptions]).setPubsubRootUrl(pubsubContainer.ip)

          val job = new Job {
            override val datastore: Datastore = spec.datastore
          }

          builder.execute(job, sotOptions, sotArgs)
        }

        TimeUnit.SECONDS.sleep(10) // TODO - Get rid of

        val message = Message("user", "teamName", score = 1, eventTime = 0, eventTimeStr = "0")
        datastore.put("blah", message)

        val pubsubMessage = new PubsubMessage().encodeData(serialize(message))
        pubsubMessage.setAttributes(ImmutableMap.of("timestamp_ms", System.currentTimeMillis.toString))

        val publishRequest = new PublishRequest().setMessages(Seq(pubsubMessage))

        pubsub.projects().topics().publish(topic.getName, publishRequest).execute()

        TimeUnit.SECONDS.sleep(10) // TODO - Get rid of

        val pullRequest = new PullRequest().setMaxMessages(1)

        val pullResponse = pubsub.projects().subscriptions().pull(subscription.getName, pullRequest).execute()
        println(s"===> Received messages: ${pullResponse.getReceivedMessages}")

        val response = pullResponse.getReceivedMessages.head.getMessage.decodeData()

        deserialize[MessageExtended](response).toSeq must matchPattern {
          case Seq(MessageExtended("user", "teamName", 1, 0, "0", 1)) =>
        }
      }
    }
  }
}