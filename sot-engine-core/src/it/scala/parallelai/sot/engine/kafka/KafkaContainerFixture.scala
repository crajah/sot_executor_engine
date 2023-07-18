package parallelai.sot.engine.kafka

import java.net.URI

import org.scalatest.Suite
import org.testcontainers.containers.wait.Wait
import parallelai.sot.containers.{FixedHostPortContainer}

/**
  * docker run -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=0.0.0.0 --env ADVERTISED_PORT=9092 all4it/local-kafka:v2
  */
trait KafkaContainerFixture {
  this: Suite =>

  val container = new FixedHostPortContainer(
    imageName = "all4it/local-kafka:v2",
    environment = Map("ADVERTISED_HOST" -> "0.0.0.0", "ADVERTISED_PORT" -> "9092"),
    exposedPorts = Seq(9092, 2181),
    waitStrategy = Option(Wait.forListeningPort()))
}
