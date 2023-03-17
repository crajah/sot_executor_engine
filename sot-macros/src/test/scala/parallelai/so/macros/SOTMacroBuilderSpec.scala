package parallelai.so.macros

import org.scalatest.compatible.Assertion
import org.scalatest.{FlatSpec, Matchers}
import parallelai.sot.executor.model.SOTMacroConfig
import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.macros.SOTMainMacroImpl
import spray.json.{JsArray, JsObject, JsString}

import scala.meta.{Term, _}

class SOTMacroBuilderSpec extends FlatSpec with Matchers {

  "SOTMacroBuilder service" should "build pubsub to bigquery macro" in {
    val avroFields =
      JsArray(JsObject(Map(
        "name" -> JsString("user"),
        "type" -> JsString("string")
      )),
        JsObject(Map(
          "name" -> JsString("score"),
          "type" -> JsString("in")
        )))

    val avroDef = AvroDefinition(`type` = "record", name = "MessageExtended", namespace = "parallelai.sot.avro", fields = avroFields)
    val in = AvroSchema(`type` = "avro", name = "inschema1", version = "", definition = avroDef)

    val bqFields = JsArray(JsObject(Map(
      "mode" -> JsString("REQUIRED"),
      "name" -> JsString("user"),
      "type" -> JsString("STRING")
    )),
      JsObject(Map(
        "mode" -> JsString("REQUIRED"),
        "name" -> JsString("total_score"),
        "type" -> JsString("INTEGER")
      )))

    val bqDefinition = BigQueryDefinition(`type` = "bigquerydefinition", name = "BigQueryRow", fields = bqFields)
    val out = BigQuerySchema(`type` = "bigquery", name = "outschema1", version = "", definition = bqDefinition)

    val schemas = List(in, out)

    val sources = List(
      PubSubSource(`type` = "pubsub", name = "insource", topic = "p2pin"),
      BigQuerySource(`type` = "bigquery", name = "outsource", dataset = "dataset1", table = "table1")
    )

    val dag = List(
      DAGMapping(from = "in", to = "mapper1"),
      DAGMapping(from = "mapper1", to = "out")
    )

    val steps = List(
      SourceOp(`type` = "source", name = "in", schema = "inschema1", source = "insource"),
      TransformationOp(`type` = "transformation", name = "mapper1", op = "map", func = "m => BigQueryRow(m, (m.name, m.count))"),
      SinkOp(`type` = "sink", name = "out", schema = Some("outschema1"), source = "outsource")
    )

    val config = Config(name = "", version = "", schemas = schemas, sources = sources, dag = dag, steps = steps)

    val expectedBlock =
      q"""
            `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"MessageExtended\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
            class MessageExtended
            `@BigQueryType`.fromSchema("{\"type\":\"bigquerydefinition\",\"name\":\"BigQueryRow\",\"fields\":[{\"mode\":\"REQUIRED\",\"name\":\"user\",\"type\":\"STRING\"},{\"mode\":\"REQUIRED\",\"name\":\"total_score\",\"type\":\"INTEGER\"}]}")
            class BigQueryRow
            def transform(in: SCollection[MessageExtended]) = {
              in.map(m => BigQueryRow(m, (m.name, m.count)))
            }
            val inArgs = PubSubArgs(topic = "p2pin")
            val outArgs = BigQueryArgs(dataset = "dataset1", table = "table1")
            val getBuilder = new ScioBuilderPubSubToBigQuery(transform, inArgs, outArgs)
            val x = 1
       """
    assertEqualStructure(config, expectedBlock)

  }

