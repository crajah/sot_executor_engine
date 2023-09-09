package parallelai.sot.engine.generic.row

import shapeless.ops.hlist.IsHCons
import shapeless.{HList, HNil, LabelledGeneric, syntax}
import shapeless.record._
import shapeless.record.Record
import shapeless._
import shapeless.labelled.FieldType
import shapeless.labelled.{FieldType, field}

trait Container[T] {
  val value: T
}

case class Id[T](value: T) extends Container[T]

trait LabelledGenericTC[F[_], T] {
  type Repr <: HList

  def to(t: F[T]): F[Repr]

  def from(r: F[Repr]): F[T]
}

//<: Container[_]
trait LowPriority {
  type Aux[F[_], T, Repr0 <: HList] = LabelledGenericTC[F, T] {type Repr = Repr0}

  implicit def labelledGeneric[T, R <: HList](implicit gen: LabelledGeneric.Aux[T, R]): LabelledGenericTC.Aux[Id, T, R] =
    new LabelledGenericTC[Id, T] {
      type Repr = R

      def to(t: Id[T]): Id[R] = Id[R](gen.to(t.value))

      def from(r: Id[R]): Id[T] = Id[T](gen.from(r.value))
    }
}

import scala.reflect.runtime.universe.{TypeTag, typeOf}

object LabelledGenericTC extends LowPriority {


  //  def apply[F[_], T](implicit lgen: LabelledGenericTC[F, T]): LabelledGenericTC.Aux[F, T, lgen.Repr] = lgen

  //  implicit def labelledGenericOption[F[_], T, R <: HList](implicit gen: LabelledGenericTC.Aux[F, T, R]): LabelledGenericTC.Aux[Option, T, F[R]] =
  //    new LabelledGenericTC[Option, T] {
  //      type Repr = Option[F[R]]
  //
  //      def to(t: Option[T]): Option[R] = t.map(v => gen.to(v))
  //
  //      def from(r: Option[R]): Option[T] = r.map(v => gen.from(v))
  //    }
  //
  //  implicit def labelledGenericList[F, T, R <: HList](implicit gen: LabelledGenericTC.Aux[F, T, R]): LabelledGenericTC.Aux[List, T, R] =
  //    new LabelledGenericTC[List, T] {
  //      type Repr = List[R]
  //
  //      def to(t: List[T]): List[R] = t.map(x => gen.to(x))
  //
  //      def from(r: List[gen.Repr]): List[T] = r.map(x => gen.from(x))
  //    }
  implicit def labelledGenericOption[T, R <: HList](implicit gen: LabelledGeneric.Aux[T, R]): LabelledGenericTC.Aux[Option, T, R] =
    new LabelledGenericTC[Option, T] {
      type Repr = R

      def to(t: Option[T]): Option[R] = t.map(v => gen.to(v))

      def from(r: Option[R]): Option[T] = r.map(v => gen.from(v))
    }

  implicit def labelledGenericList[T, R <: HList](implicit gen: LabelledGeneric.Aux[T, R]): LabelledGenericTC.Aux[List, T, R] =
    new LabelledGenericTC[List, T] {
      type Repr = R

      def to(t: List[T]): List[R] = t.map(x => gen.to(x))

      def from(r: List[gen.Repr]): List[T] = r.map(x => gen.from(x))
    }

}


trait FirstNonConstructor[T] {
  type Out
  type First
  def print(): String

  def to(t: T): Out

  def from(r: Out): T
}

trait FirstLowPriority {
  implicit def caseInt: FirstNonConstructor.Aux[Int, Int, Int] =
    new FirstNonConstructor[Int] {
      type Out = Int
      type First = Int

      def print(): String = s"Base[Int]"

      def to(t: Int): Out = t

      def from(r: Out): Int = r
    }

  implicit def hnil: FirstNonConstructor.Aux[HNil, HNil, HNil] =
    new FirstNonConstructor[HNil] {
      type Out = HNil
      type First = HNil

      def print(): String = s"Base[HNil]"

      def to(t: HNil): Out = HNil

      def from(r: Out): HNil = HNil

    }
}

trait SecondLowPriority extends FirstLowPriority {


