package parallelai.sot.engine.scio

import scala.language.implicitConversions
import scala.reflect.ClassTag
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.testing.TestPipeline
import org.scalatest.Suite
import com.spotify.scio.testing.{PipelineTestUtils, SCollectionMatchers}
import com.spotify.scio.values.SCollection

trait PipelineSpec extends PipelineTestUtils with SCollectionMatchers {
  this: Suite =>

  def pipe[T](test: Pipeline => T): T = {
    val pipeline = TestPipeline.fromOptions(PipelineOptionsFactory.fromArgs("--stableUniqueNames=WARNING").create()).enableAbandonedNodeEnforcement(false)

    try {
      test(pipeline)
    } finally {
      pipeline.run
      () // We don't care about the return value of "run"
    }
  }

  def containsInAnyOrder[T: ClassTag](values: T*): IterableMatcher[SCollection[T], T] = containInAnyOrder(values.toSeq)
}
