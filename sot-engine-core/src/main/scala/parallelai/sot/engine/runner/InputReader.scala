package parallelai.sot.engine.runner

import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.values.SCollection
import com.trueaccord.scalapb.GeneratedMessage
import parallelai.sot.engine.config.gcp.SOTUtils
import parallelai.sot.engine.generic.row.{DeepRec, Row}
import parallelai.sot.engine.io.utils.annotations.HasJSONAnnotation
import parallelai.sot.executor.model.SOTMacroConfig.{KafkaTapDefinition, PubSubTapDefinition}
import com.spotify.scio.sot.PaiScioContext._
import shapeless.{HList, LabelledGeneric}
import io.circe.generic.auto._
import io.circe.parser._

trait Reader[TAP, UTIL, ANNO, TIN <: ANNO] extends Serializable {
  type In <: HList

  def read(sc: ScioContext, tap: TAP, utils: UTIL)(implicit m: Manifest[TIN]): SCollection[Row.Aux[In]]
}

object Reader {

  type Aux[TAP, UTIL, ANNO, I <: ANNO, In0 <: HList] = Reader[TAP, UTIL, ANNO, I] {type In = In0}

  def apply[TAP, UTIL, ANNO, TIN <: ANNO](implicit reader: Reader[TAP, UTIL, ANNO, TIN]) = reader

  implicit def pubSubAvroReader[T0 <: HasAvroAnnotation, Repr <: HList](implicit
                                                                        gen: LabelledGeneric.Aux[T0, Repr],
                                                                        rdr: DeepRec[Repr]): Reader.Aux[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0, rdr.Out] =
    new Reader[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0] {
      type In = rdr.Out

      def read(sc: ScioContext, tap: PubSubTapDefinition, utils: SOTUtils)(implicit m: Manifest[T0]): SCollection[Row.Aux[rdr.Out]] = {
        sc.typedPubSubAvro[T0](tap, utils).map(a => Row[rdr.Out](rdr(gen.to(a))))
      }
    }

  implicit def pubSubProtobuf[T0 <: GeneratedMessage with com.trueaccord.scalapb.Message[T0], Repr <: HList](implicit
                                                                                                             messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0],
                                                                                                             gen: LabelledGeneric.Aux[T0, Repr],
                                                                                                             rdr: DeepRec[Repr]): Reader.Aux[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0, rdr.Out] =
    new Reader[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0] {
      type In = rdr.Out

      def read(sc: ScioContext, tap: PubSubTapDefinition, utils: SOTUtils)(implicit m: Manifest[T0]): SCollection[Row.Aux[rdr.Out]] = {
        sc.typedPubSubProto[T0](tap, utils).map(a => Row[rdr.Out](rdr(gen.to(a))))
      }
    }

  implicit def pubSubJSON[T0 <: HasJSONAnnotation, Repr <: HList](implicit
                                                                  ev: io.circe.Decoder[T0],
                                                                  gen: LabelledGeneric.Aux[T0, Repr],
                                                                  rdr: DeepRec[Repr]): Reader.Aux[PubSubTapDefinition, SOTUtils, HasJSONAnnotation, T0, rdr.Out] =
    new Reader[PubSubTapDefinition, SOTUtils, HasJSONAnnotation, T0] {
      type In = rdr.Out

      def read(sc: ScioContext, tap: PubSubTapDefinition, utils: SOTUtils)(implicit m: Manifest[T0]): SCollection[Row.Aux[rdr.Out]] = {
        sc.typedPubSubJSON[T0](tap, utils).map(a => Row[rdr.Out](rdr(gen.to(a))))
      }
    }

  implicit def kafkaProtobuf[T0 <: GeneratedMessage with com.trueaccord.scalapb.Message[T0], Repr <: HList]( implicit
                                                                                                             messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0],
                                                                                                             gen: LabelledGeneric.Aux[T0, Repr],
                                                                                                             rdr: DeepRec[Repr]): Reader.Aux[KafkaTapDefinition, SOTUtils, GeneratedMessage, T0, rdr.Out] =
    new Reader[KafkaTapDefinition, SOTUtils, GeneratedMessage, T0] {
      type In = rdr.Out

      def read(sc: ScioContext, tap: KafkaTapDefinition, utils: SOTUtils)(implicit m: Manifest[T0]): SCollection[Row.Aux[rdr.Out]] = {
        sc.typedKafkaProto[T0](tap, utils).map(a => Row[rdr.Out](rdr(gen.to(a))))
      }
    }

  implicit def kafkaJSON[T0 <: HasJSONAnnotation, Repr <: HList](implicit
                                                                  ev: io.circe.Decoder[T0],
                                                                  gen: LabelledGeneric.Aux[T0, Repr],
                                                                  rdr: DeepRec[Repr]): Reader.Aux[KafkaTapDefinition, SOTUtils, HasJSONAnnotation, T0, rdr.Out] =
    new Reader[KafkaTapDefinition, SOTUtils, HasJSONAnnotation, T0] {
      type In = rdr.Out

      def read(sc: ScioContext, tap: KafkaTapDefinition, utils: SOTUtils)(implicit m: Manifest[T0]): SCollection[Row.Aux[rdr.Out]] = {
        sc.typedKafkaJSON[T0](tap, utils).map(a => Row[rdr.Out](rdr(gen.to(a))))
      }
    }
}