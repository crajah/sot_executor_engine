package parallelai.sot.macros

import scala.meta.{Term, _}
import spray.json.{JsArray, JsObject, JsString}
import org.scalatest.compatible.Assertion
import org.scalatest.{FlatSpec, Matchers}
import parallelai.sot.engine.config.SchemaResourcePath
import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.executor.model.SOTMacroJsonConfig

class SOTMacroBuilderSpec extends FlatSpec with Matchers {

//  "SOTMacroBuilder service" should "build pubsub to bigquery macro" in {
//    val avroFields =
//      JsArray(JsObject(Map(
//        "name" -> JsString("user"),
//        "type" -> JsString("string")
//      )),
//        JsObject(Map(
//          "name" -> JsString("score"),
//          "type" -> JsString("in")
//        )))
//
//    val avroDef = AvroDefinition(`type` = "record", name = "MessageExtended", namespace = "parallelai.sot.avro", fields = avroFields)
//    val in = AvroSchema(`type` = "avro", id = "inschema1", name = "inschema1", version = "", definition = avroDef)
//
//    val bqFields = JsArray(JsObject(Map(
//      "mode" -> JsString("REQUIRED"),
//      "name" -> JsString("user"),
//      "type" -> JsString("STRING")
//    )),
//      JsObject(Map(
//        "mode" -> JsString("REQUIRED"),
//        "name" -> JsString("total_score"),
//        "type" -> JsString("INTEGER")
//      )))
//
//    val bqDefinition = BigQueryDefinition(`type` = "bigquerydefinition", name = "BigQueryRow", fields = bqFields)
//    val out = BigQuerySchema(`type` = "bigquery", id = "outschema1", name = "outschema1", version = "", definition = bqDefinition)
//
//    val schemas = List(in, out)
//
//    val taps = List(
//      PubSubTapDefinition(`type` = "pubsub", id = "insource", topic = "p2pin", None, None, None),
//      BigQueryTapDefinition(`type` = "bigquery", id = "outsource", dataset = "dataset1", table = "table1", None, None)
//
//    )
//
//    val dag = List(
//      DAGMapping(from = "in", to = "mapper1"),
//      DAGMapping(from = "mapper1", to = "mapper2"),
//      DAGMapping(from = "mapper2", to = "out")
//    )
//
//    val steps = List(
//      SourceOp(`type` = "source", id = "in", name = "in", schema = "inschema1", tap = "insource"),
//      TransformationOp(`type` = "transformation", id = "mapper1", name = "mapper1", op = "map", params = List(List("m => BigQueryRow(m, (m.name, m.count))")), paramsEncoded = false),
//      TransformationOp(`type` = "transformation", id = "mapper2", name = "mapper2", op = "map", params = List(List("((Row(('events ->> List()) :: HNil)), ({(aggr, m) => Row(('events ->> aggr.get('events) :+ m.get('eventName)) :: HNil)}, {(aggr1, aggr2) => Row(('events ->> aggr1.get('events) ++ aggr2.get('events)) :: HNil)}))")), paramsEncoded = false),
//      SinkOp(`type` = "sink", id = "out", name = "out", schema = Some("outschema1"), tap = "outsource")
//    )
//
//    val config = Config(id = "", name = "", version = "", schemas = schemas, taps = taps, dag = dag, steps = steps)
//
//    val expectedBlock =
//      q"""
//            `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"MessageExtended\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
//            class MessageExtended
//            `@BigQueryType`.fromSchema("{\"type\":\"bigquerydefinition\",\"name\":\"BigQueryRow\",\"fields\":[{\"mode\":\"REQUIRED\",\"name\":\"user\",\"type\":\"STRING\"},{\"mode\":\"REQUIRED\",\"name\":\"total_score\",\"type\":\"INTEGER\"}]}")
//            class BigQueryRow
//            def transform(in: SCollection[MessageExtended]) = {
//              in.map(m => BigQueryRow(m, (m.name, m.count)))
//            }
//            val inArgs = PubSubArgs(topic = "p2pin")
//            val outArgs = BigQueryArgs(dataset = "dataset1", table = "table1")
//            val getBuilder = new ScioBuilderPubSubToBigQuery(transform, inArgs, outArgs)
//            val x = 1
//       """
//    assertEqualStructure(config, expectedBlock)
//
//  }
//
//  "SOTMacroBuilder service" should "build from json" in {
//    val config =  SOTMacroJsonConfig("ps2bq-test-join.json")
//
//    val expectedBlock =
//      q"""
//           `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"MessageExtended\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
//            class MessageExtended
//            def transform(in: SCollection[MessageExtended]) = {
//              in.map(m => BigTableRecord(m, ("cf", "name", m.name)))
//            }
//            val inArgs = PubSubArgs(topic = "p2pin")
//            val outArgs = BigTableArgs(instanceId = "biBigTablegtable-test", tableId = "bigquerytest", familyName = List("cf"), numNodes = 3)
//            val getBuilder = new ScioBuilderPubSubToBigTable(transform, inArgs, outArgs)
//            val x = 1
//          """
//    assertEqualStructure(config, expectedBlock)
//  }

//
//  "SOTMacroBuilder service" should "build pubsub to datastore macro" in {
//    val avroFields =
//      JsArray(JsObject(Map(
//        "name" -> JsString("user"),
//        "type" -> JsString("string")
//      )),
//        JsObject(Map(
//          "name" -> JsString("score"),
//          "type" -> JsString("in")
//        )))
//    val avroDef = AvroDefinition(`type` = "record", name = "MessageExtended", namespace = "parallelai.sot.avro", fields = avroFields)
//    val in = AvroSchema(`type` = "PubSub", id =  "inschema1", version = "", definition = avroDef)
//
//
//    val schemas = List(in)
//
//    val taps = List(
//      PubSubTapDefinition(`type` = "pubsub", id =  "insource", topic = "p2pin"),
//      DatastoreTapDefinition(`type` = "datastore", id =  "outsource", kind = "kind1")
//    )
//
//    val dag = List(
//      DAGMapping(from = "in", to = "mapper1"),
//      DAGMapping(from = "mapper1", to = "out")
//    )
//    val steps = List(
//      SourceOp(`type` = "source", name = "in", schema = "inschema1", tap = "insource"),
//      TransformationOp(`type` = "transformation", name = "mapper1", op = "map", func = "m => 'teamscores ->> m._1 :: 'score1 ->> m._2.toString :: 'score2 ->> (m._2 * 2) :: HNil"),
//      SinkOp(`type` = "sink", name = "out", schema = None, tap = "outsource")
//    )
//
//    val config = Config(name = "", version = "", schemas = schemas, taps = taps, dag = dag, steps = steps)
//
//    val expectedBlock =
//      q"""
//            `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"MessageExtended\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
//            class MessageExtended
//            def transform(in: SCollection[MessageExtended]) = {
//              in.map(m => 'teamscores ->> m._1 :: 'score1 ->> m._2.toString :: 'score2 ->> (m._2 * 2) :: HNil)
//            }
//            val inArgs = PubSubArgs(topic = "p2pin")
//            val outArgs = DatastoreArgs(kind = "kind1")
//            val getBuilder = new ScioBuilderPubSubToDatastore(transform, inArgs, outArgs)
//            val x = 1
//          """
//    assertEqualStructure(config, expectedBlock)
//  }
//
//  "SOTMacroBuilder service" should "build pubsub to datastore macro with schema defined" in {
//    val avroFields =
//      JsArray(JsObject(Map(
//        "name" -> JsString("user"),
//        "type" -> JsString("string")
//      )),
//        JsObject(Map(
//          "name" -> JsString("score"),
//          "type" -> JsString("in")
//        )))
//    val avroDef = AvroDefinition(`type` = "record", name = "MessageExtended", namespace = "parallelai.sot.avro", fields = avroFields)
//
//    val in = AvroSchema(`type` = "avro", id =  "inschema1", version = "", definition = avroDef)
//    val dsSchema = DatastoreDefinition(`type` = "datastoredefinition", name = "OutputSchema",
//      fields = List(
//        DatastoreDefinitionField(name = "key", `type` = "String"),
//        DatastoreDefinitionField(name = "value1", `type` = "String"),
//        DatastoreDefinitionField(name = "value2", `type` = "Double")))
//    val out = DatastoreSchema(`type` = "datastore", version = "", definition = dsSchema, id =  "outschema1")
//    val schemas = List(in, out)
//
//    val taps = List(
//      PubSubTapDefinition(`type` = "pubsub", id =  "insource", topic = "p2pin"),
//      DatastoreTapDefinition(`type` = "datastore", id =  "outsource", kind = "kind1")
//    )
//
//    val dag = List(
//      DAGMapping(from = "in", to = "mapper1"),
//      DAGMapping(from = "mapper1", to = "out")
//    )
//    val steps = List(
//      SourceOp(`type` = "source", name = "in", schema = "inschema1", tap = "insource"),
//      TransformationOp(`type` = "transformation", "mapper1", "map", "m => OutputSchema(m._1, m._2.toString,(m._2 * 2))"),
//      SinkOp(`type` = "sink", name = "out", schema = Some("outschema1"), tap = "outsource")
//    )
//
//    val config = Config(name = "", version = "", schemas = schemas, taps = taps, dag = dag, steps = steps)
//
//    val expectedBlock =
//      q"""
//         `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"MessageExtended\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
//         class MessageExtended
//         case class OutputSchema(key: String, value1: String, value2: Double)
//         def transform(in: SCollection[MessageExtended]) = {
//           in.map(m => OutputSchema(m._1, m._2.toString,(m._2 * 2)))
//         }
//         val inArgs = PubSubArgs(topic = "p2pin")
//         val outArgs = DatastoreArgs(kind = "kind1")
//         val getBuilder = new ScioBuilderPubSubToDatastoreWithSchema(transform, inArgs, outArgs)
//         val x = 1
//          """
//    assertEqualStructure(config, expectedBlock)
//  }
//
//  "SOTMacroBuilder service" should "build pubsub to pubsub macro" in {
//    val avroFieldsIn =
//      JsArray(JsObject(Map(
//        "name" -> JsString("user"),
//        "type" -> JsString("string")
//      )),
//        JsObject(Map(
//          "name" -> JsString("score"),
//          "type" -> JsString("in")
//        )))
//    val avroDefIn = AvroDefinition(`type` = "record", name = "Message", namespace = "parallelai.sot.avro", fields = avroFieldsIn)
//    val in = AvroSchema(`type` = "PubSub", id =  "inschema1", version = "", definition = avroDefIn)
//
//    val avroFieldsOut =
//      JsArray(JsObject(Map(
//        "name" -> JsString("user"),
//        "type" -> JsString("string")
//      )),
//        JsObject(Map(
//          "name" -> JsString("score"),
//          "type" -> JsString("in")
//        )))
//    val avroDefOut = AvroDefinition(`type` = "record", name = "MessageExtended", namespace = "parallelai.sot.avro", fields = avroFieldsOut)
//    val out = AvroSchema(`type` = "PubSub", id =  "outschema1", version = "", definition = avroDefOut)
//
//    val schemas = List(in, out)
//
//    val taps = List(
//      PubSubTapDefinition(`type` = "pubsub", id =  "source1", topic = "p2pin"),
//      PubSubTapDefinition(`type` = "pubsub", id =  "source2", topic = "p2pout")
//    )
//
//    val dag = List(
//      DAGMapping(from = "in", to = "mapper1"),
//      DAGMapping(from = "mapper1", to = "out")
//    )
//    val steps = List(
//      SourceOp(`type` = "source", name = "in", schema = "inschema1", tap = "source1"),
//      TransformationOp(`type` = "transformation", "mapper1", "map", "m => MessageExtended(m._1, m._2.toString,(m._2 * 2))"),
//      SinkOp(`type` = "sink", name = "out", schema = Some("outschema1"), tap = "source2")
//    )
//
//    val config = Config(name = "", version ="", schemas = schemas, taps = taps, dag = dag, steps = steps)
//
//    val expectedBlock =
//      q"""
//         `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"Message\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
//         class Message
//         `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"MessageExtended\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
//         class MessageExtended
//         def transform(in: SCollection[Message]) = {
//           in.map(m => MessageExtended(m._1, m._2.toString,(m._2 * 2)))
//         }
//         val inArgs = PubSubArgs(topic = "p2pin")
//         val outArgs = PubSubArgs(topic = "p2pout")
//         val getBuilder = new ScioBuilderPubSubToPubSub(transform, inArgs, outArgs)
//         val x = 1
//          """
//    assertEqualStructure(config, expectedBlock)
//  }
//
  def assertEqualStructure(config: Config, expectedBlock: Term.Block): Assertion = {
    val rs = q"object Test { object conf { }; val x =1 }" match {
      case q"object $name {object conf { ..$confStatements };  ..$statements}" =>
        SOTMainMacroImpl.expand(name, confStatements, statements, config)
    }

    val stats = rs.templ.stats.get
    val expected = Term.Block.unapply(expectedBlock).get
    for (i <- 1 until expected.length) {
      expected(i).structure should ===(stats(i).structure)
    }
    expected.structure should ===(stats.structure)
  }


}