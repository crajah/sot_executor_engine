package parallelai.sot.executor

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import parallelai.sot.executor.dsl.Model._
import parallelai.sot.executor.dsl.Rules._

/**
  * Created by tamjam on 26/06/2017.
  */
class BuilderSpec extends WordSpec
  with Matchers
  with BeforeAndAfterAll {

  var ruleSet: RuleSet = _

  override def beforeAll(): Unit = {
  }

  "Builder" should {

    "read, transform and write" in {

      1 should be (1)

    }


  }


}
