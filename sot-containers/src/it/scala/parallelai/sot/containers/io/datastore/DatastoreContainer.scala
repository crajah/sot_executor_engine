package parallelai.sot.containers.io.datastore

import scala.collection.JavaConversions._
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.{Wait, WaitStrategy}
import com.dimafeng.testcontainers.SingleContainer
import parallelai.sot.containers.Environment

/**
  *
  * @param imageName String Required name of image - if a tag is not provided then "latest" is assumed
  * @param datastorePort Port Outside container that maps to the exposedPort
  * @param exposedPort Port Used by the container services that will be exposed to the outside for mapping against
  * @param environment Map[String, String] Environment variables
  * @param waitStrategy WaitStrategy State how to wait for the container to be up and running (NOTE the nasty default of null allowing for an easier client call)
  */
class DatastoreContainer(val imageName: String = "google/cloud-sdk:latest",
                         val datastorePort: Int = Environment.freePort(),
                         val exposedPort: Int = 8081,
                         val environment: Map[String, String] = Map.empty,
                         // val classpathResourceMapping: Seq[(String, String, BindMode)] = Seq() TODO - Maybe
                         val waitStrategy: WaitStrategy = Wait.forHttp("/")) extends SingleContainer[GenericContainer[_]] {

  implicit val container: GenericContainer[_] = {
    val container = new GenericContainer(if (imageName contains ":") imageName else s"$imageName:latest")

    container.addExposedPorts(exposedPort)
    container.setPortBindings(Seq(s"$datastorePort:$exposedPort"))
    environment foreach { case (k, v) => container.addEnv(k, v) }
    container.setCommand(s"gcloud beta emulators datastore start --project=bi-crm-poc --host-port=0.0.0.0:$exposedPort") // TODO Remove hardcoded project
    container.setWaitStrategy(waitStrategy)

    container
  }
}