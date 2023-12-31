package parallelai.sot.engine.generic.row

import shapeless.{::, DepFn1, HList, HNil, Lazy, Witness}
import shapeless.labelled.{FieldType, field}
import shapeless.ops.record.Selector

case class Nested[A, B](a: A, b: B)

case class Rename[A, B](a: A, b: B)

/**
  * Resolve types such as list or option when data is projected
  */
trait TypeMapper[In] {
  type Out

  def apply(v: In): Out
}

object TypeMapper {

  type Aux[In, Out0] = TypeMapper[In] {type Out = Out0}

  def apply[In](implicit a: TypeMapper[In]): TypeMapper.Aux[In, a.Out] = a

  implicit def optionTypeMapper[Out0]: TypeMapper.Aux[Option[Out0], Out0] = new TypeMapper[Option[Out0]] {
    override type Out = Out0

    override def apply(v: Option[Out0]): Out = v.get
  }

  implicit def listTypeMapper[Out0]: TypeMapper.Aux[List[Out0], Out0] = new TypeMapper[List[Out0]] {
    override type Out = Out0

    override def apply(v: List[Out0]): Out = v.head
  }

  implicit def identityMapper[In, Out0](implicit ev: In =:= Out0): TypeMapper.Aux[In, Out0] = new TypeMapper[In] {
    override type Out = Out0

    override def apply(v: In): Out = v
  }

}


trait ExtendedSelector[L <: HList, K] extends DepFn1[L] with Serializable {
  type Out

  def apply(l: L): Out
}

trait SelectorWrapper {

  type Aux[L <: HList, K, Out0] = ExtendedSelector[L, K] {type Out = Out0}

  implicit def selectorWrapper[L <: HList, K](implicit selector: Selector[L, K]): ExtendedSelector.Aux[L, K, selector.Out] =
    new ExtendedSelector[L, K] {
      type Out = selector.Out

      override def apply(l: L): Out = selector(l)
    }

  implicit def selectorLeaf[L <: HList, K, Out0 <: HList, V, NestedOut](implicit
                                                                        selector: ExtendedSelector.Aux[L, K, Out0],
                                                                        selectorNested: ExtendedSelector.Aux[Out0, V, NestedOut],
                                                                        typeMapperNested: TypeMapper[NestedOut]
                                                                       ): ExtendedSelector.Aux[L, Nested[K, V], FieldType[V, typeMapperNested.Out]] =
    new ExtendedSelector[L, Nested[K, V]] {
      override type Out = FieldType[V, typeMapperNested.Out]

      override def apply(l: L): Out = {
        val f = field[V](typeMapperNested(selectorNested(selector(l))))
        f
      }
    }

  implicit def selectorLeafRename[L <: HList, K, Out0 <: HList, V, VV](implicit
                                                                       selector: ExtendedSelector.Aux[L, K, Out0],
                                                                       selectorNested: ExtendedSelector[Out0, V]
                                                                      ): ExtendedSelector.Aux[L, Nested[K, Rename[V, VV]], FieldType[VV, selectorNested.Out]] =
    new ExtendedSelector[L, Nested[K, Rename[V, VV]]] {
      override type Out = FieldType[VV, selectorNested.Out]

      override def apply(l: L): Out = {
        val f = field[VV](selectorNested(selector(l)))
        f
      }
    }

}

object ExtendedSelector extends SelectorWrapper {

  def apply[L <: HList, K](implicit selector: ExtendedSelector[L, K]): Aux[L, K, selector.Out] = selector

  implicit def selectorWrapperOption[L <: HList, K, Out0](implicit selector: Selector.Aux[L, K, Out0],
                                                          typeMapper: TypeMapper[Out0]): ExtendedSelector.Aux[L, K, typeMapper.Out] =
    new ExtendedSelector[L, K] {
      type Out = typeMapper.Out

      override def apply(l: L): Out = typeMapper(selector(l))
    }

  implicit def selectorNested[L <: HList, K, Out0 <: HList, N1, N2](implicit
                                                                    selector: ExtendedSelector.Aux[L, K, Out0],
                                                                    selectorNested: ExtendedSelector[Out0, Nested[N1, N2]]
                                                                   ): ExtendedSelector.Aux[L, Nested[K, Nested[N1, N2]], selectorNested.Out] =
    new ExtendedSelector[L, Nested[K, Nested[N1, N2]]] {
      type Out = selectorNested.Out

      def apply(l: L): Out = selectorNested(selector(l))
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

  implicit def hconsSelectAllRename[L <: HList, KH, KHH, KT <: HList]
  (implicit
   sh: ExtendedSelector[L, KH],
   st: SelectAll[L, KT]
  ): Aux[L, Rename[KH, KHH] :: KT, FieldType[KHH, sh.Out] :: st.Out] =
    new SelectAll[L, Rename[KH, KHH] :: KT] {
      type Out = FieldType[KHH, sh.Out] :: st.Out

      def apply(l: L): Out = field[KHH](sh(l)) :: st(l)
    }

}


object SelectAll extends LowestPrioritySelectAll {

  def apply[L <: HList, K <: HList](implicit sa: SelectAll[L, K]): Aux[L, K, sa.Out] = sa

  implicit def hnilSelectAll[L <: HList]: Aux[L, HNil, HNil] =
    new SelectAll[L, HNil] {
      type Out = HNil

      def apply(l: L): Out = HNil
    }

  implicit def hconsSelectAllNestedType[L <: HList, K, V, KT <: HList]
  (implicit
   sh: ExtendedSelector[L, Nested[K, V]],
   st: SelectAll[L, KT]
  ): Aux[L, Nested[K, V] :: KT, sh.Out :: st.Out] =
    new SelectAll[L, Nested[K, V] :: KT] {
      type Out = sh.Out :: st.Out

      def apply(l: L): Out = sh(l) :: st(l)
    }

}