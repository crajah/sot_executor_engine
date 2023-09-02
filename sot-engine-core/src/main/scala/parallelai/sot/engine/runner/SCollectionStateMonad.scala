package parallelai.sot.engine.runner

import com.spotify.scio.ScioContext
import com.spotify.scio.sot.tensorflow._
import com.spotify.scio.sot.accumulator._
import com.spotify.scio.values.{SCollection, WindowOptions}
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import org.joda.time.Duration
import org.tensorflow.Tensor
import parallelai.sot.engine.Project
import parallelai.sot.engine.generic.row.{DeepRec, Row}
import parallelai.sot.engine.io.datastore.{FromEntity, Kind, ToEntity}
import parallelai.sot.engine.io.{SchemalessTapDef, TapDef}
import parallelai.sot.executor.model.SOTMacroConfig.TapDefinition
import shapeless.labelled.FieldType
import shapeless.ops.hlist.{At, Length, Prepend}
import shapeless.ops.record.Values
import shapeless.{::, HList, HNil, LabelledGeneric, Nat, Witness}

import scala.reflect.ClassTag
import scalaz.IndexedState

object SCollectionStateMonad {

  type GroupedSCollection[K, L] = SCollection[Row.Aux[FieldType[Witness.`'_1`.T, K] :: FieldType[Witness.`'_2`.T, List[L]] :: HNil]]
  type JoinedSCollection[K, L1, L2] = SCollection[Row.Aux[FieldType[Witness.`'_1`.T, K] :: FieldType[Witness.`'_2`.T, (FieldType[Witness.`'_1`.T, L1] :: FieldType[Witness.`'_2`.T, L2] :: HNil)] :: HNil]]

  def read[L <: HList, SCOLS <: HList, SCOLOUT <: HList, UTIL, TAP <: TapDefinition, ANNO, In <: ANNO : Manifest](sc: ScioContext, tap: TapDef[TAP, UTIL, ANNO, In], utils: UTIL)
                                                                                                                 (implicit reader: Reader.Aux[TAP, UTIL, ANNO, In, L], prepend: Prepend.Aux[SCOLS, SCollection[Row.Aux[L]] :: HNil, SCOLOUT]): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState { sColls =>
      val sColl = reader.read(sc, tap.tapDefinition, utils)
      val res = prepend(sColls, sColl :: HNil)
      (res, res)
    }

