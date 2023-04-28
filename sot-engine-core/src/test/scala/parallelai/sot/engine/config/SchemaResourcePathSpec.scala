package parallelai.sot.engine.config

import org.scalatest.{MustMatchers, WordSpec}
import parallelai.sot.engine.system._

class SchemaResourcePathSpec extends WordSpec with MustMatchers {
  "Schema resource path" should {
    "default" in {
      withSystemProperties("config.resource" -> "application.test.conf") {
        SchemaResourcePath().value mustEqual "my-schema.json"
      }
    }
  }
}