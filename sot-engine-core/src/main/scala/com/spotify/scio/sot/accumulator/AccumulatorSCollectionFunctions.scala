package com.spotify.scio.sot.accumulator

import com.google.datastore.v1.client.DatastoreHelper.makeKey
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.coders.Coder
import org.apache.beam.sdk.state.{StateSpec, StateSpecs, ValueState}
import org.apache.beam.sdk.transforms.{DoFn, ParDo}
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, StateId}
import org.apache.beam.sdk.values.KV
import com.spotify.scio.util.Functions
import org.apache.beam.sdk.io.gcp.datastore.DatastoreIOSOT
import org.joda.time.Instant
import parallelai.sot.engine.Project
import parallelai.sot.engine.generic.row.Row
import parallelai.sot.engine.io.datastore._
import shapeless.HList

import scala.reflect.ClassTag

/**
  * StatefulDoFn keeps track of a state of the marked variables and stores them to datastore if persistence is provided.
  *
  * @param getValue     function to get the value to keep track of from the data object
  * @param defaultValue value to initialise the state
  * @param aggr         aggregate values
  * @param persistence  storage option to persist states
  * @param coder        value coder for value state
  * @tparam K     key
  * @tparam V     value
  * @tparam Value value type
  */
class StatefulDoFn[K, V <: HList, Value <: HList](getValue: Row.Aux[V] => Row.Aux[Value],
                                                  defaultValue: Row.Aux[Value],
                                                  aggr: (Row.Aux[Value], Row.Aux[Value]) => Row.Aux[Value],
                                                  persistence: Option[Datastore],
                                                  coder: Coder[Row.Aux[Value]])(implicit toL: ToEntity[Value], fromL: FromEntity[Value])
  extends DoFn[KV[K, Row.Aux[V]], (Row.Aux[V], Row.Aux[Value], Instant)] {

  @StateId("value")
  private val stateSpec: StateSpec[ValueState[Row.Aux[Value]]] = StateSpecs.value(coder)

  @ProcessElement
  def processElement(context: ProcessContext, @StateId("value") state: ValueState[Row.Aux[Value]]): Unit = {

    val key = context.element().getKey

    val current = readValue(key, Option(state.read()))

    val value = getValue(context.element().getValue)
    val newValue = aggr(current.getOrElse(defaultValue), value)
    context.output((context.element().getValue, newValue, Instant.now()))
    state.write(newValue)
  }

  private def readValue(key: K, value: Option[Row.Aux[Value]]) = {
    value match {
      case Some(c) => Option(c)
      case None =>
        persistence match {
          case Some(p) =>
            key match {
              case k: Int => p.getRow(k)
              case k: String => p.getRow(k)
              case _ => throw new Exception("Only String and Int type keys supports persistence.")
            }
          case None => None
        }
    }
  }

}

class UpdateTimestampDoFn[V <: HList, Value <: HList] extends DoFn[(Row.Aux[V], Row.Aux[Value], Instant), (Row.Aux[V], Row.Aux[Value])] {

  @ProcessElement
  def processElement(context: ProcessContext): Unit = {
    val value = context.element()
    context.outputWithTimestamp((value._1, value._2), value._3)
  }
}

/**
  * Enhanced version of [[com.spotify.scio.values.SCollection SCollection]] with Accumulator methods.
  */
class AccumulatorSCollectionFunctions[V <: HList](@transient val self: SCollection[Row.Aux[V]])
  extends Serializable {

  def accumulator[K: ClassTag, Out <: HList, Value <: HList](keyMapper: Row.Aux[V] => K,
                                                             getValue: Row.Aux[V] => Row.Aux[Value],
                                                             defaultValue: Row.Aux[Value],
                                                             aggr: (Row.Aux[Value], Row.Aux[Value]) => Row.Aux[Value],
                                                             toOut: (Row.Aux[V], Row.Aux[Value]) => Row.Aux[Out],
                                                             datastoreSettings: Option[(Project, Kind)]
                                                            )(implicit toL: ToEntity[Value], fromL: FromEntity[Value]): SCollection[Row.Aux[Out]] = {

    val datastore = datastoreSettings.map { case (project, kind) => Datastore(project = project, kind = kind) }

    val toKvTransform = ParDo.of(Functions.mapFn[Row.Aux[V], KV[K, Row.Aux[V]]](v => {
      val key = keyMapper(v)
      KV.of(key, v)
    }))
    val valueCoder: Coder[Row.Aux[Value]] = self.getCoder[Row.Aux[Value]]
    val o = self.applyInternal(toKvTransform).setCoder(self.getKvCoder[K, Row.Aux[V]])
    val statefulStep = self.context.wrap(o).parDo(new StatefulDoFn(getValue, defaultValue, aggr, datastore, valueCoder))

    datastoreSettings match {
      case Some((projectName, kind)) =>
        statefulStep.parDo(new UpdateTimestampDoFn[V, Value]()).map { rec =>
          val entity = rec._2.hList.toEntityBuilder
          val key = keyMapper(rec._1)
          val keyEntity = key match {
            case name: String => makeKey(kind.value, name.asInstanceOf[AnyRef])
            case id: Int => makeKey(kind.value, id.asInstanceOf[AnyRef])
          }
          entity.setKey(keyEntity)
          entity.build()
        }.applyInternal(DatastoreIOSOT.v1.write.withProjectId(projectName.id).removeDuplicatesWithinCommits(true))
      case None =>
    }
    statefulStep.map { case (v, value, _) => toOut(v, value) }
  }
}