  "SOTMacroBuilder service" should "build pubsub to bigtable macro" in {
    val avroFields =
      JsArray(JsObject(Map(
        "name" -> JsString("user"),
        "type" -> JsString("string")
      )),
        JsObject(Map(
          "name" -> JsString("score"),
          "type" -> JsString("in")
        )))

    val avroDef = AvroDefinition(`type` = "record", name = "MessageExtended", namespace = "parallelai.sot.avro", fields = avroFields)
    val in = AvroSchema(`type` = "avro", name = "inschema1", version = "", definition = avroDef)

    val schemas = List(in)

    val sources = List(
      PubSubSource(`type` = "pubsub", name = "insource", topic = "p2pin"),
      BigTableSource(`type` = "bigtable", name = "outsource", instanceId = "biBigTablegtable-test", tableId = "bigquerytest", familyName = List("cf"), numNodes = 3)
    )

    val dag = List(
      DAGMapping(from = "in", to = "mapper1"),
      DAGMapping(from = "mapper1", to = "out")
    )
    val steps = List(
      SourceOp(`type` = "source", name = "in", schema = "inschema1", source = "insource"),
      TransformationOp(`type` = "transformation", name = "mapper1", op = "map", func = "m => BigTableRecord(m, (\"cf\", \"name\", m.name))"),
      SinkOp(`type` = "sink", name = "out", schema = Some("outschema1"), source = "outsource")
    )

    val config = Config(name = "", version = "", schemas = schemas, sources = sources, dag = dag, steps = steps)

    val expectedBlock =
      q"""
           `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"MessageExtended\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
            class MessageExtended
            def transform(in: SCollection[MessageExtended]) = {
              in.map(m => BigTableRecord(m, ("cf", "name", m.name)))
            }
            val inArgs = PubSubArgs(topic = "p2pin")
            val outArgs = BigTableArgs(instanceId = "biBigTablegtable-test", tableId = "bigquerytest", familyName = List("cf"), numNodes = 3)
            val getBuilder = new ScioBuilderPubSubToBigTable(transform, inArgs, outArgs)
            val x = 1
          """
    assertEqualStructure(config, expectedBlock)
  }

  "SOTMacroBuilder service" should "build pubsub to datastore macro" in {
    val avroFields =
      JsArray(JsObject(Map(
        "name" -> JsString("user"),
        "type" -> JsString("string")
      )),
        JsObject(Map(
          "name" -> JsString("score"),
          "type" -> JsString("in")
        )))
    val avroDef = AvroDefinition(`type` = "record", name = "MessageExtended", namespace = "parallelai.sot.avro", fields = avroFields)
    val in = AvroSchema(`type` = "PubSub", name = "inschema1", version = "", definition = avroDef)


    val schemas = List(in)

    val sources = List(
      PubSubSource(`type` = "pubsub", name = "insource", topic = "p2pin"),
      DatastoreSource(`type` = "datastore", name = "outsource", kind = "kind1")
    )

    val dag = List(
      DAGMapping(from = "in", to = "mapper1"),
      DAGMapping(from = "mapper1", to = "out")
    )
    val steps = List(
      SourceOp(`type` = "source", name = "in", schema = "inschema1", source = "insource"),
      TransformationOp(`type` = "transformation", name = "mapper1", op = "map", func = "m => 'teamscores ->> m._1 :: 'score1 ->> m._2.toString :: 'score2 ->> (m._2 * 2) :: HNil"),
      SinkOp(`type` = "sink", name = "out", schema = None, source = "outsource")
    )

    val config = Config(name = "", version = "", schemas = schemas, sources = sources, dag = dag, steps = steps)

    val expectedBlock =
      q"""
            `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"MessageExtended\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
            class MessageExtended
            def transform(in: SCollection[MessageExtended]) = {
              in.map(m => 'teamscores ->> m._1 :: 'score1 ->> m._2.toString :: 'score2 ->> (m._2 * 2) :: HNil)
            }
            val inArgs = PubSubArgs(topic = "p2pin")
            val outArgs = DatastoreArgs(kind = "kind1")
            val getBuilder = new ScioBuilderPubSubToDatastore(transform, inArgs, outArgs)
            val x = 1
          """
    assertEqualStructure(config, expectedBlock)
  }

  "SOTMacroBuilder service" should "build pubsub to datastore macro with schema defined" in {
    val avroFields =
      JsArray(JsObject(Map(
        "name" -> JsString("user"),
        "type" -> JsString("string")
      )),
        JsObject(Map(
          "name" -> JsString("score"),
          "type" -> JsString("in")
        )))
    val avroDef = AvroDefinition(`type` = "record", name = "MessageExtended", namespace = "parallelai.sot.avro", fields = avroFields)

    val in = AvroSchema(`type` = "avro", name = "inschema1", version = "", definition = avroDef)
    val dsSchema = DatastoreDefinition(`type` = "datastoredefinition", name = "OutputSchema",
      fields = List(
        DatastoreDefinitionField(name = "key", `type` = "String"),
        DatastoreDefinitionField(name = "value1", `type` = "String"),
        DatastoreDefinitionField(name = "value2", `type` = "Double")))
    val out = DatastoreSchema(`type` = "datastore", version = "", definition = dsSchema, name = "outschema1")
    val schemas = List(in, out)

    val sources = List(
      PubSubSource(`type` = "pubsub", name = "insource", topic = "p2pin"),
      DatastoreSource(`type` = "datastore", name = "outsource", kind = "kind1")
    )

    val dag = List(
      DAGMapping(from = "in", to = "mapper1"),
      DAGMapping(from = "mapper1", to = "out")
    )
    val steps = List(
      SourceOp(`type` = "source", name = "in", schema = "inschema1", source = "insource"),
      TransformationOp(`type` = "transformation", "mapper1", "map", "m => OutputSchema(m._1, m._2.toString,(m._2 * 2))"),
      SinkOp(`type` = "sink", name = "out", schema = Some("outschema1"), source = "outsource")
    )

    val config = Config(name = "", version = "", schemas = schemas, sources = sources, dag = dag, steps = steps)

    val expectedBlock =
      q"""
         `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"MessageExtended\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
         class MessageExtended
         case class OutputSchema(key: String, value1: String, value2: Double)
         def transform(in: SCollection[MessageExtended]) = {
           in.map(m => OutputSchema(m._1, m._2.toString,(m._2 * 2)))
         }
         val inArgs = PubSubArgs(topic = "p2pin")
         val outArgs = DatastoreArgs(kind = "kind1")
         val getBuilder = new ScioBuilderPubSubToDatastoreWithSchema(transform, inArgs, outArgs)
         val x = 1
          """
    assertEqualStructure(config, expectedBlock)
  }

