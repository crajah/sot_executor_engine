package com.spotify.scio.sot.accumulator

import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.coders.{Coder, KvCoder, VarIntCoder}
import org.apache.beam.sdk.state.{StateSpec, StateSpecs, ValueState}
import org.apache.beam.sdk.transforms.{DoFn, ParDo}
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, StateId}
import org.apache.beam.sdk.values.KV

import com.spotify.scio.util.Functions

import scala.reflect.ClassTag

/**
  *
  * @param getValue function to get the value to store from the data object
  * @param aggr aggregate values
  * @param toOut function that creates the output object
  * @param coder value coder for value state
  * @tparam K key
  * @tparam V value
  * @tparam Out output type
  * @tparam Value value type
  */
class StatefulDoFn[K, V, Out, Value](getValue: V => Value,
                                     aggr: (Option[Value], Value) => Value,
                                     toOut: (V, Value) => Out,
                                     coder: Coder[Value])
  extends DoFn[KV[K, V], Out] {

  @StateId("index")
  private val indexSpec: StateSpec[ValueState[Value]] = StateSpecs.value(coder)

  @ProcessElement
  def processElement(context: ProcessContext, @StateId("index") index: ValueState[Value]): Unit = {

    val current: Option[Value] = Option(index.read())
    val value = getValue(context.element().getValue)
    val newValue = aggr(current, value)
    context.output(toOut(context.element().getValue, newValue))
    index.write(newValue)
  }

}

/**
  * Enhanced version of [[com.spotify.scio.values.SCollection SCollection]] with Accumulator methods.
  */
class AccumulatorSCollectionFunctions[V: ClassTag](@transient val self: SCollection[V])
  extends Serializable {

  def incrementAccumulator[K: ClassTag, Out: ClassTag, Value: ClassTag](keyMapper: V => (K, V),
                                                                        getValue: V => Value,
                                                                        aggr: (Option[Value], Value) => Value,
                                                                        toOut: (V, Value) => Out): SCollection[Out] = {
    val toKvTransform = ParDo.of(Functions.mapFn[V, KV[K, V]](v => {
      val kv = keyMapper(v)
      KV.of(kv._1, kv._2)
    }))
    val incrementCoder: Coder[Value] = self.getCoder[Value]
    val o = self.applyInternal(toKvTransform).setCoder(self.getKvCoder[K, V])
    self.context.wrap(o).parDo(new StatefulDoFn(getValue, aggr, toOut, incrementCoder))
  }

}