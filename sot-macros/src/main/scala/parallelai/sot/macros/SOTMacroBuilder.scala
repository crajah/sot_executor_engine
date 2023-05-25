package parallelai.sot.macros

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.meta._
import parallelai.sot.engine.config.SchemaResourcePath
import parallelai.sot.engine.serialization.protobuf.ProtoPBCCodeGen
import parallelai.sot.executor.model.SOTMacroConfig.{Config, DAGMapping, _}
import parallelai.sot.executor.model.SOTMacroJsonConfig._
import parallelai.sot.executor.model._
import parallelai.sot.macros.SOTMacroHelper._
import spray.json._

class SOTBuilder extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    val config = SOTMacroJsonConfig(SchemaResourcePath().value)

    defn match {
      case q"object $name {object conf { ..$confStatements };  ..$statements}" =>
        SOTMainMacroImpl.expand(name, confStatements, statements, config)
      case _ =>
        abort("@main must annotate an object.")
    }
  }
}

object SOTMainMacroImpl {
  def expand(name: Term.Name, confStatements: Seq[Stat], statements: Seq[Stat], config: Config): Defn.Object = {

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

      case json: JSONSchema =>
        json.definition match {
          case jsond: JSONDefinition => Some(jsonSchemaCodeGeneration(jsond))
          case _ => throw new Exception("JSON does not support this definition")
        }

      case _ =>
        throw new Exception("Unsupported Schema Type")
    }.flatten

    val definitionsSchemasTypes = schemaTypeValDecl(config, dag)

    val transformations = transformationsCodeGenerator(config, dag)

    val allConfStatements = confStatements ++ definitionsSchemasTypes
    val configObject = Seq(q"object conf { ..$allConfStatements }")

    val syn = parsedSchemas ++ configObject ++ transformations ++ statements

    val code =
      q"""object $name {
        ..$syn
      }"""

