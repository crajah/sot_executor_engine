package parallelai.sot.executor.builder

import com.google.protobuf.Message
import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition
import parallelai.sot.executor.protobuf.PBReader
import parallelai.sot.executor.scio.PaiScioContext._
import com.trueaccord.scalapb.GeneratedMessage

import scala.reflect.ClassTag

trait Reader[TAP, CONFIG, ANNO, TIN <: ANNO] {
  def read(sc: ScioContext, tap: TAP, config: CONFIG)(implicit m: Manifest[TIN]): SCollection[TIN]
}

object Reader {
  def apply[TAP, CONFIG, ANNO, TIN <: ANNO](implicit reader: Reader[TAP, CONFIG, ANNO, TIN]) = reader

  implicit def pubSubAvroReader[T0 <: HasAvroAnnotation]: Reader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] = new Reader[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] {
    def read(sc: ScioContext, tap: PubSubTapDefinition, config: GcpOptions)(implicit m: Manifest[T0]): SCollection[T0] = {
      sc.typedPubSubAvro[T0](config.getProject, tap.topic)
    }
  }

  implicit def pubSubProtobuf[T0 <: GeneratedMessage  with com.trueaccord.scalapb.Message[T0]](implicit messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0]): Reader[PubSubTapDefinition, GcpOptions, GeneratedMessage, T0] = new Reader[PubSubTapDefinition, GcpOptions, GeneratedMessage, T0] {
    def read(sc: ScioContext, tap: PubSubTapDefinition, config: GcpOptions)(implicit m: Manifest[T0]): SCollection[T0] = {
      sc.typedPubSubProto[T0](config.getProject, tap.topic)
    }
  }
}