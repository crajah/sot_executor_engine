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

  implicit def pubSubReader: InputReader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation] = new InputReader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation] {
    def read[In <: HasAvroAnnotation : Manifest](sc: ScioContext, tap: PubSubTapDefinition, config: GcpOptions): SCollection[In] =
      sc.typedPubSub[In](config.getProject, tap.topic)
  }

}


trait Reader[TAP, CONFIG, ANNO, TIN] {
  def read(sc: ScioContext, tap: TAP, config: CONFIG)(implicit m: Manifest[TIN]): SCollection[TIN]
}

object Reader {
  def apply[TAP, CONFIG, ANNO, TIN](implicit reader: Reader[TAP, CONFIG, ANNO, TIN]) = reader

  implicit def pubSubAvroReader[T0 <: HasAvroAnnotation]: Reader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] = new Reader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] {
    def read(sc: ScioContext, tap: PubSubTapDefinition, config: GcpOptions)(implicit m: Manifest[T0]): SCollection[T0] = {
      sc.typedPubSub[T0](config.getProject, tap.topic)

    }
  }
}
