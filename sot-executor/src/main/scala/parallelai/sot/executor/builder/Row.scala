package parallelai.sot.executor.builder

import shapeless._
import ops.hlist._
import parallelai.sot.executor.builder.DeepRec.ToCcPartiallyApplied
import record._
import shapeless.labelled.{FieldType, field}
import shapeless.ops.record.{Modifier, Selector, _}


trait MapType[A] {
  type B

  def apply(v: A): B
}

trait LowPriorityMapType {

  type Aux[A, B0] = MapType[A] {type B = B0}

  implicit def identityMappable[A]: Aux[A, A] = new MapType[A] {
    type B = A
    override def apply(v: A): B = v
  }
}

object MapType extends LowPriorityMapType {

  implicit val intToLong: Aux[Int, Long] = new MapType[Int] {
    type B = Long
    override def apply(v: Int): Long = v.toLong
  }

  implicit val intToLongOps: Aux[Option[Int], Option[Long]] = new MapType[Option[Int]] {
    type B = Option[Long]
    override def apply(v: Option[Int]): Option[Long] = {
      v match {
        case Some(value) => Some(value.toLong)
        case None => None
      }
    }
  }

}

class Row[L <: HList](val hl: L) {

  type FSL[K] = Selector[L, K]

  def updatedAt[V, W, Out <: HList](n: Nat, value: V)(implicit
                                                      replacer: ReplaceAt.Aux[L, n.N, V, (W, Out)]): Out = replacer(hl, value)._2

  def appendList[V <: HList, Out <: HList](v: V)(implicit prep: Prepend.Aux[L, V, Out]) = new Row[Out](prep(hl, v))

  def keys(implicit keys: Keys[L]): keys.Out = keys()

  def get(k: Witness)(implicit selector: Selector[L, k.T]): selector.Out = selector(hl)

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

  def to[Out](implicit gen: LabelledGeneric[Out]): ToCcPartiallyApplied[Out, gen.Repr] =
    new ToCcPartiallyApplied[Out, gen.Repr](gen)

  def apply[A <: Product, Repr <: HList](a: A)(implicit
                                               gen: LabelledGeneric.Aux[A, Repr],
                                               rdr: DeepRec[Repr]): Row[rdr.Out] =
    new Row[rdr.Out](rdr(gen.to(a)))

}