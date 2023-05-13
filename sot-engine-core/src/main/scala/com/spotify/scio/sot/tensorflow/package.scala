package com.spotify.scio.sot

import com.spotify.scio.values._

import scala.reflect.ClassTag

package object tensorflow {

  import scala.language.implicitConversions

  /**
    * Implicit conversion from [[com.spotify.scio.values.SCollection SCollection]] to
    * [[TensorFlowSCollectionFunctions]].
    */
  implicit def makeTensorFlowSCollectionFunctions[T: ClassTag](s: SCollection[T])
  : TensorFlowSCollectionFunctions[T] = new TensorFlowSCollectionFunctions(s)

}
