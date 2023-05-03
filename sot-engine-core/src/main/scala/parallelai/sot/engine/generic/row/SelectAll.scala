package parallelai.sot.engine.generic.row

import shapeless.{::, DepFn1, HList, HNil, Lazy, Witness}
import shapeless.labelled.{FieldType, field}
import shapeless.ops.record.Selector

case class Nested[A, B](a: A, b: B)

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

    override def apply(v: Option[Out0]): Out0 = v.get
  }

  implicit def listTypeMapper[Out0]: TypeMapper.Aux[List[Out0], Out0] = new TypeMapper[List[Out0]] {
    override type Out = Out0

    override def apply(v: List[Out0]): Out0 = v.head
  }

  implicit def identityMapper[In]: TypeMapper[In] = new TypeMapper[In] {
    override type Out = In

    override def apply(v: In): Out = v
  }

}


trait ExtendedSelector[L <: HList, K] extends DepFn1[L] with Serializable {
  type Out

  def apply(l: L): Out
}

trait SelectorWrapper {

  type Aux[L <: HList, K, Out0] = ExtendedSelector[L, K] {type Out = Out0}

  implicit def selectorWrapper[L <: HList, K, Out0](implicit selector: Selector.Aux[L, K, Out0]): ExtendedSelector.Aux[L, K, Out0] =
    new ExtendedSelector[L, K] {
      type Out = Out0

      override def apply(l: L): Out = selector(l)
    }

}

object ExtendedSelector extends SelectorWrapper {

  def apply[L <: HList, K](implicit selector: ExtendedSelector[L, K]): Aux[L, K, selector.Out] = selector

  implicit def selectorWrapperOption[L <: HList, K, Out0, OutTyped](implicit selector: Selector.Aux[L, K, Out0],
                                                                    typeMapper: TypeMapper.Aux[Out0, OutTyped]): ExtendedSelector.Aux[L, K, OutTyped] =
    new ExtendedSelector[L, K] {
      type Out = OutTyped

      override def apply(l: L): OutTyped = typeMapper(selector(l))
    }

  implicit def selectorLeaf[L <: HList, K, Out0 <: HList, V, NestedOut](implicit
                                                                        selector: ExtendedSelector.Aux[L, K, Out0],
                                                                        selectorNested: ExtendedSelector.Aux[Out0, V, NestedOut],
                                                                        typeMapperNested: TypeMapper[NestedOut]
                                                                       ): ExtendedSelector.Aux[L, Nested[K, V], FieldType[V, typeMapperNested.Out]] =
    new ExtendedSelector[L, Nested[K, V]] {
      type Out = FieldType[V, typeMapperNested.Out]

      def apply(l: L): FieldType[V, typeMapperNested.Out] = field[V](typeMapperNested(selectorNested(selector(l))))
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
   sh: Lazy[ExtendedSelector[L, KH]],
   st: SelectAll[L, KT]
  ): Aux[L, KH :: KT, sh.value.Out :: st.Out] =
    new SelectAll[L, KH :: KT] {
      type Out = sh.value.Out :: st.Out

      def apply(l: L): Out = sh.value(l) :: st(l)
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
   sh: Lazy[ExtendedSelector.Aux[L, KH, KHOut]],
   shNested: ExtendedSelector[KHOut, KN],
   st: SelectAll[L, KT]
  ): Aux[L, Nested[KH, KN] :: KT, shNested.Out :: st.Out] =
    new SelectAll[L, Nested[KH, KN] :: KT] {
      type Out = shNested.Out :: st.Out

      def apply(l: L): Out = shNested(sh.value(l)) :: st(l)
    }

}