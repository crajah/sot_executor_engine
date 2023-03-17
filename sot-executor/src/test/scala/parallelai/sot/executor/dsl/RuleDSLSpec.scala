package parallelai.sot.executor.dsl

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import parallelai.sot.executor.dsl.Model._
import parallelai.sot.executor.dsl.Rules._

/**
  * Created by tamjam on 26/06/2017.
  */
class RuleDSLSpec extends WordSpec
  with Matchers
  with BeforeAndAfterAll {

  var ruleSet: RuleSet = _

  override def beforeAll(): Unit = {

    ruleSet = "trigger_name" on Kafka schema "some" topic "topic" and
      "hbase1" source HBase table "table" schema "something" key "key" from "trigger_name" field "customer/id" as Int and
      "hbase2" source HBase table "table" schema "something" key "key" from "hbase1" field "a/b" as "" check
      "hbase1" field "customer/value" op GT value 5 and
      "hbase2" field "customer/location" op EQ value "London" and
      "trigger_name" field "oops" op LT value 10 emit
      "success_event" on Kafka schema "some" topic "topic" containing
      "custioner/id" from "trigger_name" field "customer/id" as Int and
      "field1" from "hbase1" field "a/b" as "" validate

  }

  "RuleDSL" should {

    "generate trigger" in {

      //"trigger_name" on Kafka schema "some" topic "topic"

      val trigger = MsgSource(name = "trigger_name", layer = Kafka, schema = "some", topic = "topic")

      ruleSet.fromSources.trigger should be (trigger)

    }

    "generate first source" in {

      //"hbase1" source HBase table "table" schema "something" key "key" from "trigger_name" field "customer/id" as Int

      val trigger = MsgSource(name = "trigger_name", layer = Kafka, schema = "some", topic = "topic")

      val source0 = OdsSource(name = "hbase1", layer = HBase, table = "table", schema = "something",
        keyMapping = FieldMappingPart[Int](toFieldPath = "key", from = FieldPart[Int](source = trigger, fieldPath = TypedTriggerField[Int]("customer/id"))))

      ruleSet.fromSources.sources(0) should be (source0)

    }

    "generate second source" in {

      //"hbase2" source HBase table "table" schema "something" key "key" from "hbase1" field "a/b" as ""

      val trigger = MsgSource(name = "trigger_name", layer = Kafka, schema = "some", topic = "topic")

      val source0 = OdsSource(name = "hbase1", layer = HBase, table = "table", schema = "something",
        keyMapping = FieldMappingPart[Int](toFieldPath = "key", from = FieldPart[Int](source = trigger, fieldPath = TypedTriggerField[Int]("customer/id"))))

      val source1 = OdsSource(name = "hbase2", layer = HBase, table = "table", schema = "something",
        keyMapping = FieldMappingPart[Int](toFieldPath = "key", from = FieldPart[Int](source = source0, fieldPath = TypedODSField[Int](family = "a", column = "b"))))

      ruleSet.fromSources.sources(1) should be (source1)

    }

    "generate first condition" in {

      //"hbase1" field "customer/value" op GT value 5

      val trigger = MsgSource(name = "trigger_name", layer = Kafka, schema = "some", topic = "topic")

      val source0 = OdsSource(name = "hbase1", layer = HBase, table = "table", schema = "something",
        keyMapping = FieldMappingPart[Int](toFieldPath = "key", from = FieldPart[Int](source = trigger, fieldPath = TypedTriggerField[Int]("customer/id"))))

      val source1 = OdsSource(name = "hbase2", layer = HBase, table = "table", schema = "something",
        keyMapping = FieldMappingPart[Int](toFieldPath = "key", from = FieldPart[Int](source = source0, fieldPath = TypedODSField[Int](family = "a", column = "b"))))

      val condition0 = Condition[Int](lhs = FieldPart[Int](source = source0, fieldPath = TypedODSField[Int](family = "customer", column = "value" )), op = GT, rhs = 5)

      ruleSet.conditions(0) should be (condition0)

    }

    "generate second condition" in {

      //"hbase2" field "customer/location" op EQ value "London" and

      val trigger = MsgSource(name = "trigger_name", layer = Kafka, schema = "some", topic = "topic")

      val source0 = OdsSource(name = "hbase1", layer = HBase, table = "table", schema = "something",
        keyMapping = FieldMappingPart[Int](toFieldPath = "key", from = FieldPart[Int](source = trigger, fieldPath = TypedTriggerField[Int]("customer/id"))))

      val source1 = OdsSource(name = "hbase2", layer = HBase, table = "table", schema = "something",
        keyMapping = FieldMappingPart[Int](toFieldPath = "key", from = FieldPart[Int](source = source0, fieldPath = TypedODSField[Int](family = "a", column = "b"))))

      val condition1 = Condition[String](lhs = FieldPart[String](source = source1, fieldPath = TypedODSField[String](family = "customer", column = "location" )), op = EQ, rhs = "London")

      ruleSet.conditions(1) should be (condition1)

    }

    "generate third condition" in {

      //"trigger_name" field "oops" op LT value 10 emit

      val trigger = MsgSource(name = "trigger_name", layer = Kafka, schema = "some", topic = "topic")

      val source0 = OdsSource(name = "hbase1", layer = HBase, table = "table", schema = "something",
        keyMapping = FieldMappingPart[Int](toFieldPath = "key", from = FieldPart[Int](source = trigger, fieldPath = TypedTriggerField[Int]("customer/id"))))

      val source1 = OdsSource(name = "hbase2", layer = HBase, table = "table", schema = "something",
        keyMapping = FieldMappingPart[Int](toFieldPath = "key", from = FieldPart[Int](source = source0, fieldPath = TypedODSField[Int](family = "a", column = "b"))))

      val condition2 = Condition[Int](lhs = FieldPart[Int](source = trigger, fieldPath = TypedTriggerField(path = "oops")), op = LT, rhs = 10)

      ruleSet.conditions(2) should be (condition2)

    }

    "generate emit event" in {

      //"success_event" on Kafka schema "some" topic "topic" containing

      val emitSource = MsgSource(name = "success_event", layer = Kafka, schema = "some", topic = "topic")

      ruleSet.emitSource should be (emitSource)

    }

    "generate first emit field" in {

      //"custioner/id" from "trigger_name" field "customer/id" as Int and

      val trigger = MsgSource(name = "trigger_name", layer = Kafka, schema = "some", topic = "topic")

      val emitField0 = FieldMappingPart[Int](toFieldPath = "custioner/id", from = FieldPart[Int](source = trigger, fieldPath = TypedTriggerField[Int](path = "customer/id")))

      ruleSet.emitFields(0) should be (emitField0)

    }

    "generate second emit field" in {

      //"field1" from "hbase1" field "a/b" as "" validate

      val trigger = MsgSource(name = "trigger_name", layer = Kafka, schema = "some", topic = "topic")

      val source0 = OdsSource(name = "hbase1", layer = HBase, table = "table", schema = "something",
        keyMapping = FieldMappingPart[Int](toFieldPath = "key", from = FieldPart[Int](source = trigger, fieldPath = TypedTriggerField[Int]("customer/id"))))

      val emitField1 = FieldMappingPart[String](toFieldPath = "field1", from = FieldPart[String](source = source0, fieldPath = TypedODSField[String](family = "a", column = "b")))

      ruleSet.emitFields(1) should be (emitField1)


    }


  }


}
