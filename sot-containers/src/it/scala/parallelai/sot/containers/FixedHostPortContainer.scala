package parallelai.sot.containers

import com.dimafeng.testcontainers.SingleContainer
import org.testcontainers.containers.wait.WaitStrategy
import org.testcontainers.containers.{FixedHostPortGenericContainer}

import scala.language.implicitConversions

/**
  * Instantiate a Container - This instantiation is equivalent to for example (subject to parameter values):
  * <pre>
  *   docker run -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=0.0.0.0 --env ADVERTISED_PORT=9092 all4it/local-kafka:v2
  * </pre>
  *
  * @param imageName String Required name of image - if a tag is not provided then "latest" is assumed
  * @param exposedPorts Seq[Int] Used by the container services that will be exposed to the outside for mapping against
  * @param environment Map[String, String] Environment variables
  * @param waitStrategy Option[WaitStrategy] State how to wait for the container to be up and running (NOTE the nasty default of null allowing for an easier client call)
  * @param commands Seq[String] The commands to run against the instantiated container
  *
  * Note - in contrast to Container this will map exposedPorts to the same ports in the Docker host
  */

class FixedHostPortContainer(val imageName: String,
                override val exposedPorts: Seq[Int] = Seq.empty,
                val environment: Map[String, String] = Map.empty,
                val waitStrategy: Option[WaitStrategy] = None,
                val commands: Seq[String] = Seq.empty) extends SingleContainer[FixedHostPortGenericContainer[_]] {

  implicit val container: FixedHostPortGenericContainer[_] = {

    // Must fully qualify the type of container here, otherwise it failes with runtime error possibly because
    // container.withFixedExposedPort overwrites itself with Nothing
    val container: FixedHostPortGenericContainer[_] = new FixedHostPortGenericContainer(if (imageName contains ":") imageName else s"$imageName:latest")

    exposedPorts foreach { case p => container.withFixedExposedPort(p, p) }

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