    println(code)
    code
  }

  /**
    * Generates the code for the all the operations
    */
  def transformationsCodeGenerator(config: Config, dag: Topology[String, DAGMapping]): Seq[Defn.Class] = {
    val transformations = getMonadTransformations(config, dag, q"init[HNil]")

    Seq(
      q"""
       class Builder extends Serializable() {
         def execute(sotUtils: SOTUtils, sc: ScioContext, args: Args): Unit = {
           val job = ${transformations}

           job.run(HNil)._1
           val result = sc.close()
           if (args.getOrElse("waitToFinish", "true").toBoolean) sotUtils.waitToFinish(result.internal)
         }
       }
      """)
  }

  private def getMonadTransformations(config: Config, dag: Topology[String, DAGMapping], q: Term): Term = {

    val tsorted = dag.topologicalSort()._2
    val sinks = getSinks(config)
    val idsStack = mutable.Map[String, Int]()
    val srcSteps = getSources(config).zipWithIndex.map({
      case (srcOp,i) => {
        setNextStackId(idsStack, srcOp._1)
        (Term.Name("read"), List(List(Term.Name("sc"), buildTap(Term.Name("conf.sourceTaps"), srcOp._2, srcOp._3, i), Term.Name("sotUtils"))))
      }
    })

    val ops = tsorted.map({
      case (e1, e2) => {
        val op = getOp(e2, config.steps)
        val stepParsed =  op match {
          case _: TransformationOp => {
            setNextStackId(idsStack, e2)
            val opParsed = parseOperation(op, dag, config).get
            (opParsed._2, opParsed._3)
          }
          case _: TFPredictOp => {
            setNextStackId(idsStack, e2)
            val opParsed = parseOperation(op, dag, config).get
            (opParsed._2, opParsed._3)
          }
          case sinkOp: SinkOp => {
            val sinkDef = sinks.find(_._1 == sinkOp.id).get
            val writeMethod = if (sinkDef._2.isDefined) "write" else "writeSchemaless"
            (Term.Name(writeMethod), List(List(buildTap(Term.Name("conf.sinkTaps"), sinkDef._2, sinkDef._3, sinks.indexOf(sinkDef)), Term.Name("sotUtils"))))
          }
        }
        val inEdgeIndex = idsStack.get(e1)
        val idTerm = Term.Name("_" + inEdgeIndex.get)
        (stepParsed._1, List(List(q"sColls.at(Nat.${idTerm})")) ::: stepParsed._2)
      }
    }).toList

    parseStateMonadExpression(srcSteps ++ ops, q)
  }

  private def setNextStackId(idsStack: mutable.Map[String, Int], id: String) = {
    val values = idsStack.values
    val newId = if (values.isEmpty) 0 else values.max + 1
    idsStack.put(id, newId)
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
        case class $name ( ..$listSchema) extends parallelai.sot.engine.io.utils.annotations.HasDatastoreAnnotation
      """

    Seq(block)
  }

  def SOTFieldParser(fieldName: String, fieldType: String, fieldMode: String): Term.Param = {
    fieldMode match {
      case "nullable" => s"$fieldName: Option[$fieldType]".parse[Term.Param].get
      case "repeated" => s"$fieldName: List[$fieldType]".parse[Term.Param].get
      case "required" => s"$fieldName: $fieldType".parse[Term.Param].get
    }
  }

  def SOTCaseClassParser(fields: List[JSONDefinitionField], name: String): List[(String, Term.Param)] = {
    fields.flatMap {
      f =>
        f.`type` match {
          case "record" =>
            val newName = NameProvider.getUniqueName(name)
            (name, SOTFieldParser(f.name, newName, f.mode)) :: SOTCaseClassParser(f.fields.get, newName)
          case _ => List((name, SOTFieldParser(f.name, f.`type`, f.mode)))
        }
    }
  }

  def jsonSchemaCodeGeneration(definition: JSONDefinition): Seq[Defn.Class] = {
    val schemaFields = SOTCaseClassParser(definition.fields, definition.name)

    schemaFields.groupBy(_._1).map {
      case (key, value) =>
        val name = Type.Name(key)
        val listSchema = value.map(_._2)
        q"""
        case class $name ( ..$listSchema) extends parallelai.sot.engine.io.utils.annotations.HasJSONAnnotation
        """
    }.toList

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
    val generatedCode = ProtoPBCCodeGen.executeAll(definition.schemaBase64)
    val stats = generatedCode.parse[Source].get.stats

    stats.head match {
      case q"package $name  {..$statements}" =>
        Seq(
          q"""object gen { ..$statements}""",
          q"import SOTBuilder.gen._")
      case _ =>
        abort("@main must annotate an object.")
    }
  }

  def schemaTypeValDecl(config: Config, dag: Topology[String, DAGMapping]): Seq[Defn.Val] = {
    val sources = buildTaps(getSources(config), Term.Name("conf.sourceTaps"))
    val sinks = buildTaps(getSinks(config), Term.Name("conf.sinkTaps"))

    Seq(q"val sources = $sources",
        q"val sinks = $sinks"
    )
  }

  private def buildTaps(taps: List[(String, Option[Schema], TapDefinition)], term: Term.Name) = {
    val sinksDefs = taps.view.zipWithIndex.map({
      case ((_, sinkSchema, sinkTap), i) =>
        buildTap(term, sinkSchema, sinkTap, i)
    })
    val sinks = sinksDefs.tail.
      foldLeft(Term.ApplyInfix(sinksDefs.head, Term.Name("::"), List(), List(Term.Name("HNil"))))((cumul: Term.ApplyInfix, t: Term.Apply) => Term.ApplyInfix(t, Term.Name("::"), List(), List(cumul)))
    sinks
  }

  private def buildTap(term: Term.Name, sinkSchema: Option[Schema], sinkTap: TapDefinition, i: Int) = {
    val sinkConfigApply = if (sinkSchema.isDefined) {
      typedTap(sinkSchema, sinkTap)
    } else {
      schemalessTap(sinkSchema, sinkTap)
    }
    Term.Apply(sinkConfigApply, Seq(q"$term(${Lit.Int(i)})._3"))
  }

  private def schemalessTap(sinkSchema: Option[Schema], sinkTap: TapDefinition) = {
    Term.ApplyType(Term.Name("SchemalessTapDef"),
      List(Type.Name(getTapType(sinkTap)),
        Type.Name("parallelai.sot.engine.config.gcp.SOTUtils"),
        Type.Name(getSchemaAnnotation(sinkSchema))))
  }

  private def typedTap(maybeSchema: Option[Schema], tap: TapDefinition) = {
    Term.ApplyType(Term.Name("TapDef"),
      List(Type.Name(getTapType(tap)),
        Type.Name("parallelai.sot.engine.config.gcp.SOTUtils"),
        Type.Name(getSchemaAnnotation(maybeSchema)),
        Type.Name(maybeSchema.get.definition.name)))
  }

  def buildSchemaType(definitionName: String, annotation: String): Term.ApplyInfix =
    q"${Lit.String(definitionName)} -> ${Term.ApplyType(Term.Name("SchemaType"), List(Type.Name(annotation), Type.Name(definitionName)))}"

  def getSchemaAnnotation(schema: Option[Schema]): String = schema match {
    case Some(_: BigQuerySchema) => "com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation"
    case Some(_: AvroSchema) => "com.spotify.scio.avro.types.AvroType.HasAvroAnnotation"
    case Some(_: DatastoreSchema) => "parallelai.sot.engine.io.utils.annotations.HasDatastoreAnnotation"
    case Some(_: ProtobufSchema) => "com.trueaccord.scalapb.GeneratedMessage"
    case Some(_: JSONSchema) => "parallelai.sot.engine.io.utils.annotations.HasJSONAnnotation"
    case None => "parallelai.sot.engine.io.utils.annotations.Schemaless"
    case Some(s) => throw new Exception("Unsupported Schema Type " + s.`type`)
  }

  def getSchemaName(schema: Option[Schema]): String = schema match {
    case Some(s) => s.definition.name
    case None => "com.google.api.services.bigquery.model.TableRow"
  }

  private def getTapType(tapDefinition: TapDefinition) = tapDefinition.getClass.getCanonicalName

  private def getSchemaType(config: Config, sourceOp: SourceOp) =
    SOTMacroHelper.getSchema(sourceOp.schema, config.schemas).definition.name.parse[Type].get

  private def getSchemaType(config: Config, sinkOp: SinkOp) =
    SOTMacroHelper.getSchema(sinkOp.schema.get, config.schemas).definition.name.parse[Type].get
}