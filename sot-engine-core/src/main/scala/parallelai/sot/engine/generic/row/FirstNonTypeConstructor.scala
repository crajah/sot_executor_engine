package parallelai.sot.engine.generic.row

import shapeless.{HList, LabelledGeneric}

trait FirstNonTypeConstructor[T] extends Serializable {
  type Out

  def to(t: T): Out

  def from(r: Out): T
}

trait FirstLowPriority {
  implicit def caseInt: FirstNonTypeConstructor.Aux[Int, Int] =
    new FirstNonTypeConstructor[Int] {
      type Out = Int

      def to(t: Int): Out = t

      def from(r: Out): Int = r
    }

  implicit def caseString: FirstNonTypeConstructor.Aux[String, String] =
    new FirstNonTypeConstructor[String] {
      type Out = String

      def to(t: String): Out = t

      def from(r: Out): String = r
    }

  implicit def caseDouble: FirstNonTypeConstructor.Aux[Double, Double] =
    new FirstNonTypeConstructor[Double] {
      type Out = Double

      def to(t: Double): Out = t

      def from(r: Out): Double = r
    }

  implicit def caseBoolean: FirstNonTypeConstructor.Aux[Boolean, Boolean] =
    new FirstNonTypeConstructor[Boolean] {
      type Out = Boolean

      def to(t: Boolean): Out = t

      def from(r: Out): Boolean = r
    }

  implicit def caseLong: FirstNonTypeConstructor.Aux[Long, Long] =
    new FirstNonTypeConstructor[Long] {
      type Out = Long

      def to(t: Long): Out = t

      def from(r: Out): Long = r
    }
}

trait SecondLowPriority extends FirstLowPriority {

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

object FirstNonTypeConstructor extends SecondLowPriority {

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


