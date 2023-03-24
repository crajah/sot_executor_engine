package parallelai.sot.executor.builder

import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition
import parallelai.sot.executor.scio.PaiScioContext._
import shapeless.{HList, LabelledGeneric}

trait Reader[TAP, CONFIG, ANNO, TIN <: ANNO] {
  type TINGEN <: HList
  def read(sc: ScioContext, tap: TAP, config: CONFIG)(implicit m: Manifest[TIN]): SCollection[Row[TINGEN]]
}

object Reader {

  type Aux[TAP, CONFIG, ANNO, TIN <: ANNO, TINGEN0 <: HList] = Reader[TAP, CONFIG, ANNO, TIN] {type TINGEN = TINGEN0}

  def apply[TAP, CONFIG, ANNO, TIN <: ANNO](implicit reader: Reader[TAP, CONFIG, ANNO, TIN]) = reader

  implicit def pubSubAvroReader[T0 <: HasAvroAnnotation with Product, TINGEN <: HList](implicit labelledGeneric: LabelledGeneric.Aux[T0,TINGEN]): Reader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] = new Reader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] {
    type TINGEN = labelledGeneric.Repr
    def read(sc: ScioContext, tap: PubSubTapDefinition, config: GcpOptions)(implicit m: Manifest[T0]): SCollection[Row[TINGEN]] = {
      sc.typedPubSub[T0](config.getProject, tap.topic).map(x => Row[T0,TINGEN](x))
    }
  }
}