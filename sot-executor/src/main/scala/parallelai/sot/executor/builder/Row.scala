package parallelai.sot.executor.builder

import shapeless._
import ops.hlist._
import record._
import shapeless.labelled.{FieldType, field}
import shapeless.ops.record.{Modifier, Selector, _}


trait MapType[A] {
  type B

  def apply(v: A): B
}

trait LowPriorityMapType {

  type Aux[A, B0] = MapType[A] {type B = B0}

  implicit def identityMappable[A]: Aux[A, A] = new MapType[A] {
    type B = A
    override def apply(v: A): B = v
  }
}

object MapType extends LowPriorityMapType {

  implicit val intToLong: Aux[Int, Long] = new MapType[Int] {
    type B = Long
    override def apply(v: Int): Long = v.toLong
  }

  implicit val intToLongOps: Aux[Option[Int], Option[Long]] = new MapType[Option[Int]] {
    type B = Option[Long]
    override def apply(v: Option[Int]): Option[Long] = {
      v match {
        case Some(value) => Some(value.toLong)
        case None => None
      }
    }
  }

  implicit def emumToString[A <: NamedEnum]: Aux[A, String] = new MapType[A] {
    type B = String
    override def apply(v: A): String = v.name
  }

  implicit def emumOpsToString[A <: NamedEnum]: Aux[Option[A], Option[String]] = new MapType[Option[A]] {
    type B = Option[String]
    override def apply(v: Option[A]): Option[String] = {
      v match {
        case Some(o) => Some(o.name)
        case None => None
      }
    }
  }

}

trait FromRecord[L <: HList] {
  type Out <: HList

  def apply(l: L): Out
}

trait LowPriorityFromRecord {

  type Aux[L <: HList, Out0] = FromRecord[L] {type Out = Out0}

  implicit def hconsFromRec0[K <: Symbol, V, VMap,
  T <: HList, TOut <: HList](implicit
                             mappable: MapType.Aux[V, VMap],
                             fromRec: FromRecord.Aux[T, TOut]
                            ): Aux[FieldType[K, V] :: T, FieldType[K, VMap] :: TOut] = new FromRecord[FieldType[K, V] :: T] {
    type Out = FieldType[K, VMap] :: TOut

    def apply(l: FieldType[K, V] :: T): Out =
      field[K](mappable(l.head)) :: fromRec(l.tail)
  }

}

object FromRecord extends LowPriorityFromRecord {

  def apply[L <: HList](implicit toM: FromRecord[L]): Aux[L, toM.Out] = toM

  implicit val hnilFromRec: FromRecord.Aux[HNil, HNil] = new FromRecord[HNil] {
    type Out = HNil

    def apply(l: HNil): HNil = HNil
  }

  implicit def hconsFromRec1[K <: Symbol, V <: Product, H <: HList,
  HRep <: HList, T <: HList, TRep <: HList](implicit
                                            gen: LabelledGeneric.Aux[V, HRep],
                                            fromRecH: FromRecord.Aux[H, HRep],
                                            fromRecT: FromRecord.Aux[T, TRep]
                                           ): Aux[FieldType[K, H] :: T, FieldType[K, V] :: TRep] = new FromRecord[FieldType[K, H] :: T] {

    type Out = FieldType[K, V] :: TRep

    def apply(l: FieldType[K, H] :: T): Out =
      field[K](gen.from(fromRecH(l.head))) :: fromRecT(l.tail)
  }

  //Parsing lists
  implicit def hconsFromRec2[K <: Symbol, V <: Product, H <: HList,
  HRep <: HList, T <: HList, TRep <: HList](implicit
                                            gen: LabelledGeneric.Aux[V, HRep],
                                            fromRecH: FromRecord.Aux[H, HRep],
                                            fromRecT: FromRecord.Aux[T, TRep]
                                           ): Aux[FieldType[K, List[H]] :: T, FieldType[K, List[V]] :: TRep] = new FromRecord[FieldType[K, List[H]] :: T] {

    type Out = FieldType[K, List[V]] :: TRep

    def apply(l: FieldType[K, List[H]] :: T): Out =
      field[K](l.head.map(r => gen.from(fromRecH(r)))) :: fromRecT(l.tail)
  }

  implicit def hconsFromRecOptions[K <: Symbol, V <: Product, H <: HList,
  HRep <: HList, T <: HList, TRep <: HList](implicit
                                            gen: LabelledGeneric.Aux[V, HRep],
                                            fromRecH: FromRecord.Aux[H, HRep],
                                            fromRecT: FromRecord.Aux[T, TRep]
                                           ): Aux[FieldType[K, Option[H]] :: T, FieldType[K, Option[V]] :: TRep] = new FromRecord[FieldType[K, Option[H]] :: T] {

    type Out = FieldType[K, Option[V]] :: TRep

    def apply(l: FieldType[K, Option[H]] :: T): Out = {

      val value: Option[H] = l.head
      value match {
        case Some(v) => field[K](Some(gen.from(fromRecH(v)))) :: fromRecT(l.tail)
        case None => field[K](None) :: fromRecT(l.tail)
      }
    }
  }
}

trait ToRecord[L <: HList] {
  type Out <: HList

  def apply(l: L): Out
}

trait LowPriorityToRecord {

  type Aux[L <: HList, Out0] = ToRecord[L] {type Out = Out0}

