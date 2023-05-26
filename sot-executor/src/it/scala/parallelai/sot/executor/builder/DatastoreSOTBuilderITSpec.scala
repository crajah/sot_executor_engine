package parallelai.sot.executor.builder

import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.apache.avro.generic.{GenericData, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.EncoderFactory
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.beam.sdk.io.gcp.pubsub.PubsubOptions
import org.scalatest.{MustMatchers, WordSpec}
import com.dimafeng.testcontainers.{Container, MultipleContainers}
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.util.Utils._
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.pubsub.model._
import com.google.api.services.pubsub.{Pubsub, PubsubScopes}
import com.google.common.collect.ImmutableMap
import com.spotify.scio.ScioContext
import parallelai.sot.containers.ForAllContainersSpec
import parallelai.sot.engine.config.gcp.SOTOptions
import parallelai.sot.engine.io.datastore._
import parallelai.sot.engine.io.pubsub.PubsubContainerSpec
import parallelai.sot.engine.projectId
import parallelai.sot.engine.system._

/**
  * curl http://localhost:8085/v1/projects/bi-crm-poc/topics
  */
class DatastoreSOTBuilderITSpec extends WordSpec with MustMatchers with ForAllContainersSpec with DatastoreContainerSpec with PubsubContainerSpec {
  override val container: Container = MultipleContainers(datastoreContainer, pubsubContainer)

  "" should {
    "" in {
      withSystemProperties("config.resource" -> "application.datastore.test.conf") {
        val pubsub: Pubsub = pubsubClient()
        val topic: Topic = pubsub.projects().topics().create(s"projects/$projectId/topics/p2pin", new Topic).execute()
        println(s"===> Created topic: $topic")

        // Create "out" topic
        val outTopic: Topic = pubsub.projects().topics().create(s"projects/$projectId/topics/p2pout", new Topic).execute()
        println(s"===> Created 'out' topic: $outTopic")

        // Subscription to outbound topic
        val subscription: Subscription = pubsub.projects().subscriptions().create(s"projects/$projectId/subscriptions/p2pout", new Subscription().setTopic(s"projects/$projectId/topics/p2pout")).execute()

        Future {
          val cmdArgs = Array(s"--project=$projectId", "--runner=DirectRunner", "--region=europe-west1", "--zone=europe-west2-a", "--workerMachineType=n1-standard-1", "--diskSizeGb=150", "--maxNumWorkers=1", "--waitToFinish=false")
          val (sotOptions, sotArgs) = ScioContext.parseArguments[SOTOptions](cmdArgs)

          val pipelineOptions = sotOptions.as(classOf[PubsubOptions])
          pipelineOptions.setPubsubRootUrl("http://localhost:8085")

          DatastoreSOTBuilder.execute(pipelineOptions, sotArgs)
        }


        TimeUnit.SECONDS.sleep(10)

        val schema: Schema =
          SchemaBuilder.record("Message").namespace("parallelai.sot.avro").fields()
            .requiredString("user")
            .requiredString("teamName")
            .requiredInt("score")
            .requiredLong("eventTime")
            .requiredString("eventTimeStr")
            .endRecord()

        val record = new GenericData.Record(schema)
        record.put("user", "user")
        record.put("teamName", "teamName")
        record.put("score", 1)
        record.put("eventTime", 0L)
        record.put("eventTimeStr", "0")

        val pubsubMessage = new PubsubMessage().encodeData(toBytes(schema)(record))
        pubsubMessage.setAttributes(ImmutableMap.of("timestamp_ms", System.currentTimeMillis.toString))
        println(s"===> pubsub message = ${pubsubMessage.getData}")

        val publishRequest = new PublishRequest().setMessages(Seq(pubsubMessage))

        pubsub.projects().topics().publish(topic.getName, publishRequest).execute()

        TimeUnit.SECONDS.sleep(10)


        val pullRequest = new PullRequest().setMaxMessages(1)

        val pullResponse = pubsub.projects().subscriptions().pull(subscription.getName, pullRequest).execute()
        println(s"===> Received messages: ${pullResponse.getReceivedMessages}")

        pullResponse.getReceivedMessages.head.getMessage mustBe a [PubsubMessage] // TODO - A better assertion
      }
    }
  }

  def toBytes(schema: Schema)(record: GenericRecord): Array[Byte] = {
    val writer = new GenericDatumWriter[GenericRecord](schema)
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    writer.write(record, encoder)
    encoder.flush() // !
    out.toByteArray
  }

  def pubsubClient(httpTransport: HttpTransport = getDefaultTransport, jsonFactory: JsonFactory = getDefaultJsonFactory): Pubsub = {
    val credential: GoogleCredential = GoogleCredential.getApplicationDefault.createScoped(PubsubScopes.all)

    new Pubsub.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName("test")
      .setRootUrl("http://localhost:8085")
      .setSuppressRequiredParameterChecks(false)
      .build()
  }
}