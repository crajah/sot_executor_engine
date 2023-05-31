package parallelai.sot.engine.generic.row

import parallelai.sot.engine.generic.row._
import parallelai.sot.engine.generic.row.DeepRec.ToCcPartiallyApplied
import shapeless._
import shapeless.labelled.{FieldType, field}
import shapeless.ops.hlist._
import shapeless.ops.record.{Modifier, Selector, _}
//important import: without it fields names might become incorrect in nested rows
import shapeless.record._
import shapeless.record.Record
import syntax.singleton._

trait Row extends Serializable {

  type L <: HList

  val hList: L

  type FSL[K] = Selector[L, K]

  def updatedAt[V, W, Out <: HList](n: Nat, value: V)
                                   (implicit replacer: ReplaceAt.Aux[L, n.N, V, (W, Out)]): Out =
    replacer(hList, value)._2

  def appendList[V <: HList, Out <: HList](v: V)(implicit prep: Prepend.Aux[L, V, Out]) =
    Row[Out](prep(hList, v))

  def keys(implicit keys: Keys[L]): keys.Out =
    keys()

  def get(k: Witness)(implicit selector: Selector[L, k.T]): selector.Out =
    selector(hList)

  def project[T <: HList, W <: HList, Out <: HList](v: T)(implicit m: WitnessType.Aux[T, W], selector: SelectAll.Aux[L, W, Out], isKeyDuplicated: IsKeyDuplicated[Out]): Row.Aux[Out] =
    Row[selector.Out](isKeyDuplicated(selector(hList)))

  def projectTyped[K <: HList](implicit selector: SelectAll[L, K]): Row.Aux[selector.Out] =
    Row[selector.Out](selector(hList))

  def append[V, Out <: HList](k: Witness, v: V)
                             (implicit updater: Updater.Aux[L, FieldType[k.T, V], Out], lk: LacksKey[L, k.T]): Row.Aux[Out] =
    Row(updater(hList, field[k.T](v)))

  def concat[B <: HList](b: Row.Aux[B])
                        (implicit p: Prepend[L, B]): Row.Aux[p.Out] =
    Row(p(hList, b.hList))

  def update[W](k: WitnessWith[FSL], value: W)
               (implicit modifier: Modifier[L, k.T, k.instance.Out, W]): Row.Aux[modifier.Out] =
    Row(modifier(hList, _ => value))

  def updateWith[W](k: WitnessWith[FSL])(f: k.instance.Out => W)
                   (implicit modifier: Modifier[L, k.T, k.instance.Out, W]): Row.Aux[modifier.Out] =
    Row(modifier(hList, f))

  def remove[V, Out <: HList](k: Witness)
                             (implicit remover: Remover.Aux[L, k.T, (V, Out)]): Row.Aux[Out] =
    Row(remover(hList)._2)
}

object Row {
  type Aux[L0 <: HList] = Row {type L = L0}

  def to[Out](implicit gen: LabelledGeneric[Out]): ToCcPartiallyApplied[Out, gen.Repr] =
    new ToCcPartiallyApplied[Out, gen.Repr](gen)

  def apply[A <: Product, Repr <: HList](a: A)
                                        (implicit gen: LabelledGeneric.Aux[A, Repr], rdr: DeepRec[Repr]): Row.Aux[rdr.Out] =
    Row[rdr.Out](rdr(gen.to(a)))


  def apply[Repr <: HList](gen: Repr): Row.Aux[Repr] =
    new Row {
      type L = Repr
      val hList = gen
    }
}