package parallelai.sot.executor.builder

import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.values.SCollection
import com.trueaccord.scalapb.GeneratedMessage
import parallelai.sot.executor.common.SOTUtils
import parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition
import parallelai.sot.executor.scio.PaiScioContext._

trait Reader[TAP, UTIL, ANNO, TIN <: ANNO] {
  def read(sc: ScioContext, tap: TAP, utils: UTIL)(implicit m: Manifest[TIN]): SCollection[TIN]
}

object Reader {
  def apply[TAP, UTIL, ANNO, TIN <: ANNO](implicit reader: Reader[TAP, UTIL, ANNO, TIN]) = reader

  implicit def pubSubAvroReader[T0 <: HasAvroAnnotation]: Reader[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0] = new Reader[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0] {
    def read(sc: ScioContext, tap: PubSubTapDefinition, utils: SOTUtils)(implicit m: Manifest[T0]): SCollection[T0] = {
      sc.typedPubSubAvro[T0](utils.getProject, tap.topic)
    }
  }

  implicit def pubSubProtobuf[T0 <: GeneratedMessage  with com.trueaccord.scalapb.Message[T0]](implicit messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0]): Reader[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0] = new Reader[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0] {
    def read(sc: ScioContext, tap: PubSubTapDefinition, utils: SOTUtils)(implicit m: Manifest[T0]): SCollection[T0] = {
      sc.typedPubSubProto[T0](utils.getProject, tap.topic)
    }
  }
}