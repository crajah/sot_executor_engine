package parallelai.sot.engine.io.pubsub

import grizzled.slf4j.Logging
import org.scalatest.Suite
import org.testcontainers.containers.wait.Wait
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.util.Utils.{getDefaultJsonFactory, getDefaultTransport}
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.pubsub.model.{Subscription, Topic}
import com.google.api.services.pubsub.{Pubsub, PubsubScopes}
import parallelai.sot.containers.{Container, ContainersFixture}
import parallelai.sot.engine.ProjectFixture

trait PubsubContainerFixture extends Logging {
  this: Suite with ContainersFixture with ProjectFixture =>

  lazy val pubsubContainer = new PubsubContainer(8085)

  /*override def teardown(): Unit = {
    println(s"===> Pubsub container") // TODO - Remove
    // TODO Blitz topics
  }*/

  def pubsubClient(httpTransport: HttpTransport = getDefaultTransport, jsonFactory: JsonFactory = getDefaultJsonFactory): Pubsub = {
    val credential: GoogleCredential = GoogleCredential.getApplicationDefault.createScoped(PubsubScopes.all)

    new Pubsub.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName("test")
      .setRootUrl(pubsubContainer.ip)
      .setSuppressRequiredParameterChecks(true)
      .setSuppressAllChecks(true)
      .build()
  }

  def newTopic(name: String, pubsub: Pubsub): Topic = {
    val topic = pubsub.projects().topics().create(s"projects/${project.id}/topics/$name", new Topic).execute()
    info(s"Created topic: $topic")
    topic
  }

  def newSubscription(name: String,  pubsub: Pubsub): Subscription = {
    val subscription = pubsub.projects().subscriptions().create(s"projects/${project.id}/subscriptions/$name", new Subscription().setTopic(s"projects/${project.id}/topics/$name")).execute()
    info(s"Created subscription: $subscription")
    subscription
  }

  class PubsubContainer(exposedPort: Int) extends Container(
    imageName = "google/cloud-sdk",
    exposedPorts = Seq(exposedPort),
    waitStrategy = Option(Wait.forHttp("/")),
    commands = Seq(s"gcloud beta emulators pubsub start --project=${project.id} --host-port=0.0.0.0:$exposedPort")) {

    lazy val host: String = containerIpAddress

    lazy val port: Int = container.getMappedPort(exposedPort)

    lazy val ip: String = s"http://$host:$port"
  }
}