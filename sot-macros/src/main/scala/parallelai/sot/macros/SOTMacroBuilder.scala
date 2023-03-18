package parallelai.sot.macros

import java.io.File

import spray.json._
import parallelai.sot.executor.model.{SOTMacroJsonConfig, Topology}
import com.typesafe.config.ConfigFactory
import parallelai.sot.executor.model.SOTMacroConfig.{Config, DAGMapping, _}
import parallelai.sot.executor.model.SOTMacroJsonConfig._
import parallelai.sot.macros.SOTMainMacroImpl.getSchemaType

import scala.collection.immutable.Seq
import scala.meta._

class SOTBuilder extends scala.annotation.StaticAnnotation with EngineConfig {

  inline def apply(defn: Any): Any = meta {
    val source = getClass.getResource("/application.conf").getPath
    val fileName = ConfigFactory.parseFile(new File(source)).getString("json.file.name")
    val config = SOTMacroJsonConfig(fileName)

    defn match {
      case q"object $name { ..$statements }" =>
        SOTMainMacroImpl.expand(name, statements, config)
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

  def expand(name: Term.Name, statements: Seq[Stat], config: Config): Defn.Object = {
    val dag = config.parseDAG()
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
      case _ => throw new Exception("Unsupported Schema Type")
    }.flatten

    val transformations = transformationsCodeGenerator(config, dag)

    val source = dag.getSourceVertices().head
    val sourceOp = SOTMacroHelper.getOp(source, config.steps).asInstanceOf[SourceOp]

    val sink = dag.getSinkVertices().head
    val sinkOp = SOTMacroHelper.getOp(sink, config.steps).asInstanceOf[SinkOp]

    val builder = argsCodeGenerator(sourceOp, sinkOp, config)

    val typeInGen = Seq(
      q"""
         type In = ${getSchemaType(config, sourceOp)}
       """
    )

    val typeOutGen = Seq(
      q"""
         type Out = ${getSchemaType(config, sinkOp)}
       """
    )


    val readInputGen =
      Seq(
        q"""
         def readInput[In <: HasAvroAnnotation : Manifest](sc: ScioContext) = {
           sc.typedPubSub[In]("bi-crm-poc", "p2pin")
         }
       """)

    val writeOutputGen =
      Seq(
        q"""
         def writeOutput[Out <: HasAnnotation : Manifest](sCollection: SCollection[Out]) = {
           sCollection.saveAsTypedBigQuery("bigquerytest.testtable")
         }
       """)


    val syn = parsedSchemas ++ typeInGen ++ typeOutGen ++ transformations ++ builder ++ statements ++ readInputGen ++ writeOutputGen

    val x =
      q"""object $name {
         ..$syn
         }"""
    println(x)
    x
  }


  private def getSchemaType(config: Config, sourceOp: SourceOp) = {
    SOTMacroHelper.getSchema(sourceOp.schema, config.schemas).definition.name.parse[Type].get
  }
  private def getSchemaType(config: Config, sinkOp: SinkOp) = {
    SOTMacroHelper.getSchema(sinkOp.schema.get, config.schemas).definition.name.parse[Type].get
  }

  trait SourceCodeGenerator[A, C] {
    def generate(config: A, options: C): Seq[Defn]
  }

//  object SourceCodeGenerator {
//
//    def apply[A <: TapDefinition, C](implicit inputGenerator: SourceCodeGenerator[A, C]) = inputGenerator
//
//    def instance[A <: TapDefinition, C](func: (A, C) => Seq[Defn]): SourceCodeGenerator[A, C] =
//      new SourceCodeGenerator[A, C] {
//        def generate(config: A, options: C): Seq[Defn] =
//          func(config)
//      }
//
//    implicit def pubSubSource = instance[PubSubTapDefinition, ](pubSubTap =>
//      Seq(
//      q"""
//         def readInput[In <: HasAvroAnnotation : Manifest](sc: ScioContext) = {
//           sc.typedPubSub[In](${Lit.String(pubSubTap.topic)}, ${Lit.String(pubSubTap.topic)})
//         }
//       """))
//  }

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
    val sinkSource = SOTMacroHelper.getTap(in.tap, config.taps)
    val sourceSource = SOTMacroHelper.getTap(out.tap, config.taps)

    (sinkSource, sourceSource) match {
      case (i: PubSubTapDefinition, o: BigQueryTapDefinition) =>
        Seq(q"val inArgs = PubSubArgs(topic = ${Lit.String(i.topic)})",
          q"val outArgs = BigQueryArgs(dataset = ${Lit.String(o.dataset)}, table = ${Lit.String(o.table)})")
//    ,
//          q"val getBuilder = new ScioBuilderPubSubToBigQuery(transform, inArgs, outArgs)")
      case (i: PubSubTapDefinition, o: PubSubTapDefinition) =>
        Seq(q"val inArgs = PubSubArgs(topic = ${Lit.String(i.topic)})",
          q"val outArgs = PubSubArgs(topic = ${Lit.String(o.topic)})",
          q"val getBuilder = new ScioBuilderPubSubToPubSub(transform, inArgs, outArgs)")
      case (i: PubSubTapDefinition, o: BigTableTapDefinition) =>
        val cfValues = o.familyName.map(Lit.String(_))
        val cfList = Term.Apply(Term.Name("List"), cfValues)
        Seq(q"val inArgs = PubSubArgs(topic = ${Lit.String(i.topic)})",
          q"val outArgs = BigTableArgs(instanceId = ${Lit.String(o.instanceId)}, tableId = ${Lit.String(o.tableId)}, familyName = $cfList, numNodes = ${Lit.Int(o.numNodes)})",
          q"val getBuilder = new ScioBuilderPubSubToBigTable(transform, inArgs, outArgs)")
      case (i: PubSubTapDefinition, o: DatastoreTapDefinition) =>

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