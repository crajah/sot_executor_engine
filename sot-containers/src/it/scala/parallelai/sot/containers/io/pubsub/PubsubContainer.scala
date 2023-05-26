package parallelai.sot.containers.io.pubsub

import scala.collection.JavaConversions._
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.{Wait, WaitStrategy}
import com.dimafeng.testcontainers.SingleContainer

class PubsubContainer(val imageName: String = "google/cloud-sdk:latest",
                      val datastorePort: Int = 8085, // TODO - Remove hard coding
                      val exposedPort: Int = 8085,
                      val environment: Map[String, String] = Map.empty,
                      // val classpathResourceMapping: Seq[(String, String, BindMode)] = Seq() TODO - Maybe
                      val waitStrategy: WaitStrategy = Wait.forHttp("/")) extends SingleContainer[GenericContainer[_]] {

  implicit val container: GenericContainer[_] = {
    val container = new GenericContainer(if (imageName contains ":") imageName else s"$imageName:latest")

    container.addExposedPorts(exposedPort)
    container.setPortBindings(Seq(s"$datastorePort:$exposedPort"))
    environment foreach { case (k, v) => container.addEnv(k, v) }
    container.setCommand(s"gcloud beta emulators pubsub start --project=bi-crm-poc --host-port=0.0.0.0:$exposedPort") // TODO Remove hardcoded project
    container.setWaitStrategy(waitStrategy)

    container
  }
}