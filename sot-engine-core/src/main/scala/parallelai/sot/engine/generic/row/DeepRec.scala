package parallelai.sot.engine.generic.row

import shapeless.labelled.{FieldType, field}
import shapeless.{DepFn1, LabelledGeneric, _}

trait DeepRec[L] extends DepFn1[L] with Serializable {
  type Out <: HList

  def fromRec(out: Out): L
}

trait LowPriorityDeepRec {
  type Aux[L, Out0] = DeepRec[L] { type Out = Out0 }

//  implicit def hconsDeepRec0[H, T <: HList](implicit tdr: Lazy[DeepRec[T]]): DeepRec.Aux[H :: T, H :: tdr.value.Out] =
//    new DeepRec[H :: T] {
//      type Out = H :: tdr.value.Out
//
//      def apply(in: H :: T): H :: tdr.value.Out =
//        in.head :: tdr.value(in.tail)
//
//      def fromRec(out: H :: tdr.value.Out): H :: T =
//        out.head :: tdr.value.fromRec(out.tail)
//    }
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

  //  implicit def hconsDeepRec2[K <: Symbol, V, Repr <: HList, T <: HList](implicit gen: LabelledGeneric.Aux[V, Repr],
  //                                                                        hdr: Lazy[DeepRec[Repr]],
  //                                                                        tdr: Lazy[DeepRec[T]]
  //                                                                       ): DeepRec.Aux[FieldType[K, Option[V]] :: T, FieldType[K, Option[hdr.value.Out]] :: tdr.value.Out] =
  //    new DeepRec[FieldType[K, Option[V]] :: T] {
  //      type Out = FieldType[K, Option[hdr.value.Out]] :: tdr.value.Out
  //
  //      def apply(in: FieldType[K, Option[V]] :: T): FieldType[K, Option[hdr.value.Out]] :: tdr.value.Out = {
  //        val value: Option[V] = in.head
  //
  //        value match {
  //          case Some(v) => field[K](Some(hdr.value.apply(gen.to(v)))) :: tdr.value.apply(in.tail)
  //          case None => field[K](None) :: tdr.value.apply(in.tail)
  //        }
  //      }
  //
  //      def fromRec(out: FieldType[K, Option[hdr.value.Out]] :: tdr.value.Out): FieldType[K, Option[V]] :: T = {
  //        val value: Option[hdr.value.Out] = out.head
  //
  //        value match {
  //          case Some(v) => field[K](Some(gen.from(hdr.value.fromRec(v)))) :: tdr.value.fromRec (out.tail)
  //          case None => field[K](None) :: tdr.value.fromRec (out.tail)
  //        }
  //      }
  //    }
  //
  //  implicit def hconsDeepRec3[K <: Symbol, V, Repr <: HList, T <: HList](implicit gen: LabelledGeneric.Aux[V, Repr],
  //                                                                        hdr: Lazy[DeepRec[Repr]],
  //                                                                        tdr: Lazy[DeepRec[T]]
  //                                                                       ): DeepRec.Aux[FieldType[K, List[V]] :: T, FieldType[K, List[hdr.value.Out]] :: tdr.value.Out] =
  //    new DeepRec[FieldType[K, List[V]] :: T] {
  //      type Out = FieldType[K, List[hdr.value.Out]] :: tdr.value.Out
  //
  //      def apply(in: FieldType[K, List[V]] :: T): FieldType[K, List[hdr.value.Out]] :: tdr.value.Out =
  //        field[K](in.head.map(v => hdr.value(gen.to(v)))) :: tdr.value(in.tail)
  //
  //      def fromRec(out: FieldType[K, List[hdr.value.Out]] :: tdr.value.Out): FieldType[K, List[V]] :: T =
  //        field[K](out.head.map(v => gen.from(hdr.value.fromRec(v)))) :: tdr.value.fromRec(out.tail)
  //    }
}