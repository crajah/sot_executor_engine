package parallelai.sot.containers

import java.util.concurrent.TimeUnit
import org.scalatest.Suite
import com.dimafeng.testcontainers.{ForAllTestContainer, ForEachTestContainer}

trait ContainersFixture {
  val container: com.dimafeng.testcontainers.Container

  def setup(): Unit = ()

  def teardown(): Unit = ()
}

trait ForAllContainersFixture extends ContainersFixture with ForAllTestContainer {
  self: Suite =>

  /**
    * Start container delay to let it get going
    */
  override def afterStart(): Unit = {
    TimeUnit.SECONDS.sleep(3)
    setup()
    super.afterStart()
  }

  override def beforeStop(): Unit = {
    teardown()
    super.beforeStop()
  }
}

trait ForEachContainersFixture extends ContainersFixture with ForEachTestContainer {
  self: Suite =>

  /**
    * Start container delay to let it get going
    */
  override def afterStart(): Unit = {
    TimeUnit.SECONDS.sleep(3)
    setup()
    super.afterStart()
  }

  override def beforeStop(): Unit = {
    teardown()
    super.beforeStop()
  }
}