package com.spotify.scio.sot.accumulator

import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.coders.{Coder, KvCoder, VarIntCoder}
import org.apache.beam.sdk.state.{StateSpec, StateSpecs, ValueState}
import org.apache.beam.sdk.transforms.{DoFn, ParDo}
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, StateId}
import org.apache.beam.sdk.values.KV
import org.apache.commons.lang.ObjectUtils
import parallelai.sot.engine.generic.row.Row
import shapeless.HList
import java.util.function.{BiFunction => JBiFunction, BiPredicate => JBiPredicate, Function => JFunction, Predicate => JPredicate}

import com.spotify.scio.util.Functions

import scala.reflect.ClassTag


class StatefulDoFn[K, V, Out](getValue: V => Int,
                              toOut: (V, Int) => Out)
  extends DoFn[KV[K, V], Out] {

  @StateId("index")
  private val indexSpec: StateSpec[ValueState[Integer]] = StateSpecs.value(VarIntCoder.of())

  @ProcessElement
  def processElement(context: ProcessContext, @StateId("index") index: ValueState[Integer]): Unit = {

    val current: Integer = Option(index.read()).getOrElse(0)
    val key = context.element().getKey
    val value = getValue(context.element().getValue)
    context.output(toOut(context.element().getValue, current))
    index.write(current + value)
  }

}

/**
  * Enhanced version of [[com.spotify.scio.values.SCollection SCollection]] with Accumulator methods.
  */
class AccumulatorSCollectionFunctions[V: ClassTag](@transient val self: SCollection[V])
  extends Serializable {

  def incrementAccumulator[K: ClassTag, Out: ClassTag](keyMapper: V => (K, V), getValue: V => Int, toOut: (V, Int) => Out): SCollection[Out] = {
    val toKvTransform = ParDo.of(Functions.mapFn[V, KV[K, V]](v => {
      val kv = keyMapper(v)
      KV.of(kv._1, kv._2)
    }))
    val o = self.applyInternal(toKvTransform).setCoder(self.getKvCoder[K, V])
    self.context.wrap(o).parDo(new StatefulDoFn(getValue, toOut))
  }

}