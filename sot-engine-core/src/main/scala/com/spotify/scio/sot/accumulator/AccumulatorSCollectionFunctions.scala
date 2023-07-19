package com.spotify.scio.sot.accumulator

import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.coders.Coder
import org.apache.beam.sdk.state.{StateSpec, StateSpecs, ValueState}
import org.apache.beam.sdk.transforms.{DoFn, ParDo}
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, StateId}
import org.apache.beam.sdk.values.KV
import com.spotify.scio.util.Functions
import parallelai.sot.engine.Project
import parallelai.sot.engine.io.datastore._
import shapeless.{HList, LabelledGeneric}

import scala.reflect.ClassTag

/**
  *
  * @param getValue function to get the value to store from the data object
  * @param aggr     aggregate values
  * @param toOut    function that creates the output object
  * @param coder    value coder for value state
  * @tparam K     key
  * @tparam V     value
  * @tparam Out   output type
  * @tparam Value value type
  */
class StatefulDoFn[K, V, Out, Value, L <: HList](getValue: V => Value,
                                     aggr: (Option[Value], Value) => Value,
                                     toOut: (V, Value) => Out,
                                     persistence: Option[Datastore],
                                     coder: Coder[Value])(implicit gen: LabelledGeneric.Aux[StatefulDoFn.State[Value], L], toL: ToEntity[L], fromL: FromEntity[L])
  extends DoFn[KV[K, V], Out] {

  @StateId("value")
  private val stateSpec: StateSpec[ValueState[Value]] = StateSpecs.value(coder)

  @ProcessElement
  def processElement(context: ProcessContext, @StateId("value") state: ValueState[Value]): Unit = {

    val key = context.element().getKey

    val current = getValue(key, Option(state.read()))

    val value = getValue(context.element().getValue)
    val newValue = aggr(current, value)
    context.output(toOut(context.element().getValue, newValue))
    state.write(newValue)
    persistValue(key, newValue)
  }

  private def persistValue(key:K, value: Value) = {
    persistence match {
      case Some(p) =>
        key match {
          case k: Int => p.put(k, StatefulDoFn.State[Value](value))
          case k: String => p.put(k, StatefulDoFn.State[Value](value))
          case _ => throw new Exception("Only String and Int type keys supports persistence.")
        }
      case None =>
    }
  }

  private def getValue(key: K, value: Option[Value]) = {
    value match {
      case Some(c) => Option(c)
      case None =>
        persistence match {
          case Some(p) =>
            key match {
              case k: Int => p.get[StatefulDoFn.State[Value]](k).map(_.state)
              case k: String => p.get[StatefulDoFn.State[Value]](k).map(_.state)
              case _ => throw new Exception("Only String and Int type keys supports persistence.")
            }
          case None => None
        }
    }
  }

}

object StatefulDoFn {

  case class State[Value](state: Value)

}

/**
  * Enhanced version of [[com.spotify.scio.values.SCollection SCollection]] with Accumulator methods.
  */
class AccumulatorSCollectionFunctions[V: ClassTag](@transient val self: SCollection[V])
  extends Serializable {

  def accumulator[K: ClassTag, Out: ClassTag, Value: ClassTag, HS <: HList](keyMapper: V => (K, V),
                                                               getValue: V => Value,
                                                               aggr: (Option[Value], Value) => Value,
                                                               toOut: (V, Value) => Out,
                                                               datastoreSettings: Option[(Project, Kind)]
                                                              )(implicit gen: LabelledGeneric.Aux[StatefulDoFn.State[Value], HS], toL: ToEntity[HS], fromL: FromEntity[HS]): SCollection[Out] = {

    val datastore = datastoreSettings.map{case (project, kind) => Datastore(project = project, kind = kind)}
    val toKvTransform = ParDo.of(Functions.mapFn[V, KV[K, V]](v => {
      val kv = keyMapper(v)
      KV.of(kv._1, kv._2)
    }))
    val valueCoder: Coder[Value] = self.getCoder[Value]
    val o = self.applyInternal(toKvTransform).setCoder(self.getKvCoder[K, V])
    self.context.wrap(o).parDo(new StatefulDoFn(getValue, aggr, toOut, datastore, valueCoder))
  }

}