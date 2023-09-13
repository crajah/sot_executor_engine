package parallelai.sot.engine.generic.row

import shapeless.labelled.{FieldType, field}
import shapeless.{DepFn1, LabelledGeneric, _}

trait DeepRec[L] extends DepFn1[L] with Serializable {
  type Out <: HList

  def fromRec(out: Out): L
}

trait LowPriorityDeepRec {
  type Aux[L, Out0] = DeepRec[L] { type Out = Out0 }

  implicit def hconsDeepRec0[H, T <: HList](implicit tdr: Lazy[DeepRec[T]]): DeepRec.Aux[H :: T, H :: tdr.value.Out] =
    new DeepRec[H :: T] {
      type Out = H :: tdr.value.Out

      def apply(in: H :: T): H :: tdr.value.Out =
        in.head :: tdr.value(in.tail)

      def fromRec(out: H :: tdr.value.Out): H :: T =
        out.head :: tdr.value.fromRec(out.tail)
    }

}

object DeepRec extends LowPriorityDeepRec {
  class ToCcPartiallyApplied[A, Repr](val gen: LabelledGeneric.Aux[A, Repr]) extends Serializable {
    def from[Out0, Out1](out: Out0)(implicit rdr: DeepRec.Aux[Repr, Out1],
                                    eqv: Out0 =:= Out1): A =
      gen.from(rdr.fromRec(eqv(out)))
  }

  implicit val hnilDeepRec: DeepRec.Aux[HNil, HNil] = new DeepRec[HNil] {
    type Out = HNil

    def apply(in: HNil): HNil = in

    def fromRec(out: HNil): HNil = out
  }

  implicit def hconsDeepRec1[K <: Symbol, V, Repr <: HList, T <: HList](implicit
                                                                        hdr: Lazy[FirstNonTypeConstructor[V]],
                                                                        tdr: Lazy[DeepRec[T]]
                                                                       ): DeepRec.Aux[FieldType[K, V] :: T, FieldType[K, hdr.value.Out] :: tdr.value.Out] =
    new DeepRec[FieldType[K, V] :: T] {
      type Out = FieldType[K, hdr.value.Out] :: tdr.value.Out

      def apply(in: FieldType[K, V] :: T): FieldType[K, hdr.value.Out] :: tdr.value.Out =
        field[K](hdr.value.to(in.head)) :: tdr.value(in.tail)

      def fromRec(out: FieldType[K, hdr.value.Out] :: tdr.value.Out): FieldType[K, V] :: T =
        field[K](hdr.value.from(out.head)) :: tdr.value.fromRec(out.tail)
    }

}