package parallelai.sot.executor.builder

import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition
import parallelai.sot.executor.scio.PaiScioContext._

trait Reader[TAP, CONFIG, ANNO, TIN] {
  def read(sc: ScioContext, tap: TAP, config: CONFIG)(implicit m: Manifest[TIN]): Result.Aux[TIN]
}

object Reader {
  def apply[TAP, CONFIG, ANNO, TIN](implicit reader: Reader[TAP, CONFIG, ANNO, TIN]) = reader

  implicit def pubSubAvroReader[T0 <: HasAvroAnnotation]: Reader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] = new Reader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] {
    def read(sc: ScioContext, tap: PubSubTapDefinition, config: GcpOptions)(implicit m: Manifest[T0]): Result.Aux[T0] = {
      Result.instance(sc.typedPubSub[T0](config.getProject, tap.topic))
    }
  }
}

trait Result {
  type R
  val res: SCollection[R]
}

object Result {
  type Aux[R0] = Result {type R = R0}

  implicit def instance[R0](l: SCollection[R0]): Result.Aux[R0] = new Result {
    type R = R0
    val res = l
  }
}