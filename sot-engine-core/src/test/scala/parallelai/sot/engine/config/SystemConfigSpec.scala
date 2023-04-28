package parallelai.sot.engine.config

import org.scalatest.{MustMatchers, WordSpec}
import parallelai.sot.engine.system._

class SystemConfigSpec extends WordSpec with MustMatchers {
  "System config" should {
    "read" in {
      withSystemProperties("config.resource" -> "application.test.conf") {
        Foo().bar mustEqual "mybar"
      }
    }
  }

  class Foo(val bar: String)

  object Foo {
    def apply(): Foo = SystemConfig[Foo]("foo")
  }
}