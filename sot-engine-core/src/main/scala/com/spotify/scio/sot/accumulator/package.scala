package com.spotify.scio.sot

import com.spotify.scio.values.SCollection
import scala.reflect.ClassTag

package object accumulator {

    import scala.language.implicitConversions

    /**
      * Implicit conversion from [[com.spotify.scio.values.SCollection SCollection]] to
      * [[AccumulatorSCollectionFunctions]].
      */
    implicit def makeAccumulatorSCollectionFunctions[V: ClassTag](s: SCollection[V])
    : AccumulatorSCollectionFunctions[V] = new AccumulatorSCollectionFunctions(s)


}
