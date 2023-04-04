package parallelai.sot.executor.builder

import shapeless._
import ops.hlist._
import record._
import shapeless.labelled.{FieldType, field}
import syntax.singleton._
import shapeless.ops.record.{Modifier, Selector, _}

trait ToRecord[L <: HList] {
  type Out <: HList

  def apply(l: L): Out
}

trait LowPriorityToRecord {

  type Aux[L <: HList, Out0] = ToRecord[L] {type Out = Out0}

  implicit def hconsToRec1[K <: Symbol, V,
  T <: HList, TOut <: HList](implicit
                             toRec: ToRecord.Aux[T, TOut]
                            ): Aux[FieldType[K, V] :: T, FieldType[K, V] :: TOut] = new ToRecord[FieldType[K, V] :: T] {
    type Out = FieldType[K, V] :: TOut

    def apply(l: FieldType[K, V] :: T): Out =
      l.head :: toRec(l.tail)
  }
}


object ToRecord extends LowPriorityToRecord {

  def apply[L <: HList](implicit toM: ToRecord[L]): Aux[L, toM.Out] = toM

  implicit val hnilToRec: ToRecord.Aux[HNil, HNil] = new ToRecord[HNil] {
    type Out = HNil

    def apply(l: HNil): HNil = HNil
  }

  implicit def hconsToRec0[K <: Symbol, V <: Product,
  H <: HList, HOut <: HList, T <: HList,
  TOut <: HList](implicit
                 gen: LabelledGeneric.Aux[V, H],
                 toRecH: ToRecord.Aux[H, HOut],
                 toRecT: ToRecord.Aux[T, TOut]
                ): Aux[FieldType[K, V] :: T, FieldType[K, HOut] :: TOut] = new ToRecord[FieldType[K, V] :: T] {

    type Out = FieldType[K, HOut] :: TOut

    def apply(l: FieldType[K, V] :: T): Out =
      field[K](toRecH.apply(gen.to(l.head))) :: toRecT(l.tail)
  }
}

class Row[L <: HList](val hl: L) {

  type FSL[K] = Selector[L, K]

  def updatedAt[V, W, Out <: HList](n: Nat, value: V)(implicit
                                                      replacer: ReplaceAt.Aux[L, n.N, V, (W, Out)]): Out = replacer(hl, value)._2

  def appendList[V <: HList, Out <: HList](v: V)(implicit prep: Prepend.Aux[L, V, Out]) = new Row[Out](prep(hl, v))

  def keys(implicit keys: Keys[L]): keys.Out = keys()

  def get(k: Witness)(implicit selector: Selector[L, k.T]): selector.Out = selector(hl)

  def to[Out](implicit gen: LabelledGeneric.Aux[Out, L]): Out = gen.from(hl)

  def append[V, Out <: HList](k: Witness, v: V)(implicit updater: Updater.Aux[L, FieldType[k.T, V], Out],
                                                lk: LacksKey[L, k.T]): Row[Out] = {
    new Row(updater(hl, field[k.T](v)))
  }

  def update[W](k: WitnessWith[FSL], value: W)
               (implicit modifier: Modifier[L, k.T, k.instance.Out, W]): Row[modifier.Out] = new Row(modifier(hl, _ => value))

  def updateWith[W](k: WitnessWith[FSL])(f: k.instance.Out => W)
                   (implicit modifier: Modifier[L, k.T, k.instance.Out, W]): Row[modifier.Out] = new Row(modifier(hl, f))

  def remove[V, Out <: HList](k: Witness)(implicit remover: Remover.Aux[L, k.T, (V, Out)]): Row[Out] = new Row(remover(hl)._2)

}

object Row {

  def apply[P <: Product, L <: HList](p: P)(implicit gen: LabelledGeneric.Aux[P, L], tmr: ToRecord[L]) = new Row[tmr.Out](tmr(gen.to(p)))

}