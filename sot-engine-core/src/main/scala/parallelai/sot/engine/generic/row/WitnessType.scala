package parallelai.sot.engine.generic.row

import shapeless._
import shapeless.{::, HNil, Witness}

trait NestedWitness[A <: Witness, B] {
  type OutA
  type OutB
}

trait LowPriorityNestedMappable {

  type Aux[A <: Witness, B, OutA0, OutB0] = NestedWitness[A, B] {type OutA = OutA0; type OutB = OutB0}

  implicit def nestedWitnessRecursive[A <: Witness, N1 <: Witness, N2](implicit n: NestedWitness[N1, N2]): NestedWitness.Aux[A, Nested[N1, N2], A#T, Nested[n.OutA, n.OutB]] =
    new NestedWitness[A, Nested[N1, N2]] {
      type OutA = A#T
      type OutB = Nested[n.OutA, n.OutB]
    }

}

object NestedWitness extends LowPriorityNestedMappable {

  def apply[A <: Witness, B](implicit a: NestedWitness[A, B]): NestedWitness.Aux[A, B, a.OutA, a.OutB] = a

  implicit def nestedWitnessLeaf[A <: Witness, B <: Witness]: NestedWitness.Aux[A, B, A#T, B#T] =
    new NestedWitness[A, B] {
      override type OutA = A#T
      override type OutB = B#T
    }

}

trait WitnessType[L] {
  type Out <: HList
}

object WitnessType {

  def apply[L <: HList](implicit m: WitnessType[L]): WitnessType.Aux[L, m.Out] = m

  type Aux[L <: HList, Out0 <: HList] = WitnessType[L] {type Out = Out0}

  implicit val hnilListWitness: WitnessType.Aux[HNil, HNil] = new WitnessType[HNil] {
    type Out = HNil
  }

  implicit def simpleWitness[H <: Witness, T <: HList](implicit t: WitnessType[T]): WitnessType.Aux[H :: T, H#T :: t.Out] =
    new WitnessType[H :: T] {
      override type Out = H#T :: t.Out
    }

  implicit def nestedWitnessMappable[N1 <: Witness, N2, T <: HList](implicit
                                                                    h2: NestedWitness[N1, N2],
                                                                    t: WitnessType[T]): WitnessType.Aux[Nested[N1, N2] :: T, Nested[h2.OutA, h2.OutB] :: t.Out] =
    new WitnessType[Nested[N1, N2] :: T] {
      override type Out = Nested[h2.OutA, h2.OutB] :: t.Out
    }
}