package parallelai.sot.executor.builder

import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition
import parallelai.sot.executor.scio.PaiScioContext._

trait Reader[TAP, CONFIG, ANNO, TIN <: ANNO] {
  def read(sc: ScioContext, tap: TAP, config: CONFIG)(implicit m: Manifest[TIN]): Result.Aux[ANNO, TIN]
}

object Reader {
  def apply[TAP, CONFIG, ANNO, TIN <: ANNO](implicit reader: Reader[TAP, CONFIG, ANNO, TIN]) = reader

  implicit def pubSubAvroReader[T0 <: HasAvroAnnotation]: Reader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] = new Reader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] {
    def read(sc: ScioContext, tap: PubSubTapDefinition, config: GcpOptions)(implicit m: Manifest[T0]): Result.Aux[HasAvroAnnotation, T0] = {
      Result.instance[HasAvroAnnotation, T0](sc.typedPubSub[T0](config.getProject, tap.topic))
    }
  }
}

trait Result {
  type A
  type R
  val res: SCollection[R]
}

object Result {
  type Aux[A0, R0] = Result {type A = A0; type R = R0}

  implicit def instance[A0, R0](l: SCollection[R0]): Result.Aux[A0, R0] = new Result {
    type A = A0
    type R = R0
    val res = l
  }
}