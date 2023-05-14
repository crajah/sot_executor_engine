package parallelai.sot.engine.runner

import com.spotify.scio.ScioContext
import com.spotify.scio.values.SCollection
import org.tensorflow.Tensor
import parallelai.sot.engine.generic.row.Row
import parallelai.sot.engine.io.TapDef
import parallelai.sot.executor.model.SOTMacroConfig.TapDefinition
import shapeless.{HList, Witness}
import shapeless.labelled.{FieldType, field}
import shapeless.ops.hlist.LeftFolder
import shapeless.ops.record.{LacksKey, Remover, Updater}

import scalaz.IndexedState

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

  def predict[L <: HList, Out <: HList](modelBucket: String, modelPath: String, fetchOps: Seq[String],
                                        inFn: Row.Aux[L] => Map[String, Tensor],
                                        outFn: (Row.Aux[L], Map[String, Tensor]) => Row.Aux[Out]): IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[Out]], Unit] = {
    import com.spotify.scio.sot.tensorflow._
    IndexedState(sColl => (sColl.predict(modelBucket, modelPath, fetchOps){inFn}{outFn}, ()))
  }

  def writeToSinks[L <: HList, S <: HList, OutT <: HList, UTILS](sinks: S, utils: UTILS)
                                                                (implicit
                                                                 folder: LeftFolder[S, (SCollection[Row.Aux[L]], UTILS), writer2.type]
                                                                ): IndexedState[SCollection[Row.Aux[L]], SCollection[Row.Aux[L]], Unit] =
    IndexedState(sColl => ( {
      sinks.foldLeft((sColl, utils))(writer2)(folder)
      sColl
    }, ()))

}