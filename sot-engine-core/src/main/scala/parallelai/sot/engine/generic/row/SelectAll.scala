package parallelai.sot.engine.generic.row

import shapeless.{::, DepFn1, HList, HNil, Witness}
import shapeless.labelled.{FieldType, field}
import shapeless.ops.record.Selector

case class Nested[A, B](a: A, b: B)

trait ExtendedSelector[L <: HList, K] extends DepFn1[L] with Serializable {
  type Out
  def apply(l : L): Out
}

trait SelectorWrapper {

  implicit def selectorWrapper[L<: HList, K, Out0](implicit selector: Selector.Aux[L, K, Out0]): ExtendedSelector.Aux[L, K, Out0] =
    new ExtendedSelector[L, K] {
      type Out = Out0
      override def apply(l: L): Out = selector(l)
    }

}

trait LowestPrioritySelector extends SelectorWrapper {

  type Aux[L <: HList, K, Out0] = ExtendedSelector[L, K] { type Out = Out0 }

  implicit def selectorBottom[L <: HList, K, Out0 <: HList, V](implicit
                                                               selector: ExtendedSelector.Aux[L, K, Out0],
                                                               selectorNested: ExtendedSelector[Out0, V]
                                                              ): ExtendedSelector.Aux[L, Nested[K, V], FieldType[V, selectorNested.Out]] =
    new ExtendedSelector[L, Nested[K, V]] {
      type Out = FieldType[V, selectorNested.Out]

      def apply(l: L): FieldType[V, selectorNested.Out] = field[V](selectorNested(selector(l)))
    }

}

object ExtendedSelector extends LowestPrioritySelector {

  def apply[L <: HList, K](implicit selector: ExtendedSelector[L, K]): Aux[L, K, selector.Out] = selector

  implicit def selectorRecursive[L <: HList, K, Out0 <: HList, V <: Nested[_, _]](implicit
                                                                                     selector: ExtendedSelector.Aux[L, K, Out0],
                                                                                     selectorNested: ExtendedSelector[Out0, V]
                                                                                    ): ExtendedSelector.Aux[L, Nested[K, V], selectorNested.Out] =
    new ExtendedSelector[L, Nested[K, V]] {
      type Out = selectorNested.Out

      def apply(l: L): selectorNested.Out = selectorNested(selector(l))
    }

}

trait SelectAll[L <: HList, K <: HList] extends DepFn1[L] with Serializable {
  type Out <: HList
}

trait LowestPrioritySelectAll {

  type Aux[L <: HList, K <: HList, Out0 <: HList] = SelectAll[L, K] {type Out = Out0}

  implicit def hconsSelectAll[L <: HList, KH, KT <: HList]
  (implicit
   sh: ExtendedSelector[L, KH],
   st: SelectAll[L, KT]
  ): Aux[L, KH :: KT, FieldType[KH, sh.Out] :: st.Out] =
    new SelectAll[L, KH :: KT] {
      type Out = FieldType[KH, sh.Out] :: st.Out

      def apply(l: L): Out = field[KH](sh(l)) :: st(l)
    }

}

trait LowPrioritySelectAll extends LowestPrioritySelectAll {

  implicit def hconsSelectAllFieldType[L <: HList, KH <: Nested[_, _], KT <: HList]
  (implicit
   sh: ExtendedSelector[L, KH],
   st: SelectAll[L, KT]
  ): Aux[L, KH :: KT, sh.Out :: st.Out] =
    new SelectAll[L, KH :: KT] {
      type Out = sh.Out :: st.Out

      def apply(l: L): Out = sh(l) :: st(l)
    }

}

object SelectAll extends LowPrioritySelectAll {

  def apply[L <: HList, K <: HList](implicit sa: SelectAll[L, K]): Aux[L, K, sa.Out] = sa

  implicit def hnilSelectAll[L <: HList]: Aux[L, HNil, HNil] =
    new SelectAll[L, HNil] {
      type Out = HNil

      def apply(l: L): Out = HNil
    }

  implicit def hconsSelectFieldType[L <: HList, KH <: Witness, KHOut <: HList, KN <: Witness, KT <: HList]
  (implicit
   sh: ExtendedSelector.Aux[L, KH, KHOut],
   shNested: ExtendedSelector[KHOut, KN],
   st: SelectAll[L, KT]
  ): Aux[L, Nested[KH, KN] :: KT, shNested.Out :: st.Out] =
    new SelectAll[L, Nested[KH, KN] :: KT] {
      type Out = shNested.Out :: st.Out

      def apply(l: L): Out = shNested(sh(l)) :: st(l)
    }

}