  "SOTMacroBuilder service" should "build pubsub to pubsub macro" in {
    val avroFieldsIn =
      JsArray(JsObject(Map(
        "name" -> JsString("user"),
        "type" -> JsString("string")
      )),
        JsObject(Map(
          "name" -> JsString("score"),
          "type" -> JsString("in")
        )))
    val avroDefIn = AvroDefinition(`type` = "record", name = "Message", namespace = "parallelai.sot.avro", fields = avroFieldsIn)
    val in = AvroSchema(`type` = "PubSub", name = "inschema1", version = "", definition = avroDefIn)

    val avroFieldsOut =
      JsArray(JsObject(Map(
        "name" -> JsString("user"),
        "type" -> JsString("string")
      )),
        JsObject(Map(
          "name" -> JsString("score"),
          "type" -> JsString("in")
        )))
    val avroDefOut = AvroDefinition(`type` = "record", name = "MessageExtended", namespace = "parallelai.sot.avro", fields = avroFieldsOut)
    val out = AvroSchema(`type` = "PubSub", name = "outschema1", version = "", definition = avroDefOut)

    val schemas = List(in, out)

    val sources = List(
      PubSubSource(`type` = "pubsub", name = "source1", topic = "p2pin"),
      PubSubSource(`type` = "pubsub", name = "source2", topic = "p2pout")
    )

    val dag = List(
      DAGMapping(from = "in", to = "mapper1"),
      DAGMapping(from = "mapper1", to = "out")
    )
    val steps = List(
      SourceOp(`type` = "source", name = "in", schema = "inschema1", source = "source1"),
      TransformationOp(`type` = "transformation", "mapper1", "map", "m => MessageExtended(m._1, m._2.toString,(m._2 * 2))"),
      SinkOp(`type` = "sink", name = "out", schema = Some("outschema1"), source = "source2")
    )

    val config = Config(name = "", version ="", schemas = schemas, sources = sources, dag = dag, steps = steps)

    val expectedBlock =
      q"""
         `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"Message\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
         class Message
         `@AvroType`.fromSchema("{\"type\":\"record\",\"name\":\"MessageExtended\",\"namespace\":\"parallelai.sot.avro\",\"fields\":[{\"name\":\"user\",\"type\":\"string\"},{\"name\":\"score\",\"type\":\"in\"}]}")
         class MessageExtended
         def transform(in: SCollection[Message]) = {
           in.map(m => MessageExtended(m._1, m._2.toString,(m._2 * 2)))
         }
         val inArgs = PubSubArgs(topic = "p2pin")
         val outArgs = PubSubArgs(topic = "p2pout")
         val getBuilder = new ScioBuilderPubSubToPubSub(transform, inArgs, outArgs)
         val x = 1
          """
    assertEqualStructure(config, expectedBlock)
  }

  def assertEqualStructure(config: Config, expectedBlock: Term.Block): Assertion = {
    val rs = q"object Test { val x =1 }" match {
      case q"object $name { ..$stats }" =>
        val dag = config.parseDAG()
        SOTMainMacroImpl.expand(name, stats, config, dag)
    }

    val stats = rs.templ.stats.get
    val expected = Term.Block.unapply(expectedBlock).get
    for (i <- 1 until expected.length) {
      expected(i).structure should ===(stats(i).structure)
    }
    expected.structure should ===(stats.structure)
  }


}