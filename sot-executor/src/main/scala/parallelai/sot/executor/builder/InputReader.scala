package parallelai.sot.executor.builder

import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition
import parallelai.sot.executor.scio.PaiScioContext._

trait InputReader[T, C, A] {
  def read[In <: A : Manifest](sc: ScioContext, tap: T, config: C): SCollection[In]
}

object InputReader {
  def apply[T, C, A]()(implicit inputReader: InputReader[T, C, A]) = inputReader

  implicit def pubSubReader = new InputReader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation] {
    def read[In <: HasAvroAnnotation : Manifest](sc: ScioContext, tap: PubSubTapDefinition, config: GcpOptions): SCollection[In] =
      sc.typedPubSub[In](config.getProject, tap.topic)
  }

}