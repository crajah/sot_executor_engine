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

class Row[L <: HList](val hl: L) {

  type FSL[K] = Selector[L, K]

  def updatedAt[V, W, Out <: HList](n: Nat, value: V)(implicit
                                                      replacer: ReplaceAt.Aux[L, n.N, V, (W, Out)]): Out = replacer(hl, value)._2

  def appendList[V <: HList, Out <: HList](v: V)(implicit prep: Prepend.Aux[L, V, Out]) = new Row[Out](prep(hl, v))

  def keys(implicit keys: Keys[L]): keys.Out = keys()

  def get(k: Witness)(implicit selector: Selector[L, k.T]): selector.Out = selector(hl)

  def project[K <: HList](implicit selector: SelectAll[L, K]): Row[selector.Out] = {
    new Row[selector.Out](selector(hl))
  }

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

object CC {

  case class Level3Record(iii: Int)

  case class Level2Record(ii: Int, l3: Level3Record)

  case class Level1Record(l2: Level2Record, i: Int)

  case class NestedCaseClass(l1: Level1Record, a: Int, b: String, c: Double)

}

object Test2 extends App {

  import shapeless.record._

  import CC._

  val ncc = NestedCaseClass(a = 123, b = "bbb", c = 1.23, l1 = Level1Record(l2 = Level2Record(ii = 2233, l3 = Level3Record(iii = 32423)), i = 3333))

  val row = Row(ncc)

  type v1 = FieldType[Witness.`'l1`.T, FieldType[Witness.`'l2`.T, FieldType[Witness.`'l3`.T, Witness.`'iii`.T]]] :: Witness.`'a`.T :: HNil
//  type v1 = FieldType[Witness.`'nested`.T, FieldType[Witness.`'sp`.T, FieldType[Witness.`'spp`.T, Witness.`'iii`.T]]] :: Witness.`'a`.T :: HNil

  import SelectAll._

  val rowProjected = row.project[v1]

  println(rowProjected.get('a))

}