package parallelai.sot.engine.io.pubsub

import org.scalatest.Suite
import parallelai.sot.containers.ContainersSpec
import parallelai.sot.containers.io.pubsub.PubsubContainer

trait PubsubContainerSpec {
  this: Suite with ContainersSpec =>

  lazy val pubsubContainer: PubsubContainer = new PubsubContainer

  override def teardown(): Unit = {
    println(s"===> Pubsub container") // TODO - Remove
    // TODO Blitz topics
  }
}