package parallelai.sot.containers

import java.util.concurrent.TimeUnit
import org.scalatest.Suite
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, ForEachTestContainer}

trait ContainersSpec {
  val container: Container

  def setup(): Unit = ()

  def teardown(): Unit = ()
}

trait ForAllContainersSpec extends ContainersSpec with ForAllTestContainer {
  self: Suite =>

  /**
    * Start container delay to let it get going
    */
  override def afterStart(): Unit = {
    TimeUnit.SECONDS.sleep(3)
    super.afterStart()
    setup()
  }

  override def beforeStop(): Unit = {
    super.beforeStop()
    teardown()
  }
}

trait ForEachContainersSpec extends ContainersSpec with ForEachTestContainer {
  self: Suite =>

  /**
    * Start container delay to let it get going
    */
  override def afterStart(): Unit = {
    TimeUnit.SECONDS.sleep(3)
    super.afterStart()
    setup()
  }

  override def beforeStop(): Unit = {
    super.beforeStop()
    teardown()
  }
}