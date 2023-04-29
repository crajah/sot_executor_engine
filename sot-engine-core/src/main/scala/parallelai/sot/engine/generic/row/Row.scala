package parallelai.sot.engine.generic.row

import parallelai.sot.engine.generic.row.DeepRec.ToCcPartiallyApplied
import shapeless._
import shapeless.labelled.{FieldType, field}
import shapeless.ops.hlist._
import shapeless.ops.record.{Modifier, Selector, _}
import shapeless.record.Record
import syntax.singleton._

trait SelectAll[L <: HList, K <: HList] extends DepFn1[L] with Serializable { type Out <: HList }

trait SelectorStuff {

  def apply[L <: HList, K <: HList](implicit sa: SelectAll[L, K]): Aux[L, K, sa.Out] = sa

  type Aux[L <: HList, K <: HList, Out0 <: HList] = SelectAll[L, K] { type Out = Out0 }

  implicit def selectors[L <: HList, K, Out0 <: HList, V](implicit
                                                          selector: Selector.Aux[L, K, Out0],
                                                          selectorNested: Selector[Out0, V]
                                                         ) : Selector.Aux[L, FieldType[K, V], selectorNested.Out] =
    new Selector[L, FieldType[K, V]] {
      type Out = selectorNested.Out
      def apply(l: L): selectorNested.Out = selectorNested(selector(l))
    }

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

object SelectAll extends SelectorStuff {

  implicit def hnilSelectAll[L <: HList]: Aux[L, HNil, HNil] =
    new SelectAll[L, HNil] {
      type Out = HNil
      def apply(l: L): Out = HNil
    }


  implicit def hconsSelectFieldType[L <: HList, KH, KHOut <: HList, KN, KT <: HList]
  (implicit
   sh: Selector.Aux[L, KH, KHOut],
   shNested: Lazy[Selector[KHOut, KN]],
   st: SelectAll[L, KT]
  ): Aux[L, FieldType[KH, KN] :: KT, FieldType[KN, shNested.value.Out] :: st.Out] =
    new SelectAll[L, FieldType[KH, KN] :: KT] {
      type Out = FieldType[KN, shNested.value.Out] :: st.Out
      def apply(l: L): Out = field[KN](shNested.value(sh(l))) :: st(l)
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

object Test2 extends App {

  case class SuperNestedRecord(ii : Int)

  case class NestedRecord(i: Int, sp: SuperNestedRecord)

  case class NestedCaseClass(a: Int, b: String, c: Double, n: NestedRecord)

  val ncc = NestedCaseClass(1, "bbb", 1.0, NestedRecord(1, SuperNestedRecord(12)))

  val row = Row(ncc)

  type v1 =  FieldType[Witness.`'n`.T, FieldType[Witness.`'sp`.T, Witness.`'ii`.T]] :: Witness.`'b`.T :: HNil

  import SelectAll._

  val rowProjected = row.project[v1]

  println(rowProjected.get('ii))

}