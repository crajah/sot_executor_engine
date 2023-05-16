package parallelai.sot.engine.runner

import com.spotify.scio.ScioContext
import com.spotify.scio.values.{SCollection, WindowOptions}
import org.joda.time.Duration
import parallelai.sot.engine.generic.row.Row
import parallelai.sot.engine.io.TapDef
import parallelai.sot.executor.model.SOTMacroConfig.TapDefinition
import shapeless.{HList, HNil, Witness}
import shapeless.labelled.{FieldType, field}
import shapeless.ops.hlist.LeftFolder
import shapeless.ops.record.{LacksKey, Remover, Updater}
import shapeless.syntax.singleton._

import scalaz.IndexedState
import shapeless.::

object SCollectionStateMonad {


  def read[L <: HList, TAP <: TapDefinition, UTIL, ANNO, TIN <: ANNO : Manifest](tap: TapDef[TAP, UTIL, ANNO, TIN], utils: UTIL)
                                                                                (implicit
                                                                                 reader: Reader.Aux[TAP, UTIL, ANNO, TIN, L]
                                                                                ): IndexedState[ScioContext, SCollection[Row.Aux[L]], Unit] =
    IndexedState(sc => ({
      reader.read(sc, tap.tapDefinition, utils)
    }, ()))

  def addField[V, L <: HList, Out <: HList](k: Witness, v: V)(implicit
                                                              updater: Updater.Aux[L, FieldType[k.T, V], Out],
                                                              lk: LacksKey[L, k.T]): IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[Out]], Unit] = {
    IndexedState(sColl => (sColl.map(r => Row[Out](updater(r.hl, field[k.T](v)))), ()))
  }

  def updateField[V, L <: HList, Out <: HList](k: Witness, v: V)(implicit updater: Updater.Aux[L, FieldType[k.T, V], Out]): IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[Out]], Unit] = {
    IndexedState(sColl => (sColl.map(r => Row[Out](updater(r.hl, field[k.T](v)))), ()))
  }


  def removeField[L <: HList, V, Out <: HList](k: Witness)(implicit remover: Remover.Aux[L, k.T, (V, Out)]): IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[Out]], Unit] =
    IndexedState(sColl => (sColl.map(r => Row[Out](remover(r.hl)._2)), ()))


  def filter[L <: HList](f: Row.Aux[L] => Boolean): IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[L]], Unit] =
    IndexedState(sColl => (sColl.filter(f), ()))

  def map[L <: HList, Out <: HList](f: Row.Aux[L] => Row.Aux[Out]): IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[Out]], Unit] =
    IndexedState(sColl => (sColl.map(f), ()))

  def withFixedWindows[L <: HList, Out <: HList](duration: Duration,
                                                 offset: Duration = Duration.ZERO,
                                                 options: WindowOptions = WindowOptions()): IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[L]], Unit] =
    IndexedState(sColl => (sColl.withFixedWindows(duration, offset, options), ()))

  def aggregate[L <: HList, Out <: HList](zeroValue: Row.Aux[Out])(seqOp: (Row.Aux[Out], Row.Aux[L]) => Row.Aux[Out],
                                                        combOp: (Row.Aux[Out], Row.Aux[Out]) => Row.Aux[Out]): IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[Out]], Unit] =
    IndexedState(sColl => (sColl.aggregate(zeroValue)(seqOp, combOp), ()))

  def groupByKey[L <: HList, Out <: HList, K : Manifest](f: Row.Aux[L] => K): IndexedState[SCollection[Row.Aux[L]], SCollection[(K, Iterable[Row.Aux[L]])], Unit] =
    IndexedState(sColl => (sColl.groupBy(f), ()))

  def groupByKeyRow[L <: HList, Out <: HList, K: Manifest](f: Row.Aux[L] => K)
//  : IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[L]], Unit] =
  : IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[FieldType[Witness.`'k`.T, K]  :: FieldType[Witness.`'values`.T, List[L]]  :: HNil]], Unit] =
    IndexedState(sColl => (sColl.groupBy(f).map({
      case (k: K, it: Iterable[Row.Aux[L]]) => Row(('k ->> k) ::  ('values ->> it.toList.map(_.hl)) :: HNil)
    }), ()))
  //      case (k: K, it: Iterable[Row.Aux[L]]) => Row[L](it.head.hl)
  //      case (k: K, it: Iterable[Row.Aux[L]]) => Row(('k ->> k) :: ('values ->> (it.toList.map(x => Row[L](x.hl)))) :: HNil)

  def writeToSinks[L <: HList, S <: HList, OutT <: HList, UTILS](sinks: S, utils: UTILS)
                                                                (implicit
                                                                 folder: LeftFolder[S, (SCollection[Row.Aux[L]], UTILS), writer2.type]
                                                                ): IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[L]], Unit] =
    IndexedState(sColl => ( {
      sinks.foldLeft((sColl, utils))(writer2)(folder);
      sColl
    }, ()))

}