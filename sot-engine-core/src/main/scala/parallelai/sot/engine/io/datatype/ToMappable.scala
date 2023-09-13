package parallelai.sot.engine.io.datatype

import parallelai.sot.engine.io.utils.FieldNaming
import shapeless._
import shapeless.labelled.FieldType

import scala.language.higherKinds

trait ToMappable[TAP, L <: HList, M] extends Serializable {
  def apply(l: L): M
}

trait LowPriorityToMappable1 {
  implicit def hconsToMappable1[TAP, K <: Symbol, V, T <: HList, M]
  (implicit wit: Witness.Aux[K], mt: MappableType[M, V], toT: Lazy[ToMappable[TAP, T, M]], fieldNaming: FieldNaming[TAP])
  : ToMappable[TAP, FieldType[K, V] :: T, M] = new ToMappable[TAP, FieldType[K, V] :: T, M] {
    override def apply(l: FieldType[K, V] :: T): M =
      mt.put(fieldNaming(wit.value.name), l.head, toT.value(l.tail))
  }
}

trait LowPriorityToMappableOption1 extends LowPriorityToMappable1 {
  implicit def hconsToMappableOption1[TAP, K <: Symbol, V, T <: HList, M]
  (implicit wit: Witness.Aux[K], mt: MappableType[M, V], toT: Lazy[ToMappable[TAP, T, M]], fieldNaming: FieldNaming[TAP])
  : ToMappable[TAP, FieldType [K, Option[V]] :: T, M] = new ToMappable[TAP, FieldType [K, Option[V]] :: T, M] {
    override def apply(l: FieldType[K, Option[V]] :: T): M =
      mt.put(fieldNaming(wit.value.name), l.head, toT.value(l.tail))
  }
}

trait LowPriorityToMappableOptionList1 extends LowPriorityToMappable1 {
  implicit def hconsToMappableOptionList1[TAP, K <: Symbol, V, T <: HList, M, S[_]]
  (implicit wit: Witness.Aux[K], mt: MappableType[M, V], toT: Lazy[ToMappable[TAP, T, M]], toSeq: S[V] => Seq[V], fieldNaming: FieldNaming[TAP])
  : ToMappable[TAP, FieldType [K, Option[S[V]]] :: T, M] = new ToMappable[TAP, FieldType [K, Option[S[V]]] :: T, M] {
    override def apply(l: FieldType[K, Option[S[V]]] :: T): M =
      mt.put(fieldNaming(wit.value.name), l.head.toList.flatMap(toSeq), toT.value(l.tail))
  }
}

trait LowPriorityToMappableSeq1 extends LowPriorityToMappableOption1 {
  implicit def hconsToMappableSeq1[TAP, K <: Symbol, V, T <: HList, M, S[_]]
  (implicit wit: Witness.Aux[K], mt: MappableType[M, V], toT: Lazy[ToMappable[TAP, T, M]],
   toSeq: S[V] => Seq[V], fieldNaming: FieldNaming[TAP])
  : ToMappable[TAP, FieldType [K, S[V]] :: T, M] = new ToMappable[TAP, FieldType [K, S[V]] :: T, M] {
    override def apply(l: FieldType[K, S[V]] :: T): M =
      mt.put(fieldNaming(wit.value.name), toSeq(l.head), toT.value(l.tail))
  }
}

trait LowPriorityToMappable0 extends LowPriorityToMappableSeq1 {
  implicit def hconsToMappable0[TAP, K <: Symbol, V, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], gen: LabelledGeneric.Aux[V, H], mbt: BaseMappableType[M],
   toH: Lazy[ToMappable[TAP, H, M]], toT: Lazy[ToMappable[TAP, T, M]], fieldNaming: FieldNaming[TAP])
  : ToMappable[TAP, FieldType [K, V] :: T, M] = new ToMappable[TAP, FieldType [K, V] :: T, M] {
    override def apply(l: FieldType[K, V] :: T): M =
      mbt.put(fieldNaming(wit.value.name), toH.value(gen.to(l.head)), toT.value(l.tail))
  }
}

trait LowPriorityToMappableOption0 extends LowPriorityToMappable0 {
  implicit def hconsToMappableOption0[TAP, K <: Symbol, V, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], gen: LabelledGeneric.Aux[V, H], mbt: BaseMappableType[M],
   toH: Lazy[ToMappable[TAP, H, M]], toT: Lazy[ToMappable[TAP, T, M]], fieldNaming: FieldNaming[TAP])
  : ToMappable[TAP, FieldType [K, Option[V]] :: T, M] = new ToMappable[TAP, FieldType [K, Option[V]] :: T, M] {
    override def apply(l: FieldType[K, Option[V]] :: T): M =
      mbt.put(fieldNaming(wit.value.name), l.head.map(h => toH.value(gen.to(h))), toT.value(l.tail))
  }
}