  implicit def hconsToRec0[K <: Symbol, V,
  T <: HList, TOut <: HList](implicit
                             toRec: Lazy[ToRecord.Aux[T, TOut]]
                            ): Aux[FieldType[K, V] :: T, FieldType[K, V] :: TOut] = new ToRecord[FieldType[K, V] :: T] {
    type Out = FieldType[K, V] :: TOut

    def apply(l: FieldType[K, V] :: T): Out =
      l.head :: toRec.value(l.tail)
  }
}


object ToRecord extends LowPriorityToRecord {

  def apply[L <: HList](implicit toM: ToRecord[L]): Aux[L, toM.Out] = toM

  implicit val hnilToRec: ToRecord.Aux[HNil, HNil] = new ToRecord[HNil] {
    type Out = HNil

    def apply(l: HNil): HNil = HNil
  }

  implicit def hconsToRec1[K <: Symbol, V <: Product,
  H <: HList, HOut <: HList, T <: HList,
  TOut <: HList](implicit
                 gen: LabelledGeneric.Aux[V, H],
                 toRecH: Lazy[ToRecord.Aux[H, HOut]],
                 toRecT: Lazy[ToRecord.Aux[T, TOut]]
                ): Aux[FieldType[K, V] :: T, FieldType[K, HOut] :: TOut] = new ToRecord[FieldType[K, V] :: T] {

    type Out = FieldType[K, HOut] :: TOut

    def apply(l: FieldType[K, V] :: T): Out =
      field[K](toRecH.value(gen.to(l.head))) :: toRecT.value(l.tail)
  }

  //Parsing lists
  implicit def hconsToRec2[K <: Symbol, V <: Product,
  H <: HList, HOut <: HList, T <: HList,
  TOut <: HList](implicit
                 gen: LabelledGeneric.Aux[V, H],
                 toRecH: ToRecord.Aux[H, HOut],
                 toRecT: Lazy[ToRecord.Aux[T, TOut]]
                ): Aux[FieldType[K, List[V]] :: T, FieldType[K, List[HOut]] :: TOut] = new ToRecord[FieldType[K, List[V]] :: T] {

    type Out = FieldType[K, List[HOut]] :: TOut

    def apply(l: FieldType[K, List[V]] :: T): Out =
      field[K](l.head.map(x => toRecH(gen.to(x)))) :: toRecT.value(l.tail)
  }

  //Parsing Options
  implicit def hconsToRecOptions[K <: Symbol, V <: Product,
  H <: HList, HOut <: HList, T <: HList,
  TOut <: HList](implicit
                 gen: LabelledGeneric.Aux[V, H],
                 toRecH: Lazy[ToRecord.Aux[H, HOut]],
                 toRecT: Lazy[ToRecord.Aux[T, TOut]]
                ): Aux[FieldType[K, Option[V]] :: T, FieldType[K, Option[HOut]] :: TOut] = new ToRecord[FieldType[K, Option[V]] :: T] {

    type Out = FieldType[K, Option[HOut]] :: TOut

    def apply(l: FieldType[K, Option[V]] :: T): Out = {
      val value: Option[V] = l.head
      value match {
        case Some(v) => field[K](Some(toRecH.value(gen.to(v)))) :: toRecT.value(l.tail)
        case None => field[K](None) :: toRecT.value(l.tail)
      }
    }
  }
}

class Row[L <: HList](val hl: L) {

  type FSL[K] = Selector[L, K]

  def updatedAt[V, W, Out <: HList](n: Nat, value: V)(implicit
                                                      replacer: ReplaceAt.Aux[L, n.N, V, (W, Out)]): Out = replacer(hl, value)._2

  def appendList[V <: HList, Out <: HList](v: V)(implicit prep: Prepend.Aux[L, V, Out]) = new Row[Out](prep(hl, v))

  def keys(implicit keys: Keys[L]): keys.Out = keys()

  def get(k: Witness)(implicit selector: Selector[L, k.T]): selector.Out = selector(hl)

  def append[V, Out <: HList](k: Witness, v: V)(implicit updater: Updater.Aux[L, FieldType[k.T, V], Out],
                                                lk: LacksKey[L, k.T]): Row[Out] = {
    new Row(updater(hl, field[k.T](v)))
  }

  def update[W](k: WitnessWith[FSL], value: W)
               (implicit modifier: Modifier[L, k.T, k.instance.Out, W]): Row[modifier.Out] = new Row(modifier(hl, _ => value))

  def updateWith[W](k: WitnessWith[FSL])(f: k.instance.Out => W)
                   (implicit modifier: Modifier[L, k.T, k.instance.Out, W]): Row[modifier.Out] = new Row(modifier(hl, f))

  def remove[V, Out <: HList](k: Witness)(implicit remover: Remover.Aux[L, k.T, (V, Out)]): Row[Out] = new Row(remover(hl)._2)

}

object Row {

  implicit class RowOps[In <: HList, InRep <: HList](a: Row[In]) {
    def to[Out](implicit
                gen: LabelledGeneric.Aux[Out, InRep],
                fromRec: FromRecord.Aux[In, InRep]): Out = gen.from(fromRec.apply(a.hl))
  }

  def apply[P <: Product, L <: HList](p: P)(implicit gen: LabelledGeneric.Aux[P, L], tmr: ToRecord[L]) =
    new Row[tmr.Out](tmr(gen.to(p)))

}