package parallelai.sot.macros

import java.io.File

import spray.json._
import parallelai.sot.executor.model.{SOTMacroJsonConfig, Topology}
import parallelai.sot.executor.model.SOTMacroConfig.{Source => SOTSource, _}
import com.typesafe.config.ConfigFactory
import parallelai.sot.executor.model.SOTMacroJsonConfig._

import scala.collection.immutable.Seq
import scala.meta._

class SOTBuilder extends scala.annotation.StaticAnnotation {

  inline def apply(defn: Any): Any = meta {

    defn match {
      case q"object $name { ..$stats }" =>
        val source = getClass.getResource("/application.conf").getPath
        val fileName = ConfigFactory.parseFile(new File(source)).getString("json.file.name")
        SOTMainMacroImpl.expand(name, stats, fileName)
      case _ =>
        abort("@main must annotate an object.")
    }
  }
}

object SOTMainMacroImpl {

  /**
    * Generates the code for the all the operations
    */

  def transformationsCodeGenerator(config: Config, dag: Topology[String, DAGMapping]): Seq[Defn.Def] = {

    //there should be only one source
    val sourceOperationName = dag.getSourceVertices().head
    val sourceOperation = SOTMacroHelper.getOp(sourceOperationName, config.steps) match {
      case s: SourceOp => s
      case _ => throw new Exception("Unsupported source operation")
    }

    val sourceOpCode = SOTMacroHelper.parseOperation(sourceOperation, dag, config)
    val ops = SOTMacroHelper.getOps(dag, config, sourceOperationName, List(sourceOpCode)).flatten

    val transformations = SOTMacroHelper.parseExpression(ops)

    val sourceSchema = SOTMacroHelper.getSchema(sourceOperation.schema, config.schemas)
    val sourceDefName = sourceSchema.definition
    val sourceTypeName = sourceDefName.name.parse[Type].get

    Seq(
      q"""
         def transform(in : SCollection[$sourceTypeName]) = {
           $transformations
         }
       """)
  }

  def expand(name: Term.Name, stats: Seq[Stat], fileName: String): Defn.Object = {
    val config = SOTMacroJsonConfig(fileName)
    val dag = SOTMacroHelper.validateDag(config)
    expand(name, stats, config, dag)
  }

  def expand(name: Term.Name, stats: Seq[Stat], config: Config, dag: Topology[String, DAGMapping]): Defn.Object = {

    val parsedSchemas = config.schemas.flatMap {
      case bq: BigQuerySchema =>
        bq.definition match {
          case bq: BigQueryDefinition => Some(bigQuerySchemaCodeGenerator(bq))
          case _ => throw new Exception("BigQuery does not support this definition")
        }
      case ps: AvroSchema =>
        ps.definition match {
          case av: AvroDefinition => Some(avroSchemaCodeGenerator(av))
          case _ => throw new Exception("Avro does not support this definition")
        }
      case ds: DatastoreSchema =>
        ds.definition match {
          case dsd: DatastoreDefinition => Some(datastoreSchemaCodeGeneration(dsd))
          case _ => throw new Exception("Datastore does not support this definition")
        }
      case _ => throw new Exception("Unsupported Type")
    }.flatten

    val transformations = transformationsCodeGenerator(config, dag)

    //TODO: rewrite this code once a generic solution is developed for the templates
    val source = dag.getSourceVertices().head
    val sourceOp = SOTMacroHelper.getOp(source, config.steps).asInstanceOf[SourceOp]

    val sink = dag.getSinkVertices().head
    val sinkOp = SOTMacroHelper.getOp(sink, config.steps).asInstanceOf[SinkOp]

    val builder = argsCodeGenerator(sourceOp, sinkOp, config)

    val syn = parsedSchemas ++ transformations ++ builder ++ stats

    val x =
      q"""object $name {
         ..$syn
         }"""
    println(x)
    x
  }

  /**
    * Generates the code block for
    * val inArgs = ???
    * val outArgs = ???
    * val getBuilder = ???
    *
    * @param in
    * @param out
    * @return
    */

  def argsCodeGenerator(in: SourceOp, out: SinkOp, config: Config): Seq[Defn] = {
    val sinkSource = SOTMacroHelper.getSource(in.source, config.sources)
    val sourceSource = SOTMacroHelper.getSource(out.source, config.sources)

    (sinkSource, sourceSource) match {
      case (i: PubSubSource, o: BigQuerySource) =>
        Seq(q"val inArgs = PubSubArgs(topic = ${Lit.String(i.topic)})",
          q"val outArgs = BigQueryArgs(dataset = ${Lit.String(o.dataset)}, table = ${Lit.String(o.table)})",
          q"val getBuilder = new ScioBuilderPubSubToBigQuery(transform, inArgs, outArgs)")
      case (i: PubSubSource, o: PubSubSource) =>
        Seq(q"val inArgs = PubSubArgs(topic = ${Lit.String(i.topic)})",
          q"val outArgs = PubSubArgs(topic = ${Lit.String(o.topic)})",
          q"val getBuilder = new ScioBuilderPubSubToPubSub(transform, inArgs, outArgs)")
      case (i: PubSubSource, o: BigTableSource) =>
        val cfValues = o.familyName.map(Lit.String(_))
        val cfList = Term.Apply(Term.Name("List"), cfValues)
        Seq(q"val inArgs = PubSubArgs(topic = ${Lit.String(i.topic)})",
          q"val outArgs = BigTableArgs(instanceId = ${Lit.String(o.instanceId)}, tableId = ${Lit.String(o.tableId)}, familyName = $cfList, numNodes = ${Lit.Int(o.numNodes)})",
          q"val getBuilder = new ScioBuilderPubSubToBigTable(transform, inArgs, outArgs)")
      case (i: PubSubSource, o: DatastoreSource) =>

        //check if there is any schema defined for out source
        val builder = out.schema match {
          case Some(_) => q"val getBuilder = new ScioBuilderPubSubToDatastoreWithSchema(transform, inArgs, outArgs)"
          case None => q"val getBuilder = new ScioBuilderPubSubToDatastore(transform, inArgs, outArgs)"
        }

        Seq(q"val inArgs = PubSubArgs(topic = ${Lit.String(i.topic)})",
          q"val outArgs = DatastoreArgs(kind = ${Lit.String(o.kind)})", builder)

      case _ => throw new Exception("Unsupported in/out combination")
    }
  }

  def bigQuerySchemaCodeGenerator(definition: BigQueryDefinition): Seq[Stat] = {
    val queryString = JsString(definition.toJson.compactPrint).compactPrint
    val query = queryString.parse[Term].get
    val className = Type.Name(definition.name)

    val block =
      q"""
           `@BigQueryType`.fromSchema($query)
           class $className
         """
    Term.Block.unapply(block).get
  }

  def datastoreSchemaCodeGeneration(definition: DatastoreDefinition): Seq[Stat] = {
    val listSchema = definition.fields.map { sc =>
      s"${sc.name}: ${sc.`type`}".parse[Term.Param].get
    }
    val name = Type.Name(definition.name)
    val block =
      q"""
        case class $name ( ..$listSchema)
        """
    Seq(block)
  }

  def avroSchemaCodeGenerator(definition: AvroDefinition): Seq[Stat] = {
    val queryString = JsString(definition.toJson.compactPrint).compactPrint
    val query = queryString.parse[Term].get
    val className = Type.Name(definition.name)

    val block =
      q"""
           `@AvroType`.fromSchema($query)
           class $className
         """
    Term.Block.unapply(block).get
  }

}