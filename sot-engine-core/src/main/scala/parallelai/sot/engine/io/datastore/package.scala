package parallelai.sot.engine.io

import com.google.datastore.v1.Entity
import parallelai.sot.engine.io.datatype.{CanNest, FromMappable, ToMappable}
import shapeless._

package object datastore extends DatastoreMappableType with Serializable {
  type FromEntity[L <: HList] = FromMappable[L, Entity.Builder]
  type ToEntity[L <: HList] = ToMappable[L, Entity.Builder]

  implicit object DatastoreCanNest extends CanNest[Entity.Builder]

  implicit class ConvertDS[A <: HList](a: A) extends Serializable {

    def toEntityBuilder()(implicit toL: ToEntity[A]): Entity.Builder = toL(a)

    def toEntity()(implicit toL: ToEntity[A]): Entity = toL(a).build()
  }

}
