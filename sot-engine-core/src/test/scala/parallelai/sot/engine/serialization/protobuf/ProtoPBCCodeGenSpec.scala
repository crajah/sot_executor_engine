package parallelai.sot.engine.serialization.protobuf

import scala.meta._
import org.scalatest.{FlatSpec, Matchers}

class ProtoPBCCodeGenSpec extends FlatSpec with Matchers {
  "ProtoPBCCodeGen service" should "generate source code from proto schema" in {
    val code = ProtoPBCCodeGen.executeAll("cGFja2FnZSBjb3JlOw0KDQpvcHRpb24gamF2YV9wYWNrYWdlID0gInBhcmFsbGVsYWkuc290LmV4ZWN1dG9yLmJ1aWxkZXIuU09UQnVpbGRlciI7DQpvcHRpb24gamF2YV9vdXRlcl9jbGFzc25hbWUgPSAiQ29yZU1vZGVsIjsNCg0KbWVzc2FnZSBNZXNzYWdlIHsNCiAgICANCiAgIA0KICAgIHJlcXVpcmVkIHN0cmluZyB1c2VyID0gMTsNCiAgICByZXF1aXJlZCBzdHJpbmcgdGVhbU5hbWUgPSAyOw0KICAgIHJlcXVpcmVkIGludDY0IHNjb3JlID0gMzsNCiAgICByZXF1aXJlZCBpbnQ2NCBldmVudFRpbWUgPSA0Ow0KICAgIHJlcXVpcmVkIHN0cmluZyBldmVudFRpbWVTdHIgPSA1Ow0KDQp9")
    val stats = code.parse[Source].get.stats

    stats.head match {
      case q"package $name  {..$statements}" =>
        statements.size shouldEqual 3
      case _ =>
        fail()
    }
  }
}