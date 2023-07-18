package parallelai.sot.engine.runner

import parallelai.sot.engine.generic.row.Row
import scalaz.Scalaz._
import org.scalatest.{MustMatchers, WordSpec}
import parallelai.sot.engine.runner.SCollectionStateMonad._
import parallelai.sot.engine.scio.PipelineSpec
import shapeless._
import com.spotify.scio.values.SCollection
import parallelai.sot.engine.config.gcp.SOTUtils
import parallelai.sot.engine.io.TapDef
import parallelai.sot.executor.model.SOTMacroConfig.SeqTapDefinition

class SCollectionStateMonadFilterITSpec extends WordSpec with MustMatchers with PipelineSpec {
  "SCollection state monad transformation" should {
    "read from in memory tap and produce result" in runWithContext { scioContext =>
      val source = Seq(Test("scooby"), Test("shaggy"))

      val tapDefinition = TapDef[SeqTapDefinition[Test], SOTUtils, Product, Test](SeqTapDefinition(content = source))

      val job = init[HNil].flatMap(_ => read(scioContext, tapDefinition, new SOTUtils(pipelineOptions)))

      val result: SCollection[Test] = job.run(HNil)._1.head.map { r => // TODO ._1.head .....not nice!!!
        Row.to[Test].from(r.hList)
      }

      result must containsInAnyOrder(source: _*)
    }
  }
}

case class Test(blah: String)