  //  implicit def hlist[K <: Symbol, T <: HList, R <: HList, V, H, TAIL <: HList, HR, TR <: HList](implicit tt: TypeTag[T],
  //                                                                             isHCons: IsHCons.Aux[T, H, TAIL],
  ////                                                                             ev: (H :: TAIL) =:= R,
  //                                                                               th: TypeTag[H]
  ////                                                                             firstNCHead: FirstNonConstructor.Aux[H, HR]
  ////                                                                             firstNCTail: FirstNonConstructor.Aux[TAIL, TR]
  //                                                                            ): FirstNonConstructor.Aux[ FieldType[K, V]:: TAIL, HR :: TR] =
  //    new FirstNonConstructor[FieldType[K, V]:: TAIL] {
  //      type Out = HR :: TR
  //
  //      def print(): String = s"Base[${typeOf[T]}, ${typeOf[H]}]"
  //
  //      def to(t: FieldType[K, V]:: TAIL): Out = null.asInstanceOf[Out] //firstNCHead.to(gen.to(t).head) :: firstNCTail.to(gen.to(t).tail)
  //
  //      def from(r: Out): FieldType[K, V]:: TAIL = null.asInstanceOf[FieldType[K, V]:: TAIL] //gen.from(field[K](firstNCHead.from(r.head)) :: firstNCTail.from(r.tail))
  //
  //    }


//  implicit def baseHlist[K <: Symbol, T, R <: HList, V, H, TAIL <: HList, HR, TR <: HList](implicit tt: TypeTag[T],
//                                                                                           //                                                                                           ev1: H =:= FieldType[K, V],
//                                                                                           firstNCHead: FirstNonConstructor.Aux[V, HR],
//                                                                                           firstNCTail: FirstNonConstructor.Aux[TAIL, TR],
//                                                                                           th: TypeTag[H]
//                                                                                          ): FirstNonConstructor.Aux[FieldType[K, V] :: TAIL, HR :: TR] =
//    new FirstNonConstructor[FieldType[K, V] :: TAIL] {
//      type Out = HR :: TR
//      type First = HR :: TR
//
//      def print(): String = s"Base[${typeOf[T]}, HEAD:${firstNCHead.print()}]:TAIL:${firstNCTail.print()}]"
//
//      def to(t: FieldType[K, V] :: TAIL): Out = null.asInstanceOf[Out] //firstNCHead.to(gen.to(t).head) :: firstNCTail.to(gen.to(t).tail)
//
//      def from(r: Out): FieldType[K, V] :: TAIL = null.asInstanceOf[FieldType[K, V] :: TAIL] //gen.from(field[K](firstNCHead.from(r.head)) :: firstNCTail.from(r.tail))
//
//    }

  implicit def baseProduct[K <: Symbol, T, R <: HList, V, H, TAIL <: HList, HR, TR <: HList](implicit tt: TypeTag[T],
                                                                                             gen: LabelledGeneric.Aux[T, R],
                                                                                             deepRec: DeepRec[R],
//                                                                                             isHCons: IsHCons.Aux[R, H, TAIL],
//                                                                                             firstNCHead: FirstNonConstructor.Aux[H :: TAIL, HR],
//                                                                                             firstNCTail: FirstNonConstructor.Aux[TAIL, TR],
                                                                                             th: TypeTag[H]
                                                                                            ): FirstNonConstructor.Aux[T, deepRec.Out, R] =
    new FirstNonConstructor[T] {
      type Out = deepRec.Out
      type First = R

//      def print(): String = s"Base[${typeOf[T]}, HEAD:${firstNCHead.print()}]:TAIL:${firstNCTail.print()}"
      def print(): String = s"Base[${typeOf[T]}"

      def to(t: T): Out = deepRec(gen.to(t))

      def from(r: Out): T = gen.from(deepRec.fromRec(r)) //null.asInstanceOf[T] //gen.from(field[K](firstNCHead.from(r.head)) :: firstNCTail.from(r.tail))

    }

  //  implicit def baseProduct[K <: Symbol, T, R <: HList, H, TAIL <: HList, HR, TR <: HList](implicit tt: TypeTag[T],
  //                                                                             gen: LabelledGeneric.Aux[T, H],
  //                                                                             hdr: Lazy[DeepRec[HR]],
  //                                                                             tdr: Lazy[DeepRec[TR]]
  //                                                                            ): FirstNonConstructor.Aux[T, FieldType[K, hdr.value.Out] :: tdr.value.Out] =
  //
  //  new FirstNonConstructor[T] {
  //    type Out = FieldType[K, hdr.value.Out] :: tdr.value.Out
  //
  //    def print(): String = s"Base[${typeOf[T]}]"
  //
  //    def to(t: T): Out = deepRec.apply(t)
  //
  //    def from(r: Out): T = deepRec.fromRec(r)
  //
  //  }

}

object FirstNonConstructor extends SecondLowPriority {

  type Aux[T, Out0, Out1] = FirstNonConstructor[T] {type Out = Out0; type First = Out1}

  def apply[T]()(implicit firstNonConstructor: FirstNonConstructor[T]): FirstNonConstructor.Aux[T, firstNonConstructor.Out, firstNonConstructor.First] = firstNonConstructor

  implicit def option[T, Out0, Out1](implicit gen: FirstNonConstructor.Aux[T, Out0, Out1]): FirstNonConstructor.Aux[Option[T], Option[Out0], Out1] =
    new FirstNonConstructor[Option[T]] {
      type Out = Option[Out0]
      type First = Out1

      def print(): String = s"Option[${gen.print()}]"

      def to(t: Option[T]): Out = t.map(x => gen.to(x))

      def from(r: Out): Option[T] = r.map(x => gen.from(x))

    }

  implicit def list[T, Out0, Out1](implicit gen: FirstNonConstructor.Aux[T, Out0, Out1]): FirstNonConstructor.Aux[List[T], List[Out0], Out1] =
    new FirstNonConstructor[List[T]] {
      type Out = List[Out0]
      type First = Out1

      def print(): String = s"List[${gen.print()}]"

      def to(t: List[T]): Out = t.map(x => gen.to(x))

      def from(r: Out): List[T] = r.map(x => gen.from(x))

    }
}


