package parallelai.sot.engine.generic.row

import shapeless.labelled.FieldType
import shapeless.HNil
import shapeless.ops.record.LacksKey
import shapeless._

trait IsKeyDuplicated[A <: HList] {

  def apply(l: A): A

}

object IsKeyDuplicated {

  def apply[A <: HList](implicit isKeyDuplicated: IsKeyDuplicated[A]): IsKeyDuplicated[A] = isKeyDuplicated

  implicit val hnilOptionExtractor: IsKeyDuplicated[HNil] = new IsKeyDuplicated[HNil] {
    def apply(l: HNil) = l
  }

  implicit def hconsIsDuplicated[K, V, T <: HList](implicit lacksKey: LacksKey[T, K], isKeyDuplicated: IsKeyDuplicated[T]) = new IsKeyDuplicated[FieldType[K, V] :: T] {
    def apply(l: FieldType[K, V] :: T): FieldType[K, V] :: T = l.head :: isKeyDuplicated(l.tail)
  }

}

