package parallelai.sot.engine.generic.row

import parallelai.sot.engine.generic.row.DeepRec.ToCcPartiallyApplied
import shapeless._
import shapeless.labelled.{FieldType, field}
import shapeless.ops.hlist._
import shapeless.ops.record.{Modifier, Selector, _}
//important import: without it fields names might become incorrect in nested rows
import shapeless.record._
import shapeless.record.Record
import syntax.singleton._

trait SelectAll[L <: HList, K <: HList] extends DepFn1[L] with Serializable {
  type Out <: HList
}

trait LowestPrioritySelector {

  def apply[L <: HList, K <: HList](implicit sa: SelectAll[L, K]): Aux[L, K, sa.Out] = sa

  type Aux[L <: HList, K <: HList, Out0 <: HList] = SelectAll[L, K] {type Out = Out0}

    implicit def hconsSelectAll[L <: HList, KH, KT <: HList]
    (implicit
     sh: Selector[L, KH],
     st: SelectAll[L, KT]
    ): Aux[L, KH :: KT, FieldType[KH, sh.Out] :: st.Out] =
      new SelectAll[L, KH :: KT] {
        type Out = FieldType[KH, sh.Out] :: st.Out
        def apply(l: L): Out = field[KH](sh(l)) :: st(l)
      }

}

trait LowPioritySelector extends LowestPrioritySelector {

  implicit def hconsSelectAllFieldType[L <: HList, KH <: FieldType[_, _], KT <: HList]
  (implicit
   sh: Selector[L, KH],
   st: SelectAll[L, KT]
  ): Aux[L, KH :: KT, sh.Out :: st.Out] =
    new SelectAll[L, KH :: KT] {
      type Out = sh.Out :: st.Out

      def apply(l: L): Out = sh(l) :: st(l)
    }

  implicit def selectorBottom[L <: HList, K, Out0 <: HList, V](implicit
                                                               selector: Selector.Aux[L, K, Out0],
                                                               selectorNested: Selector[Out0, V]
                                                              ): Selector.Aux[L, FieldType[K, V], FieldType[V, selectorNested.Out]] =
    new Selector[L, FieldType[K, V]] {
      type Out = FieldType[V, selectorNested.Out]

      def apply(l: L): FieldType[V, selectorNested.Out] = field[V](selectorNested(selector(l)))
    }

}

object SelectAll extends LowPioritySelector {

  implicit def hnilSelectAll[L <: HList]: Aux[L, HNil, HNil] =
    new SelectAll[L, HNil] {
      type Out = HNil

      def apply(l: L): Out = HNil
    }

  implicit def selectorRecursive[L <: HList, K, Out0 <: HList, V <: FieldType[_, _]](implicit
                                                                                     selector: Selector.Aux[L, K, Out0],
                                                                                     selectorNested: Selector[Out0, V]
                                                                                    ): Selector.Aux[L, FieldType[K, V], selectorNested.Out] =
    new Selector[L, FieldType[K, V]] {
      type Out = selectorNested.Out

      def apply(l: L): selectorNested.Out = selectorNested(selector(l))
    }

  implicit def hconsSelectFieldType[L <: HList, KH <: Witness, KHOut <: HList, KN <: Witness, KT <: HList]
  (implicit
   sh: Selector.Aux[L, KH, KHOut],
   shNested: Selector[KHOut, KN],
   st: SelectAll[L, KT]
  ): Aux[L, FieldType[KH, KN] :: KT, shNested.Out :: st.Out] =
    new SelectAll[L, FieldType[KH, KN] :: KT] {
      type Out = shNested.Out :: st.Out

      def apply(l: L): Out = shNested(sh(l)) :: st(l)
    }

}

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

  case class SuperSuperNested(iii: Int)

  case class SuperNestedRecord(ii: Int, spp: SuperSuperNested)

  case class NestedRecord(sp: SuperNestedRecord, i: Int)

  case class NestedCaseClass(nested: NestedRecord, a: Int, b: String, c: Double)

}

object Test2 extends App {

  import shapeless.record._

  import CC._

  val ncc = NestedCaseClass(a = 123, b = "bbb", c = 1.23, nested = NestedRecord(sp = SuperNestedRecord(ii = 2233, spp = SuperSuperNested(iii = 32423)), i = 3333))

  val row = Row(ncc)

  import SelectAll._

  type v1 = FieldType[Witness.`'nested`.T, FieldType[Witness.`'sp`.T, FieldType[Witness.`'spp`.T, Witness.`'iii`.T]]] :: Witness.`'a`.T :: HNil
//  type v1 = FieldType[Witness.`'nested`.T, FieldType[Witness.`'sp`.T, FieldType[Witness.`'spp`.T, Witness.`'iii`.T]]] :: Witness.`'a`.T :: HNil

  val rowProjected = row.project[v1]

  println(rowProjected.get('a))

}