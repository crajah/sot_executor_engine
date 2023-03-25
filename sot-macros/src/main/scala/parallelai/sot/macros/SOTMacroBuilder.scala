package parallelai.sot.macros

import java.io.File

import com.typesafe.config.ConfigFactory
import parallelai.sot.executor.model.SOTMacroConfig.{Config, DAGMapping, _}
import parallelai.sot.executor.model.SOTMacroJsonConfig._
import parallelai.sot.executor.model.{SOTMacroJsonConfig, Topology}
import parallelai.sot.macros.SOTMacroHelper._
import spray.json._

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

    val definitionsSchemasTypes = schemaTypeValDecl(config, dag)

    val transformations = transformationsCodeGenerator(config, dag)

    val source = dag.getSourceVertices().head
    val sourceOp = SOTMacroHelper.getOp(source, config.steps).asInstanceOf[SourceOp]

    val sink = dag.getSinkVertices().head
    val sinkOp = SOTMacroHelper.getOp(sink, config.steps).asInstanceOf[SinkOp]

    val syn = parsedSchemas ++ definitionsSchemasTypes ++ transformations ++ statements

    val x =
      q"""object $name {
         ..$syn
         }"""
    println(x)
    x
  }

  /**
    * Generates the code for the all the operations
    */

  def transformationsCodeGenerator(config: Config, dag: Topology[String, DAGMapping]): Seq[Defn.Def] = {

    //there should be only one source
    val sourceSchema = getSource(config)._1
    val sourceDefName = sourceSchema.definition
    val sourceTypeName = sourceDefName.name.parse[Type].get
    val sourceAnnotation = getSchemaAnnotation(sourceSchema).parse[Type].get


    val sinkSchema = getSink(config)._1
    val sinkDefName = sinkSchema.definition
    val sinkTypeName = sinkDefName.name.parse[Type].get
    val sinkAnnotation = getSchemaAnnotation(sinkSchema).parse[Type].get

    val transformations = geTransformations(config, dag)

    val defTransformations = q"val trans = $transformations"

    Seq(
      q"""
         implicit def genericTransformation:Transformer[$sourceAnnotation, $sourceTypeName, $sinkAnnotation, $sinkTypeName] = new Transformer[$sourceAnnotation, $sourceTypeName, $sinkAnnotation, $sinkTypeName] {
           def transform(rowIn: SCollection[$sourceTypeName]): SCollection[$sinkTypeName] = {
                   val in = rowIn.map(r => Row(r))
                   $defTransformations
                   trans.map(r => r.to[$sinkTypeName])
           }
         }
       """)
  }

  private def geTransformations(config: Config, dag: Topology[String, DAGMapping]): Term = {
    val sourceOperationName = dag.getSourceVertices().head
    val sourceOperation = SOTMacroHelper.getOp(sourceOperationName, config.steps) match {
      case s: SourceOp => s
      case _ => throw new Exception("Unsupported source operation")
    }

    val sourceOpCode = SOTMacroHelper.parseOperation(sourceOperation, dag, config)
    val ops = SOTMacroHelper.getOps(dag, config, sourceOperationName, List(sourceOpCode)).flatten

    SOTMacroHelper.parseExpression(ops)
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
        case class $name ( ..$listSchema) extends HasDatastoreAnnotation
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

  def schemaTypeValDecl(config: Config, dag: Topology[String, DAGMapping]) = {
    val (sourceSchema, sourceTap) = getSource(config)
    val (sinkSchema, sinkTap) = getSink(config)

    val args = List(sourceSchema, sinkSchema).map(sch => (sch.definition.name, getSchemaAnnotation(sch)))
      .map {
        case (definitionName, annotation) => Term.ApplyType(Term.Name("SchemaType"), List(Type.Name(annotation), Type.Name(definitionName)))
      }
    val configApply = Term.Apply(Term.ApplyType(Term.Name("RunnerConfig"),
      List(Type.Name(getTapType(sourceTap)),
        Type.Name("GcpOptions"),
        Type.Name(getSchemaAnnotation(sourceSchema)),
        Type.Name(sourceSchema.definition.name),
        Type.Name(getSchemaAnnotation(sinkSchema)),
        Type.Name(sinkSchema.definition.name),
        Type.Name(getTapType(sinkTap)))), args)
    val schemaMapName = Pat.Var.Term(Term.Name("inOutSchemaHList"))
    Seq(q" val ${schemaMapName} = ${configApply}::HNil")
  }

  def buildSchemaType(definitionName: String, annotation: String): Term.ApplyInfix = {
    q"${Lit.String(definitionName)} -> ${Term.ApplyType(Term.Name("SchemaType"), List(Type.Name(annotation), Type.Name(definitionName)))}"
  }

  def getSchemaAnnotation(schema: Schema) = schema.`type` match {
    case "bigquery" => "com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation"
    case "avro" => "com.spotify.scio.avro.types.AvroType.HasAvroAnnotation"
    case "datastore" => "parallelai.sot.macros.HasDatastoreAnnotation"
    case _ => throw new Exception("Unsupported Schema Type " + schema.`type`)
  }

  private def getTapType(tapDefinition: TapDefinition) = tapDefinition.getClass.getCanonicalName

  private def getSchemaType(config: Config, sourceOp: SourceOp) = {
    SOTMacroHelper.getSchema(sourceOp.schema, config.schemas).definition.name.parse[Type].get
  }

  private def getSchemaType(config: Config, sinkOp: SinkOp) = {
    SOTMacroHelper.getSchema(sinkOp.schema.get, config.schemas).definition.name.parse[Type].get
  }
}

trait HasDatastoreAnnotation