package parallelai.sot.engine.runner

import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.values.SCollection
import com.trueaccord.scalapb.GeneratedMessage
import parallelai.sot.engine.config.gcp.SOTUtils
import parallelai.sot.engine.generic.row.{DeepRec, Row}
import parallelai.sot.engine.io.TapDef
import parallelai.sot.executor.model.SOTMacroConfig.{PubSubTapDefinition, TapDefinition}
import parallelai.sot.engine.runner.scio.PaiScioContext._
import shapeless.{::, HList, LabelledGeneric, Poly2}

trait Reader[TAP, UTIL, ANNO, TIN <: ANNO] extends Serializable {
  type In <: HList

  def read(sc: ScioContext, tap: TAP, utils: UTIL)(implicit m: Manifest[TIN]): SCollection[Row.Aux[In]]
}

object Reader {

  type Aux[TAP, UTIL, ANNO, I <: ANNO, In0 <: HList] = Reader[TAP, UTIL, ANNO, I] {type In = In0}

  def apply[TAP, UTIL, ANNO, TIN <: ANNO](implicit reader: Reader[TAP, UTIL, ANNO, TIN]) = reader

  implicit def pubSubAvroReader[T0 <: HasAvroAnnotation, Repr <: HList](implicit
                                                                        gen: LabelledGeneric.Aux[T0, Repr],
                                                                        rdr: DeepRec[Repr]): Reader.Aux[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0, rdr.Out] = new Reader[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0] {
    type In = rdr.Out

    def read(sc: ScioContext, tap: PubSubTapDefinition, utils: SOTUtils)(implicit m: Manifest[T0]): SCollection[Row.Aux[rdr.Out]] = {
      sc.typedPubSubAvro[T0](utils.getProject, tap.topic).map(a => Row[rdr.Out](rdr(gen.to(a))))
    }
  }

  implicit def pubSubProtobuf[T0 <: GeneratedMessage with com.trueaccord.scalapb.Message[T0], Repr <: HList](implicit
                                                                                                             messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0],
                                                                                                             gen: LabelledGeneric.Aux[T0, Repr],
                                                                                                             rdr: DeepRec[Repr]): Reader.Aux[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0, rdr.Out] = new Reader[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0] {
    type In = rdr.Out

    def read(sc: ScioContext, tap: PubSubTapDefinition, utils: SOTUtils)(implicit m: Manifest[T0]): SCollection[Row.Aux[rdr.Out]] = {
      sc.typedPubSubProto[T0](utils.getProject, tap.topic).map(a => Row[rdr.Out](rdr(gen.to(a))))
    }
  }
}

object reader2 extends Poly2 {
  implicit def reader[TAP <: TapDefinition, In <: ANNO : Manifest, UTIL, ANNO, SCOLS <: HList]
  (implicit
   reader: Reader[TAP, UTIL, ANNO, In]
  ): Case.Aux[
    (ScioContext, SCOLS, UTIL),
    TapDef[TAP, UTIL, ANNO, In],
    (ScioContext, SCollection[Row.Aux[reader.In]] :: SCOLS, UTIL)
    ] = at[(ScioContext, SCOLS, UTIL), TapDef[TAP, UTIL, ANNO, In]] {
    case ((sc, scols, utils), t) =>
      val scol = reader.read(sc, t.tapDefinition, utils)
      (sc, scol :: scols, utils)
  }
}