  def append[L <: HList, SCOLS <: HList, SCOLOUT <: HList](sColl: SCollection[Row.Aux[L]])(implicit prepend: Prepend.Aux[SCOLS, SCollection[Row.Aux[L]] :: HNil, SCOLOUT]): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, sColl :: HNil)
      (res, res)
    })

  def filter[SCOLS <: HList, SCOLOUT <: HList, L <: HList](sCollection: SCollection[Row.Aux[L]])
                                                          (f: Row.Aux[L] => Boolean)
                                                          (implicit
                                                           prepend: Prepend.Aux[SCOLS, SCollection[Row.Aux[L]] :: HNil, SCOLOUT]
                                                          ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, sCollection.filter(f) :: HNil)
      (res, res)
    })

  def map[SCOLS <: HList, SCOLOUT <: HList, L <: HList, Out <: HList](sCollection: SCollection[Row.Aux[L]])(f: Row.Aux[L] => Row.Aux[Out])
                                                                     (implicit
                                                                      prepend: Prepend.Aux[SCOLS, SCollection[Row.Aux[Out]] :: HNil, SCOLOUT]
                                                                     ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, sCollection.map(f) :: HNil)
      (res, res)
    })


  def flatMap[SCOLS <: HList, SCOLOUT <: HList, L <: HList, Out <: HList](sCollection: SCollection[Row.Aux[L]])(f: Row.Aux[L] => TraversableOnce[Row.Aux[Out]])
                                                                         (implicit
                                                                          prepend: Prepend.Aux[SCOLS, SCollection[Row.Aux[Out]] :: HNil, SCOLOUT]
                                                                         ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, sCollection.flatMap(x=> f(x)) :: HNil)
      (res, res)
    })


  def predict[SCOLS <: HList, SCOLOUT <: HList, L <: HList, Out <: HList](sCollection: SCollection[Row.Aux[L]])
                                                                         (modelBucket: String, modelPath: String, fetchOps: Seq[String],
                                                                          inFn: Row.Aux[L] => Map[String, Tensor],
                                                                          outFn: (Row.Aux[L], Map[String, Tensor]) => Row.Aux[Out])
                                                                         (implicit
                                                                          prepend: Prepend.Aux[SCOLS, SCollection[Row.Aux[Out]] :: HNil, SCOLOUT]
                                                                         ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, sCollection.predict(modelBucket, modelPath, fetchOps)(inFn)(outFn) :: HNil)
      (res, res)
    })

  def accumulate[K: ClassTag, SCOLS <: HList, SCOLOUT <: HList, L <: HList : ClassTag, Out <: HList, I <: HList](sCollection: SCollection[Row.Aux[L]])
                                                                                                                (getValue: Row.Aux[L] => Row.Aux[I])(
                                                                                                                  defaultValue: Row.Aux[I],
                                                                                                                  keyMapper: Row.Aux[L] => K,
                                                                                                                  aggr: (Row.Aux[I], Row.Aux[I]) => Row.Aux[I],
                                                                                                                  toOut: (Row.Aux[L], Row.Aux[I]) => Row.Aux[Out],
                                                                                                                  kind: String = "")
                                                                                                                (implicit
                                                                                                                 prepend: Prepend.Aux[SCOLS, SCollection[Row.Aux[Out]] :: HNil, SCOLOUT],
                                                                                                                 toL: ToEntity[I],
                                                                                                                 fromL: FromEntity[I]
                                                                                                                ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val opKind: Option[String] = kind match {
        case "" => None
        case s => Some(s)
      }
      val project = sCollection.context.optionsAs[GcpOptions].getProject
      val datastoreSettings = opKind.map { k => (Project(project), Kind(k)) }
      val res = prepend(sColls, sCollection.accumulator(keyMapper, getValue, defaultValue, aggr, toOut, datastoreSettings) :: HNil)
      (res, res)
    })


  def withFixedWindows[SCOLS <: HList, SCOLOUT <: HList, L <: HList, Out <: HList](sCollection: SCollection[Row.Aux[L]])
                                                                                  (duration: Duration,
                                                                                   offset: Duration = Duration.ZERO,
                                                                                   options: WindowOptions = WindowOptions())
                                                                                  (implicit
                                                                                   prepend: Prepend.Aux[SCOLS, SCollection[Row.Aux[L]] :: HNil, SCOLOUT]
                                                                                  ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, sCollection.withFixedWindows(duration, offset, options) :: HNil)
      (res, res)
    })

  def withGlobalWindow[SCOLS <: HList, SCOLOUT <: HList, L <: HList, Out <: HList](sCollection: SCollection[Row.Aux[L]])
                                                                                  (options: WindowOptions = WindowOptions())
                                                                                  (implicit
                                                                                   prepend: Prepend.Aux[SCOLS, SCollection[Row.Aux[L]] :: HNil, SCOLOUT]
                                                                                  ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, sCollection.withGlobalWindow(options) :: HNil)
      (res, res)
    })


  def aggregate[SCOLS <: HList, SCOLOUT <: HList, L <: HList, Out <: HList](sCollection: SCollection[Row.Aux[L]])
                                                                           (zeroValue: Row.Aux[Out])
                                                                           (seqOp: (Row.Aux[Out], Row.Aux[L]) => Row.Aux[Out],
                                                                            combOp: (Row.Aux[Out], Row.Aux[Out]) => Row.Aux[Out])
                                                                           (implicit
                                                                            prepend: Prepend.Aux[SCOLS, SCollection[Row.Aux[Out]] :: HNil, SCOLOUT]
                                                                           ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, sCollection.aggregate(zeroValue)(seqOp, combOp) :: HNil)
      (res, res)
    })


  def combine[SCOLS <: HList, SCOLOUT <: HList, L <: HList, Out <: HList](sCollection: SCollection[Row.Aux[L]])
                                                                         (createCombiner: Row.Aux[L] => Row.Aux[Out])
                                                                         (mergeValue: (Row.Aux[Out], Row.Aux[L]) => Row.Aux[Out],
                                                                          mergeCombiners: (Row.Aux[Out], Row.Aux[Out]) => Row.Aux[Out])
                                                                         (implicit
                                                                          prepend: Prepend.Aux[SCOLS, SCollection[Row.Aux[Out]] :: HNil, SCOLOUT]
                                                                         ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, sCollection.combine(createCombiner)(mergeValue)(mergeCombiners) :: HNil)
      (res, res)
    })


  def groupBy[SCOLS <: HList, SCOLOUT <: HList, L <: HList, Out <: HList, K: Manifest](sCollection: SCollection[Row.Aux[L]])
                                                                                      (f: Row.Aux[L] => K)
                                                                                      (implicit
                                                                                       prepend: Prepend.Aux[SCOLS, GroupedSCollection[K, L] :: HNil, SCOLOUT]
                                                                                      ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, sCollection.groupBy(f).map({
        case (k: K, it: Iterable[Row.Aux[L]]) => fromTuple((k, it.toList.map(_.hList)))
      }) :: HNil)
      (res, res)
    })


  def join[J1 <: HList, J2 <: HList, K: ClassTag, L1: ClassTag, L2: ClassTag, SCOLS <: HList, SCOLOUT <: HList, W: ClassTag]
  (sColl1: SCollection[Row.Aux[J1]], sColl2: SCollection[Row.Aux[J2]])
  (implicit
   pair1: IsPair.Aux[J1, K, L1],
   pair2: IsPair.Aux[J2, K, L2],
   prepend: Prepend.Aux[SCOLS, JoinedSCollection[K, L1, L2] :: HNil, SCOLOUT]
  ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, pair1(sColl1).join(pair2(sColl2)).map(m => fromTuple(m)) :: HNil)
      (res, res)
    }
    )


  def hashJoin[J1 <: HList, J2 <: HList, K: ClassTag, L1: ClassTag, L2: ClassTag, SCOLS <: HList, SCOLOUT <: HList, W: ClassTag]
  (sColl1: SCollection[Row.Aux[J1]], sColl2: SCollection[Row.Aux[J2]])
  (implicit
   pair1: IsPair.Aux[J1, K, L1],
   pair2: IsPair.Aux[J2, K, L2],
   prepend: Prepend.Aux[SCOLS, JoinedSCollection[K, L1, L2] :: HNil, SCOLOUT]
  ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, pair1(sColl1).hashJoin(pair2(sColl2)).map(m => fromTuple(m)) :: HNil)
      (res, res)
    }
    )


  def leftOuterJoin[J1 <: HList, J2 <: HList, K: ClassTag, L1: ClassTag, L2: ClassTag, SCOLS <: HList, SCOLOUT <: HList, W: ClassTag]
  (sColl1: SCollection[Row.Aux[J1]], sColl2: SCollection[Row.Aux[J2]])
  (implicit
   pair1: IsPair.Aux[J1, K, L1],
   pair2: IsPair.Aux[J2, K, L2],
   prepend: Prepend.Aux[SCOLS, JoinedSCollection[K, L1, Option[L2]] :: HNil, SCOLOUT]
  ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, pair1(sColl1).leftOuterJoin(pair2(sColl2)).map(m => fromTuple(m)) :: HNil)
      (res, res)
    }
    )


  def hashLeftJoin[J1 <: HList, J2 <: HList, K: ClassTag, L1: ClassTag, L2: ClassTag, SCOLS <: HList, SCOLOUT <: HList, W: ClassTag]
  (sColl1: SCollection[Row.Aux[J1]], sColl2: SCollection[Row.Aux[J2]])
  (implicit
   pair1: IsPair.Aux[J1, K, L1],
   pair2: IsPair.Aux[J2, K, L2],
   prepend: Prepend.Aux[SCOLS, JoinedSCollection[K, L1, Option[L2]] :: HNil, SCOLOUT]
  ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, pair1(sColl1).hashLeftJoin(pair2(sColl2)).map(m => fromTuple(m)) :: HNil)
      (res, res)
    }
    )


  def rightOuterJoin[J1 <: HList, J2 <: HList, K: ClassTag, L1: ClassTag, L2: ClassTag, SCOLS <: HList, SCOLOUT <: HList, W: ClassTag]
  (sColl1: SCollection[Row.Aux[J1]], sColl2: SCollection[Row.Aux[J2]])
  (implicit
   pair1: IsPair.Aux[J1, K, L1],
   pair2: IsPair.Aux[J2, K, L2],
   prepend: Prepend.Aux[SCOLS, JoinedSCollection[K, Option[L1], L2] :: HNil, SCOLOUT]
  ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, pair1(sColl1).rightOuterJoin(pair2(sColl2)).map(m => fromTuple(m)) :: HNil)
      (res, res)
    }
    )


  def fullOuterJoin[J1 <: HList, J2 <: HList, K: ClassTag, L1: ClassTag, L2: ClassTag, SCOLS <: HList, SCOLOUT <: HList, W: ClassTag]
  (sColl1: SCollection[Row.Aux[J1]], sColl2: SCollection[Row.Aux[J2]])
  (implicit
   pair1: IsPair.Aux[J1, K, L1],
   pair2: IsPair.Aux[J2, K, L2],
   prepend: Prepend.Aux[SCOLS, JoinedSCollection[K, Option[L1], Option[L2]] :: HNil, SCOLOUT]
  ): IndexedState[SCOLS, SCOLOUT, SCOLOUT] =
    IndexedState(sColls => {
      val res = prepend(sColls, pair1(sColl1).fullOuterJoin(pair2(sColl2)).map(m => fromTuple(m)) :: HNil)
      (res, res)
    }
    )


  def write[L <: HList, SCOLS <: HList, SCOLOUT <: HList, UTIL, TAP <: TapDefinition, ANNO, In <: ANNO : Manifest](sCollection: SCollection[Row.Aux[L]])
                                                                                                                  (tap: TapDef[TAP, UTIL, ANNO, In], utils: UTIL)
                                                                                                                  (implicit
                                                                                                                   gen: LabelledGeneric.Aux[In, L],
                                                                                                                   writer: Writer[TAP, UTIL, ANNO, In, L]
                                                                                                                  ): IndexedState[SCOLS, SCOLS, SCOLS] =
    IndexedState(sColls => {
      writer.write(sCollection, tap.tapDefinition, utils)
      (sColls, sColls)
    })


  def writeSchemaless[L <: HList, SCOLS <: HList, SCOLOUT <: HList, UTIL, TAP <: TapDefinition, ANNO](sCollection: SCollection[Row.Aux[L]])
                                                                                                     (tap: SchemalessTapDef[TAP, UTIL, ANNO], utils: UTIL)
                                                                                                     (implicit
                                                                                                      writer: SchemalessWriter[TAP, UTIL, ANNO, L]
                                                                                                     ): IndexedState[SCOLS, SCOLS, SCOLS] =
    IndexedState(sColls => {
      writer.write(sCollection, tap.tapDefinition, utils)
      (sColls, sColls)
    })

