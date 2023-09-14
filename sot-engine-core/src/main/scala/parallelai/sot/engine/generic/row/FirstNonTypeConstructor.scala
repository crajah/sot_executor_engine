package parallelai.sot.engine.generic.row

import shapeless.{HList, LabelledGeneric}

trait FirstNonTypeConstructor[T] extends Serializable {
  type Out

  def to(t: T): Out

  def from(r: Out): T
}

trait FirstLowPriority {

  implicit def baseProduct[K <: Symbol, T, R <: HList, V, H, TAIL <: HList, HR, TR <: HList](implicit
                                                                                             gen: LabelledGeneric.Aux[T, R],
                                                                                             deepRec: DeepRec[R]
                                                                                            ): FirstNonTypeConstructor.Aux[T, deepRec.Out] =
    new FirstNonTypeConstructor[T] {
      type Out = deepRec.Out

      def to(t: T): Out = deepRec(gen.to(t))

      def from(r: Out): T = gen.from(deepRec.fromRec(r))
    }
}

object FirstNonTypeConstructor extends FirstLowPriority {

  type Aux[T, Out0] = FirstNonTypeConstructor[T] {type Out = Out0}

  def apply[T]()(implicit firstNonConstructor: FirstNonTypeConstructor[T]): FirstNonTypeConstructor.Aux[T, firstNonConstructor.Out] = firstNonConstructor

  implicit def option[T, Out0, Out1](implicit gen: FirstNonTypeConstructor.Aux[T, Out0]): FirstNonTypeConstructor.Aux[Option[T], Option[Out0]] =
    new FirstNonTypeConstructor[Option[T]] {
      type Out = Option[Out0]

      def to(t: Option[T]): Out = t.map(x => gen.to(x))

      def from(r: Out): Option[T] = r.map(x => gen.from(x))
    }

  implicit def list[T, Out0, Out1](implicit gen: FirstNonTypeConstructor.Aux[T, Out0]): FirstNonTypeConstructor.Aux[List[T], List[Out0]] =
    new FirstNonTypeConstructor[List[T]] {
      type Out = List[Out0]

      def to(t: List[T]): Out = t.map(x => gen.to(x))

      def from(r: Out): List[T] = r.map(x => gen.from(x))
    }
}


