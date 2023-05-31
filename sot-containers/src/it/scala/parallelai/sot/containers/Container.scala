package parallelai.sot.containers

import scala.language.implicitConversions
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.WaitStrategy
import com.dimafeng.testcontainers.SingleContainer

/**
  * Instantiate a Container - This instantiation is equivalent to for example:
  * <pre>
  *   docker run -p 8080:8081 google/cloud-sdk:latest gcloud beta emulators datastore start --project=bi-crm-poc --host-port=0.0.0.0:8081
  * </pre>
  *
  * @param imageName String Required name of image - if a tag is not provided then "latest" is assumed
  * @param exposedPorts Seq[Int] Used by the container services that will be exposed to the outside for mapping against
  * @param environment Map[String, String] Environment variables
  * @param waitStrategy Option[WaitStrategy] State how to wait for the container to be up and running (NOTE the nasty default of null allowing for an easier client call)
  * @param commands Seq[String] The commands to run against the instantiated container
  *
  * Note - Regarding port bindings, any exposed ports are bound to (externally) by calling the (wrapped) container's "getMappedPort" with the exposed port of the desired service.
  */
class Container(val imageName: String,
                override val exposedPorts: Seq[Int] = Seq.empty,
                val environment: Map[String, String] = Map.empty,
                // classpathResourceMapping: Seq[(String, String, BindMode)] = Seq() TODO - Maybe
                val waitStrategy: Option[WaitStrategy] = None,
                val commands: Seq[String] = Seq.empty) extends SingleContainer[GenericContainer[_]] {

  implicit val container: GenericContainer[_] = {
    val container = new GenericContainer(if (imageName contains ":") imageName else s"$imageName:latest")

    container.addExposedPorts(exposedPorts: _*)

    environment foreach { case (k, v) => container.addEnv(k, v) }

    waitStrategy foreach container.setWaitStrategy

    commands match {
      case Seq(c) => container.setCommand(c)
      case cmds if cmds.nonEmpty => container.setCommandParts(cmds.toArray)
      case _ =>
    }

    container
  }
}