//  def writeSchemaless1[L <: HList, SCOLS <: HList, SCOLOUT <: HList, UTIL, TAP <: TapDefinition, ANNO](sCollection: SCollection[Row.Aux[L]])
//                                                                                                     (tap: SchemalessTapDef[TAP, UTIL, ANNO], utils: UTIL)
//                                                                                                     (implicit
//                                                                                                      hListSchemaProvider: HListSchemaProvider[L],
//                                                                                                      toL: ToTableRow[L]
////                                                                                                      writer: SchemalessWriter[TAP, UTIL, ANNO, L]
//                                                                                                     ): IndexedState[SCOLS, SCOLS, SCOLS] =
//    IndexedState(sColls => {
////      writer.write(sCollection, tap.tapDefinition, utils)
//      (sColls, sColls)
//    })


  def fromTuple[A <: Product, Repr <: HList](a: A)(implicit
                                                   gen: LabelledGeneric.Aux[A, Repr],
                                                   rdr: DeepRec[Repr]): Row.Aux[rdr.Out] = {
    Row[rdr.Out](rdr(gen.to(a)))
  }

}


trait IsPair[L <: HList] extends Serializable {
  type V1
  type V2

  def apply(l: SCollection[Row.Aux[L]]): SCollection[(V1, V2)]
}

object IsPair {
  type Aux[L <: HList, K0, V0] = IsPair[L] {type V1 = K0; type V2 = V0}

  implicit def pairSelector[L <: HList, K1, K2, VV1, VV2, VALS <: HList]
  (implicit len: Length.Aux[L, Nat._2], values: Values.Aux[L, VALS], at0: At.Aux[VALS, Nat._0, VV1], at1: At.Aux[VALS, Nat._1, VV2]
  ): IsPair.Aux[L, VV1, VV2] = new IsPair[L] {
    type V1 = VV1
    type V2 = VV2

    def apply(l: SCollection[Row.Aux[L]]): SCollection[(VV1, VV2)] = l.map(r => (at0(values(r.hList)), at1(values(r.hList))))
  }
}