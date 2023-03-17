package parallelai.sot.executor.dsl


/**
  * Possible DSL - DRAFT
  *
  * ruleSet = [trigger_name] on [kafka] schema [] topic [] and
  * [hbase1] source [HBase] withTable [] withKey [key] from [trigger] field [/customer/id]
  * and
  * [hbase2] source [HBase] withTable [] withKey [key] from [hbase1] field [family/column]
  * check
  * [hbase1] field [family/column] op [value]
  * and
  * [trigger_name] field [/data/some] op [value]
  * emit
  * [emit1] on [kafka] schema [] topic [] containing
  * [field1] from [trigger_name] field [/data/foo]
  * and
  * [field2] from [hbase1] field [family/column]
  * and
  * [field3] from [hbase2] field [family/column]
  */


object Tester extends App {
  /**
    * Possible DSL - DRAFT
    *
    * ruleSet = [trigger_name] on [kafka] schema [] topic [] and
    * [hbase1] source [HBase] withTable [] withKey [key] from [trigger] field [/customer/id]
    * and
    * [hbase2] source [HBase] withTable [] withKey [key] from [hbase1] field [family/column]
    * check
    * [hbase1] field [family/column] op [value]
    * and
    * [trigger_name] field [/data/some] op [value]
    * emit
    * [emit1] on [kafka] schema [] topic [] containing
    * [field1] from [trigger_name] field [/data/foo]
    * and
    * [field2] from [hbase1] field [family/column]
    * and
    * [field3] from [hbase2] field [family/column]
    */

  import Model._
  import Rules._

  val t1: RuleSet = "trigger_name" on Kafka schema "some" topic "topic" and
    "hbase1" source HBase table "table" schema "something" key "key" from "trigger_name" field "customer/id" as Int and
    "hbase2" source HBase table "table" schema "something" key "key" from "hbase1" field "a/b" as "" check
    "hbase1" field "customer/value" op GT value 5 and
    "hbase2" field "customer/location" op EQ value "London" and
    "trigger_name" field "oops"op LT value 10 emit
    "success_event" on Kafka schema "some" topic "topic" containing
    "custioner/id" from "hbase1" field "customer/id" as Int and
    "field1" from "hbase1" field "a/b" as "" validate

  val t2: RuleSet = "trigger_name" on Kafka schema "some" topic "topic" check
    "hbase1" field "a/b" op GT value 5 and
    "trigger_name" field "oops"op LT value 10 emit
    "success_event" on Kafka schema "some" topic "topic" containing
    "field1" from "hbase1" field "a/b" as Int and
    "field1" from "hbase1" field "a/b" as Double validate

}