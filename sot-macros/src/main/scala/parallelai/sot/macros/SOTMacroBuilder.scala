package parallelai.sot.macros

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.util.Base64

import com.typesafe.config.ConfigFactory
import parallelai.sot.executor.model.SOTMacroConfig.{Config, DAGMapping, _}
import parallelai.sot.executor.model.SOTMacroJsonConfig._
import parallelai.sot.executor.model._
import parallelai.sot.macros.SOTMacroHelper._
import spray.json._

import scala.collection.immutable.Seq
import scala.meta._
import org.apache.commons.io.FileUtils


class SOTBuilder(resourceName: String) extends scala.annotation.StaticAnnotation {

  inline def apply(defn: Any): Any = meta {

    val resourcePath = this match {
      case q"new $_(${Lit.String(resourceName)})" => getClass.getResource("/" + resourceName).getPath
      case _ => abort("Config path parameter should be a string.")
    }

    val filePath = ConfigFactory.parseFile(new File(resourcePath)).getString("json.file.name")
    val config = SOTMacroJsonConfig(filePath)

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
      case ps: ProtobufSchema =>
        ps.definition match {
          case av: ProtobufDefinition => Some(protoSchemaCodeGenerator(av))
          case _ => throw new Exception("Protobuf does not support this definition")
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
    val sourceDefName = sourceSchema.get.definition
    val sourceTypeName = sourceDefName.name.parse[Type].get

    val sinkSchema = getSink(config)._1

    if (sinkSchema.isDefined) {
      val sinkDefName = sinkSchema.get.definition
      val sinkTypeName = sinkDefName.name.parse[Type].get

      val transformations = geTransformations(config, dag)

      val defTransformations = q"val trans = $transformations"

      Seq(
        q"""
         implicit def genericTransformation:Transformer[$sourceTypeName, $sinkTypeName] = new Transformer[$sourceTypeName, $sinkTypeName] {
           import shapeless.record._
           type Out = (Option[Nothing], SCollection[com.google.api.services.bigquery.model.TableSchema])
           def transform(rowIn: SCollection[$sourceTypeName]): SCollection[$sinkTypeName] = {
                   val converter = Row.to[$sinkTypeName]
                   val in = rowIn.map(r => Row(r))
                   $defTransformations
                   (None, trans.map(r => converter.from(r.hl)))
           }
         }
       """)
    } else {
      val transformations = geTransformations(config, dag)

      val defTransformations = q"val trans = $transformations"

      Seq(
        q"""
         implicit def genericTransformation:Transformer[$sourceTypeName, com.google.api.services.bigquery.model.TableRow, com.google.api.services.bigquery.model.TableSchema] = new Transformer[$sourceTypeName, com.google.api.services.bigquery.model.TableRow, com.google.api.services.bigquery.model.TableSchema] {
           import shapeless.record._
           type Out = (Option[com.google.api.services.bigquery.model.TableSchema], SCollection[com.google.api.services.bigquery.model.TableRow])
           def transform(rowIn: SCollection[$sourceTypeName]): Out = {
                   def getSchema[A <: HList](a: SCollection[Row[A]])(implicit hListSchemaProvider: HListSchemaProvider[A]) = BigQuerySchemaProvider[A].getSchema
                   val in = rowIn.map(r => Row(r))
                   $defTransformations
                   (Some(getSchema(trans)), trans.map(m => m.hl.toTableRow))
           }
         }
       """)
    }
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

  def protoSchemaCodeGenerator(definition: ProtobufDefinition): Seq[Stat] = {
    val generatedCode = ProtoPBCCodGen.executeAll(definition.schemaBase64)
    val stats = generatedCode.parse[Source].get.stats
    stats.head match {
      case q"package $name  {..$statements}" =>
        Seq(
          q"""
           object gen { ..$statements}
         """,
          q"import SOTBuilder.gen._")
      case _ =>
        abort("@main must annotate an object.")
    }
  }

  def schemaTypeValDecl(config: Config, dag: Topology[String, DAGMapping]) = {
    val (sourceSchema, sourceTap) = getSource(config)
    val (sinkSchema, sinkTap) = getSink(config)

    val args = List(sourceSchema, sinkSchema).map{
      schema => (getSchemaName(schema), getSchemaAnnotation(schema))
    }.map {
        case (definitionName, annotation) => Term.ApplyType(Term.Name("SchemaType"), List(Type.Name(annotation), Type.Name(definitionName)))
      }

    val configApply = Term.Apply(Term.ApplyType(Term.Name("RunnerConfig"),
      List(Type.Name(getTapType(sourceTap)),
        Type.Name("GcpOptions"),
        Type.Name(getSchemaAnnotation(sourceSchema)),
        Type.Name(sourceSchema.get.definition.name),
        Type.Name(getSchemaAnnotation(sinkSchema)),
        Type.Name(getSchemaName(sinkSchema)),
        Type.Name(getTapType(sinkTap)))), args)
    val schemaMapName = Pat.Var.Term(Term.Name("inOutSchemaHList"))
    Seq(q" val ${schemaMapName} = ${configApply}::HNil")
  }

  def buildSchemaType(definitionName: String, annotation: String): Term.ApplyInfix = {
    q"${Lit.String(definitionName)} -> ${Term.ApplyType(Term.Name("SchemaType"), List(Type.Name(annotation), Type.Name(definitionName)))}"
  }

  def getSchemaAnnotation(schema: Option[Schema]) = schema match {
    case Some(s) if s.`type` == "bigquery" => "com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation"
    case Some(s) if s.`type` == "avro" => "com.spotify.scio.avro.types.AvroType.HasAvroAnnotation"
    case Some(s) if s.`type` == "datastore" => "parallelai.sot.macros.HasDatastoreAnnotation"
    case Some(s) if s.`type` == "protobuf" => "com.trueaccord.scalapb.GeneratedMessage"
    case None => "com.google.api.client.json.GenericJson"
    case Some(s) => throw new Exception("Unsupported Schema Type " + s.`type`)
  }

  def getSchemaName(schema: Option[Schema]) = schema match {
    case Some(s) => s.definition.name
    case None => "com.google.api.services.bigquery.model.TableRow"
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