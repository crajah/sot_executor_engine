package com.spotify.scio.sot.accumulator

import com.google.datastore.v1.client.DatastoreHelper.makeKey
import com.spotify.scio.coders.KryoAtomicCoder
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.coders.{Coder, SerializableCoder, VarIntCoder}
import org.apache.beam.sdk.state.{StateSpec, StateSpecs, ValueState}
import org.apache.beam.sdk.transforms.{DoFn, ParDo}
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, StateId}
import org.apache.beam.sdk.values.KV
import com.spotify.scio.util.{Functions, ScioUtil}
import org.apache.beam.sdk.io.gcp.datastore.DatastoreIOSOT
import org.joda.time.Instant
import parallelai.sot.engine.Project
import parallelai.sot.engine.generic.row.Row
import parallelai.sot.engine.io.datastore._
import shapeless.HList
import java.util.function.{BiFunction => JBiFunction, BiPredicate => JBiPredicate, Function => JFunction, Predicate => JPredicate}

import scala.reflect.ClassTag
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
  * StatefulDoFn keeps track of a state of the marked variables and stores them to datastore if persistence is provided.
  *
  * @param getValue     function to get the value to keep track of from the data object
  * @param defaultValue value to initialise the state
  * @param aggr         aggregate values
  * @param persistence  storage option to persist states
  * @tparam K     key
  * @tparam V     value
  * @tparam Value value type
  */
class StatefulDoFnScala[K, V <: HList, Value <: HList](getValue: V => Value,
                                                  defaultValue: Value,
                                                  aggr: (Value, Value) => Value,
                                                  persistence: Option[Datastore])(implicit toL: ToEntity[Value], fromL: FromEntity[Value], c: ClassTag[Value])
  extends DoFn[KV[K, V], (V, Value, Instant)] {

  private val LOG: Logger = LoggerFactory.getLogger(getClass)

  @StateId("value")
  private val stateSpec: StateSpec[ValueState[Value]] = StateSpecs.value(KryoAtomicCoder[Value])

  @StateId("teststate")
  private val stateSpec2: StateSpec[ValueState[Integer]] = StateSpecs.value(VarIntCoder.of())


  @ProcessElement
  def processElement(context: ProcessContext, @StateId("value") state: ValueState[Value], @StateId("teststate") state2: ValueState[Integer]): Unit = {

    val ss = state2.read()
    LOG.error(s"reading test state ${ss}")

    val updated = Option(ss).getOrElse(0).asInstanceOf[Integer] + 1
    state2.write(updated)

    val key = context.element().getKey

    val s = state.read()
    LOG.error(s"reading state ${s}")

    val current = StatefulDoFnScala.readValue[K, Value](key, Option(state.read()), persistence, fromL)

    val value = getValue(context.element().getValue)
    val newValue = aggr(current.getOrElse(defaultValue), value)
    context.output((context.element().getValue, newValue, Instant.now()))

    LOG.error(s"writing state ${newValue}")

    state.write(newValue)
  }

}

object StatefulDoFnScala {

  def readValue[K, Value <: HList](key: K, value: Option[Value],
                                   persistence: Option[Datastore],
                                   fromL: FromEntity[Value]): Option[Value] = {
    value match {
      case Some(st) => Option(st)
      case None =>
        persistence match {
          case Some(p) =>
            key match {
              case k: Int => p.getHList(k)(fromL)
              case k: String => p.getHList(k)(fromL)
              case _ => throw new Exception("Only String and Int type keys supports persistence.")
            }
          case None => None
        }
    }
  }

}

class UpdateTimestampDoFn[V <: HList, Value <: HList] extends DoFn[(V, Value, Instant), (V, Value)] {

  @ProcessElement
  def processElement(context: ProcessContext): Unit = {
    val value = context.element()
    context.outputWithTimestamp((value._1, value._2), value._3)
  }
}

object ScalaToJavaFunctions {

  import scala.language.implicitConversions

  implicit def toJavaFunction[A, B](f: A => B): JFunction[A, B] = new JFunction[A, B] with Serializable {
    override def apply(a: A): B = f(a)
  }

  implicit def toJavaPredicate[A](f: A => Boolean): JPredicate[A] = new JPredicate[A] with Serializable {
    override def test(a: A): Boolean = f(a)
  }

  implicit def toJavaBiFunction[A, B, C](f: (A, B) => C): JBiFunction[A, B, C] = new JBiFunction[A, B, C] with Serializable {
    override def apply(a: A, b: B): C = f(a, b)
  }

  implicit def toJavaBiPredicate[A, B](f: (A, B) => Boolean): JBiPredicate[A, B] = new JBiPredicate[A, B] with Serializable {
    override def test(a: A, b: B): Boolean = f(a, b)
  }

}

/**
  * Enhanced version of [[com.spotify.scio.values.SCollection SCollection]] with Accumulator methods.
  */
class AccumulatorSCollectionFunctions[V <: HList : ClassTag](@transient val self: SCollection[Row.Aux[V]])
  extends Serializable {

  def accumulator[K: ClassTag, Out <: HList, Value <: HList : ClassTag](keyMapper: Row.Aux[V] => K,
                                                             getValue: V => Value,
                                                             defaultValue: Value,
                                                             aggr: (Value, Value) => Value,
                                                             toOut: (V, Value) => Out,
                                                             datastoreSettings: Option[(Project, Kind)]
                                                            )(implicit toL: ToEntity[Value], fromL: FromEntity[Value]): SCollection[Row.Aux[Out]] = {

    import ScalaToJavaFunctions._

    val datastore = datastoreSettings.map { case (project, kind) => Datastore(project = project, kind = kind) }

    val toKvTransform = ParDo.of(Functions.mapFn[Row.Aux[V], KV[K, V]](v => {
      val key = keyMapper(v)
      KV.of(key, v.hList)
    }))

    val o = self.applyInternal(toKvTransform).setCoder(self.getKvCoder[K, V])
    val statefulStep = self.context.wrap(o).parDo(new StatefulDoFn(getValue, defaultValue, aggr, datastore, fromL))

    datastoreSettings match {
      case Some((projectName, kind)) =>
        statefulStep.parDo(new UpdateTimestampDoFn[V, Value]()).map { rec =>
          val entity = rec._2.toEntityBuilder
          val key = keyMapper(Row(rec._1))
          val keyEntity = key match {
            case name: String => makeKey(kind.value, name.asInstanceOf[AnyRef])
            case id: Int => makeKey(kind.value, id.asInstanceOf[AnyRef])
          }
          entity.setKey(keyEntity)
          entity.build()
        }.applyInternal(DatastoreIOSOT.v1.write.withProjectId(projectName.id).removeDuplicatesWithinCommits(true))
      case None =>
    }
    statefulStep.map { case (v, value, _) => Row(toOut(v, value)) }
  }
}