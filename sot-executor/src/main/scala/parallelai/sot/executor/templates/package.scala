package parallelai.sot.executor

import scala.collection.generic.CanBuildFrom

package object templates {

  /**
    * A `CanBuildFrom` for `List` implementing `Serializable`, unlike the one provided by the standard library.
    */

  implicit def listSerializableCanBuildFrom[T]: CanBuildFrom[List[T], T, List[T]] =
    new CanBuildFrom[List[T], T, List[T]] with Serializable {
      def apply(from: List[T]) = from.genericBuilder[T]

      def apply() = List.newBuilder[T]
    }

}
