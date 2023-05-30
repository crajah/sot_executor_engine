package parallelai.sot.engine

import org.scalatest.Suite

trait ProjectFixture {
  this: Suite =>

  lazy val project = Project("project-id-test")
}