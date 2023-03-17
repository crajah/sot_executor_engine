package parallelai.sot.executor.dsl

import parallelai.sot.executor.base.Configurable
import parallelai.sot.executor.dsl.Model._

import scala.concurrent.duration._
import scala.concurrent.duration.Duration._


/**
  * Created by crajah on 26/06/2017.
  */
trait SourceMaker {
  import Model._

  def makeSource(name: String, trigger: MsgSource, sources: Seq[OdsSource]):Source =
    if(trigger.name.equals(name))
      trigger
    else findSource(sources, name)

  private def findSource(sources: Seq[OdsSource], name: String) = {
    sources.filter(_.name.equals(name) ) match {
      case h::Nil => h
      case _ => throw new Exception(s"Can't find a unique source with the name: ${name}")
    }
  }
}

trait FieldMaker {
  def makeField(fname: String, mapped: Source) = {
    mapped match {
      case _ : MsgSource => TriggerField(fname)
      case _ : OdsSource => {
        val fs = fname.split("/")
        require(fs.length == 2, s"${fname} is not splittable into Family and Column. Expected format family/column")
        ODSField(fs(0), fs(1))
      }
    }
  }
}

sealed trait Field
case class TriggerField(path: String) extends Field
case class ODSField(family: String, column: String) extends Field

sealed trait TypedField[T]
case class TypedTriggerField[T](path: String) extends TypedField[T]
case class TypedODSField[T](family: String, column: String) extends TypedField[T]

sealed trait Source
case class MsgSource(name: String, layer: MessageLayer, schema: String, topic: String) extends Source
case class OdsSource(name: String, layer: ODSLayer, table: String, schema: String, keyMapping: FieldMappingPart[_]) extends Source


object Emits {
  import Model._
  import Fields._

  case class Emit1(sources: SourcesFromPart, conditions: Seq[Condition[_]], name: String) {
    def on(messageLayer: MessageLayer) = Emit2(sources, conditions, name, messageLayer)
  }
  case class Emit2(sources: SourcesFromPart, conditions: Seq[Condition[_]], name: String, messageLayer: MessageLayer) {
    def schema(schema: String) = Emit3(sources, conditions, name, messageLayer, schema)
  }
  case class Emit3(sources: SourcesFromPart, conditions: Seq[Condition[_]], name: String, messageLayer: MessageLayer, schema: String) {
    def topic(topic: String) = EmitHolder(sources, conditions, MsgSource(name, messageLayer, schema, topic))
  }
  case class EmitHolder(sources: SourcesFromPart, conditions: Seq[Condition[_]], emitSource: MsgSource) extends SourceMaker {
    def containing(_s: String) = EmitField1(sources, conditions, emitSource, Seq(), _s)
  }

  case class EmitField1(sources: SourcesFromPart, conditions: Seq[Condition[_]], emitSource: MsgSource, prevEmitFields: Seq[FieldMappingPart[_]], to: String) extends SourceMaker {
    def from(_s: String) = {
      val _src = makeSource(_s, sources.trigger, sources.sources)
      EmitField2(sources, conditions, emitSource, prevEmitFields, to, _src)
    }
  }
  case class EmitField2(sources: SourcesFromPart, conditions: Seq[Condition[_]], emitSource: MsgSource, prevEmitFields: Seq[FieldMappingPart[_]], to: String, fromSrc: Source) extends FieldMaker {
    def field(_s: String) = {
      val _fld: Field = makeField(_s, fromSrc)
      EmitField3(sources, conditions, emitSource, prevEmitFields, to, fromSrc, _fld)
    }
  }
  case class EmitField3(sources: SourcesFromPart, conditions: Seq[Condition[_]], emitSource: MsgSource, prevEmitFields: Seq[FieldMappingPart[_]], to: String, fromSrc: Source, fromField: Field) {
    def as[T](t: T) = {
      val mapping = FieldMappingPart[T](to, FieldPart[T](fromSrc, fromFieldToTypedField(fromField)))
      EmitFieldHolder(sources, conditions, emitSource, prevEmitFields :+ mapping)
    }
  }
  case class EmitFieldHolder(sources: SourcesFromPart, conditions: Seq[Condition[_]], emitSource: MsgSource, emitFields: Seq[FieldMappingPart[_]]) {
    def and(_s: String) = EmitField1(sources, conditions, emitSource, emitFields, _s)
    def validate() = RuleSet(sources, conditions, emitSource, emitFields)
  }
}

object Model {
  import Emits._

  sealed trait MessageLayer extends Configurable
  case object Kafka extends MessageLayer

  sealed trait ODSLayer extends Configurable
  case object HBase extends ODSLayer


  sealed trait AGGREGATE_TYPE
  case object SUM extends AGGREGATE_TYPE
  case object COUNT extends AGGREGATE_TYPE
  case object MEAN extends AGGREGATE_TYPE
  case object MEDIAM extends AGGREGATE_TYPE
  case object MODE extends AGGREGATE_TYPE
  case object STDDEV extends AGGREGATE_TYPE

  sealed trait OP
  case object GT extends OP // Greater than
  case object GE extends OP // Greater than Equals
  case object LT extends OP // Less than
  case object LE extends OP // Less than equals
  case object EQ extends OP // Equals
  case object NE extends OP // Not Equals
  case object IN extends OP // In list of values
  case object HS extends OP // Contains
  case object ST extends OP // Starts with
  case object ED extends OP // Ends with

  case class SourcesFromPart(trigger: MsgSource, sources: Seq[OdsSource])
  case class FieldPart[T](source: Source, fieldPath: TypedField[T])

  case class RuleSet(fromSources: SourcesFromPart, conditions: Seq[Condition[_]], emitSource: MsgSource, emitFields: Seq[FieldMappingPart[_]] )
  case class Condition[T](lhs:FieldPart[T], op: OP, rhs: T)
  case class FieldMappingPart[T](toFieldPath: String, from: FieldPart[T] )

