package parallelai.sot.containers.io.pubsub

import java.util.UUID
import scala.collection.JavaConversions._
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.{Wait, WaitStrategy}
import com.dimafeng.testcontainers.SingleContainer
import parallelai.sot.containers.Environment

/**
  * docker run -p 8085:8085 google/cloud-sdk:latest gcloud beta emulators pubsub start --project=bi-crm-poc --host-port=0.0.0.0:8085
  * @param imageName String Required name of image - if a tag is not provided then "latest" is assumed
  * @param projectId String ID of the project assigned to this pubsub
  * @param port Port Outside container that maps to the exposedPort
  * @param exposedPort Port Used by the container services that will be exposed to the outside for mapping against
  * @param environment Map[String, String] Environment variables
  * @param waitStrategy WaitStrategy State how to wait for the container to be up and running (NOTE the nasty default of null allowing for an easier client call)
  */
class PubsubContainer(val imageName: String = "google/cloud-sdk:latest",
                      val projectId: String = UUID.randomUUID().toString, // TODO Pass in Project (currently this is in engine-core)
                      val port: Int = Environment.freePort,
                      val exposedPort: Int = 8085,
                      val environment: Map[String, String] = Map.empty,
                      // val classpathResourceMapping: Seq[(String, String, BindMode)] = Seq() TODO - Maybe
                      val waitStrategy: WaitStrategy = Wait.forHttp("/")) extends SingleContainer[GenericContainer[_]] {

  implicit val container: GenericContainer[_] = {
    val container = new GenericContainer(if (imageName contains ":") imageName else s"$imageName:latest") {
      override def start(): Unit = super.start()
    }

    container.addExposedPorts(exposedPort)
    container.setPortBindings(Seq(s"$port:$exposedPort"))
    environment foreach { case (k, v) => container.addEnv(k, v) }
    container.setCommand(s"gcloud beta emulators pubsub start --project=$projectId --host-port=0.0.0.0:$exposedPort")
    container.setWaitStrategy(waitStrategy)

    container
  }
}