package com.spotify.scio.sot

import com.spotify.scio.values.SCollection
import parallelai.sot.engine.generic.row.Row
import shapeless.HList

import scala.reflect.ClassTag

package object accumulator {

    import scala.language.implicitConversions

    /**
      * Implicit conversion from [[com.spotify.scio.values.SCollection SCollection]] to
      * [[AccumulatorSCollectionFunctions]].
      */
    implicit def makeAccumulatorSCollectionFunctions[V <: HList: ClassTag](s: SCollection[Row.Aux[V]])
    : AccumulatorSCollectionFunctions[V] = new AccumulatorSCollectionFunctions(s)


}
