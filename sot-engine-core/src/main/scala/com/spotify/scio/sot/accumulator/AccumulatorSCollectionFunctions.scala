package com.spotify.scio.sot.accumulator

import com.google.datastore.v1.client.DatastoreHelper.makeKey
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.transforms.{DoFn, ParDo}
import org.apache.beam.sdk.transforms.DoFn.ProcessElement
import org.apache.beam.sdk.values.KV
import com.spotify.scio.util.Functions
import org.apache.beam.sdk.io.gcp.datastore.DatastoreIOSOT
import org.joda.time.Instant
import parallelai.sot.engine.Project
import parallelai.sot.engine.generic.row.{JavaRow, Row}
import parallelai.sot.engine.io.datastore._
import shapeless.HList
import java.util.function.{BiFunction => JBiFunction, Function => JFunction}

import scala.reflect.ClassTag

class UpdateTimestampDoFn[V <: HList, Value <: HList] extends DoFn[(JavaRow[V], JavaRow[Value], Instant), (JavaRow[V], JavaRow[Value])] {

  @ProcessElement
  def processElement(context: ProcessContext): Unit = {
    val value = context.element()
    context.outputWithTimestamp((value._1, value._2), value._3)
  }

}

object ScalaToJavaFunctions {

  import scala.language.implicitConversions

  implicit def toJavaFunction[A <: HList, B <: HList](f: Row.Aux[A] => Row.Aux[B]): JFunction[JavaRow[A], JavaRow[B]] = new JFunction[JavaRow[A], JavaRow[B]] with Serializable {
    override def apply(a: JavaRow[A]): JavaRow[B] = JavaRow(f(Row[A](a.hList)).hList)
  }

  implicit def toJavaBiFunction[A <: HList, B <: HList, C <: HList](f: (Row.Aux[A], Row.Aux[B]) => Row.Aux[C]): JBiFunction[JavaRow[A], JavaRow[B], JavaRow[C]] = new JBiFunction[JavaRow[A], JavaRow[B], JavaRow[C]] with Serializable {
    override def apply(a: JavaRow[A], b: JavaRow[B]): JavaRow[C] = JavaRow(f(Row[A](a.hList), Row[B](b.hList)).hList)
  }

  implicit def rowToJavaRow[L <: HList](row: Row.Aux[L]): JavaRow[L] = JavaRow(row.hList)

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

    import ScalaToJavaFunctions._

    val datastore = datastoreSettings.map { case (project, kind) => Datastore(project = project, kind = kind) }

    val toKvTransform = ParDo.of(Functions.mapFn[Row.Aux[V], KV[K, JavaRow[V]]](v => {
      val key = keyMapper(v)
      KV.of(key, JavaRow(v.hList))
    }))

    val o = self.applyInternal(toKvTransform).setCoder(self.getKvCoder[K, JavaRow[V]])
    val statefulStep = self.context.wrap(o).parDo(new StatefulDoFn[K, V, Value](getValue, defaultValue, aggr, datastore, fromL))

    datastoreSettings match {
      case Some((projectName, kind)) =>
        statefulStep.parDo(new UpdateTimestampDoFn[V, Value]()).map { rec =>
          val entity = rec._2.hList.toEntityBuilder()
          val key = keyMapper(Row(rec._1.hList))
          val keyEntity = key match {
            case name: String => makeKey(kind.value, name.asInstanceOf[AnyRef])
            case id: Int => makeKey(kind.value, id.asInstanceOf[AnyRef])
          }
          entity.setKey(keyEntity)
          entity.build()
        }.applyInternal(DatastoreIOSOT.v1.write.withProjectId(projectName.id).removeDuplicatesWithinCommits(true))
      case None =>
    }
    statefulStep.map { case (v, value, _) => toOut(Row(v.hList), Row(value.hList)) }
  }

}