  case class UpdateSet(from: SourcesFromPart, updates: Seq[UpdateMap] )
  case class UpdateMap(to: FieldPart[_], from: FieldPart[_], aggregate: AggregatePart )
  case class AggregatePart(aggregate: AGGREGATE_TYPE, period: Duration)

}

object Rules {
  import Model._
  import Sources._
  import Conditions._

  implicit def strToTrigger1(name: String): Trigger1 = Trigger1(name)

  case class Trigger1(name: String) {
    def on(messageLayer: MessageLayer) = Trigger2(name, messageLayer)
  }
  case class Trigger2(name: String, messageLayer: MessageLayer) {
    def schema(schema: String) = Trigger3(name, messageLayer, schema)
  }
  case class Trigger3(name: String, messageLayer: MessageLayer, schema: String) {
    def topic(topic: String) = TriggerHolder(MsgSource(name, messageLayer, schema, topic))
  }
  case class TriggerHolder(trigger: MsgSource) extends SourceMaker {
    def and(_s: String) = Source1(trigger, Seq(), _s)
    def check(_s: String) = {
      val _src = makeSource(_s, trigger, Seq())
      Condition1(SourcesFromPart(trigger, Seq()), Seq(), _src)
    }
  }
}

object Fields {
  import Model._

  def fromFieldToTypedField[T](field: Field): TypedField[T] = field match {
    case TriggerField(path) => TypedTriggerField[T](path)
    case ODSField(family, column) => TypedODSField[T](family, column)
  }
}

object Sources {
  import Model._
  import Fields._
  import Conditions._

  case class Source1(trigger: MsgSource, prevSources: Seq[OdsSource], name: String) {
    def source(layer: ODSLayer) = Source2(trigger, prevSources, name, layer)
  }
  case class Source2(trigger: MsgSource, prevSources: Seq[OdsSource], name: String, layer: ODSLayer) {
    def table(_s: String) = Source3(trigger, prevSources, name, layer, _s)
  }

  case class Source3(trigger: MsgSource, prevSources: Seq[OdsSource], name: String, layer: ODSLayer, table: String) {
    def schema(_s: String) = Source4(trigger, prevSources, name, layer, table, _s)
  }

  case class Source4(trigger: MsgSource, prevSources: Seq[OdsSource], name: String, layer: ODSLayer, table: String, schema: String) {
    def key(_s: String) = Source5(trigger, prevSources, name, layer, table, schema, _s)
  }
  case class Source5(trigger: MsgSource, prevSources: Seq[OdsSource], name: String, layer: ODSLayer, table: String, schema: String, key: String) extends SourceMaker {
    def from(_s: String) = {
      val _src = makeSource(_s, trigger, prevSources)
      Source6(trigger, prevSources, name, layer, table, schema, key, _src)
    }
  }
  case class Source6(trigger: MsgSource, prevSources: Seq[OdsSource], name: String, layer: ODSLayer, table: String, schema: String, key: String, fromSrc: Source) extends FieldMaker {
    def field(_s: String) = {
      val _fld: Field = makeField(_s, fromSrc)
      Source7(trigger, prevSources, name, layer, table, schema, key, fromSrc, _fld)
    }
  }

  case class Source7(trigger: MsgSource, prevSources: Seq[OdsSource], name: String, layer: ODSLayer, table: String, schema: String, key: String, fromSrc: Source, fromField: Field) extends FieldMaker {
    def as[T](_t: T) = {
      val odsSrc = OdsSource(name, layer, table, schema, FieldMappingPart[T](key, FieldPart[T](fromSrc, fromFieldToTypedField(fromField))))
      SourcesHolder(trigger, prevSources :+ odsSrc)
    }
  }
  case class SourcesHolder(trigger: MsgSource, sources: Seq[OdsSource]) extends SourceMaker {
    def and(_s: String) = Source1(trigger, sources, _s)
    def check(_s: String) = {
      val _src = makeSource(_s, trigger, sources)
      Condition1(SourcesFromPart(trigger, sources), Seq(), _src)
    }
  }
}

object Conditions {
  import Model._
  import Fields._
  import Emits._

  case class Condition1(sources: SourcesFromPart, prevConds: Seq[Condition[_]], lhsSrc: Source) extends FieldMaker {
    def field(_s: String) = {
      val _fld: Field = makeField(_s, lhsSrc)
      Condition2(sources, prevConds, lhsSrc, _fld)
    }
  }
  case class Condition2(sources: SourcesFromPart, prevConds: Seq[Condition[_]], lhsSrc: Source, lhsField: Field) {
    def op(_op: OP) = Condition3(sources, prevConds, lhsSrc, lhsField, _op)
  }
  case class Condition3(sources: SourcesFromPart, prevConds: Seq[Condition[_]], lhsSrc: Source, lhsField: Field, op: OP) {
    def value[T](_v: T) = {
      val _cond = Condition[T](FieldPart[T](lhsSrc, fromFieldToTypedField(lhsField)), op, _v)
      ConditionsHolder(sources, prevConds :+ _cond)
    }
  }
  case class ConditionsHolder(sources: SourcesFromPart, conditions: Seq[Condition[_]]) extends SourceMaker {
    def and(_s: String) = {
      val _src = makeSource(_s, sources.trigger, sources.sources)
      Condition1(sources, conditions, _src)
    }
    def emit(_s: String) = Emit1(sources, conditions, _s)
  }
}

object Updates {
  case class Front(trigger: MsgSource, sources: Seq[OdsSource])
  case class UpdateItem()
  case class UpdateSet(prefix: Front, updates: Seq[UpdateItem])
}