trait LowPriorityToMappableOptionList0 extends LowPriorityToMappable0 {
  implicit def hconsToMappableOptionList0[TAP, K <: Symbol, V, H <: HList, T <: HList, M, S[_]]
  (implicit wit: Witness.Aux[K], gen: LabelledGeneric.Aux[V, H], mbt: BaseMappableType[M],
   toH: Lazy[ToMappable[TAP, H, M]], toT: Lazy[ToMappable[TAP, T, M]], toSeq: S[V] => Seq[V], fieldNaming: FieldNaming[TAP])
  : ToMappable[TAP, FieldType [K, Option[S[V]]] :: T, M] = new ToMappable[TAP, FieldType [K, Option[S[V]]] :: T, M] {
    override def apply(l: FieldType[K, Option[S[V]]] :: T): M =
      mbt.put(fieldNaming(wit.value.name), l.head.toList.flatMap(toSeq).map(h => toH.value(gen.to(h))), toT.value(l.tail))
  }
}

trait LowPriorityToMappableSeq0 extends LowPriorityToMappableOption0 {
  implicit def hconsToMappableSeq0[TAP, K <: Symbol, V, H <: HList, T <: HList, M, S[_]]
  (implicit wit: Witness.Aux[K], gen: LabelledGeneric.Aux[V, H], mbt: BaseMappableType[M],
   toH: Lazy[ToMappable[TAP, H, M]], toT: Lazy[ToMappable[TAP, T, M]],
   toSeq: S[V] => Seq[V], fieldNaming: FieldNaming[TAP])
  : ToMappable[TAP, FieldType [K, S[V]] :: T, M] = new ToMappable[TAP, FieldType [K, S[V]] :: T, M] {
    override def apply(l: FieldType[K, S[V]] :: T): M =
      mbt.put(fieldNaming(wit.value.name), toSeq(l.head).map(h => toH.value(gen.to(h))), toT.value(l.tail))
  }
}

//Implicits for nested HList
trait LowPriorityNestedMappable extends LowPriorityToMappableSeq0 {

  implicit def nestedHListToMappable[TAP, K <: Symbol, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], mbt: BaseMappableType[M],
   toH: Lazy[ToMappable[TAP, H, M]], toT: Lazy[ToMappable[TAP, T, M]], fieldNaming: FieldNaming[TAP]): ToMappable[TAP, FieldType [K, H] :: T, M] =
    new ToMappable[TAP, FieldType [K, H] :: T, M] {
      override def apply(l: FieldType[K, H] :: T): M =
        mbt.put(fieldNaming(wit.value.name), toH.value(l.head), toT.value(l.tail))
    }

  implicit def nestedHListOptionToMappable[TAP, K <: Symbol, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], mbt: BaseMappableType[M],
   toH: Lazy[ToMappable[TAP, H, M]], toT: Lazy[ToMappable[TAP, T, M]], fieldNaming: FieldNaming[TAP]): ToMappable[TAP, FieldType [K, Option[H]] :: T, M] =
    new ToMappable[TAP, FieldType [K, Option[H]] :: T, M] {
      override def apply(l: FieldType[K, Option[H]] :: T): M =
        mbt.put(fieldNaming(wit.value.name), l.head.map(h => toH.value.apply(h)), toT.value(l.tail))
    }

  implicit def nestedHListOptionListToMappable[TAP, K <: Symbol, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], mbt: BaseMappableType[M],
   toH: Lazy[ToMappable[TAP, H, M]], toT: Lazy[ToMappable[TAP, T, M]], fieldNaming: FieldNaming[TAP]): ToMappable[TAP, FieldType [K, Option[List[H]]] :: T, M] =
    new ToMappable[TAP, FieldType [K, Option[List[H]]] :: T, M] {
      override def apply(l: FieldType[K, Option[List[H]]] :: T): M =
        mbt.put(fieldNaming(wit.value.name), l.head.map(h => h.map(toH.value.apply)).toList.flatten, toT.value(l.tail))
    }

  implicit def nestedHListListToMappable[TAP, K <: Symbol, H <: HList, T <: HList, M]
  (implicit wit: Witness.Aux[K], mbt: BaseMappableType[M],
   toH: Lazy[ToMappable[TAP, H, M]], toT: Lazy[ToMappable[TAP, T, M]], fieldNaming: FieldNaming[TAP]): ToMappable[TAP, FieldType [K, List[H]] :: T, M] =
    new ToMappable[TAP, FieldType [K, List[H]] :: T, M] {
      override def apply(l: FieldType[K, List[H]] :: T): M =
        mbt.put(fieldNaming(wit.value.name), l.head.map(h => toH.value.apply(h)), toT.value(l.tail))
    }

}

object ToMappable extends LowPriorityNestedMappable {
  implicit def hnilToMappable[TAP, M](implicit mbt: BaseMappableType[M])
  : ToMappable[TAP, HNil, M] = new ToMappable[TAP, HNil, M] {
    override def apply(l: HNil): M = mbt.base
  }
}
