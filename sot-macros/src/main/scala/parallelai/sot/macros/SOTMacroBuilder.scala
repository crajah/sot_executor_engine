package parallelai.sot.macros

import cats.Applicative

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.meta._
import spray.json._
import parallelai.sot.engine.config.SchemaResourcePath
import parallelai.sot.engine.projectId
import parallelai.sot.engine.serialization.protobuf.ProtoPBCCodeGen
import parallelai.sot.executor.model.SOTMacroConfig.{Config, DAGMapping, _}
import parallelai.sot.executor.model.SOTMacroJsonConfig._
import parallelai.sot.executor.model._
import parallelai.sot.macros.SOTMacroHelper._

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

    val job = jobCodeGenerator(config, dag)

    val allConfStatements = confStatements ++ definitionsSchemasTypes
    val configObject = Seq(q"object conf { ..$allConfStatements }")

    val syn = parsedSchemas ++ configObject ++ job ++ statements

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
  def jobCodeGenerator(config: Config, dag: Topology[String, DAGMapping]): Seq[Stat] = {
    val transformations: Term = getMonadTransformations(config, dag, q"init[HNil]")

    lookups(config) ++ Seq(
      q"""
        class Job extends Serializable {
          def execute(sotUtils: SOTUtils, sc: ScioContext, args: Args): Unit = {
            val job = $transformations

            job.run(HNil)._1
            val result = sc.close()
            if (args.getOrElse("waitToFinish", "true").toBoolean) sotUtils.waitToFinish(result.internal)
          }
        }
      """
    )
  }

  private def datastores(config: Config): Seq[Stat] = config.taps.collect {
    case DatastoreTapDefinition(_, id, kind, _, _) =>
      s"""val $id = Datastore(Project("$projectId"), Kind("$kind"))""".parse[Stat].get
  }

  private def lookups(config: Config): Seq[Stat] = config.lookups.flatMap {
    case DatastoreLookupDefinition(id,schema,linkedTapId) => {
      config.taps.collectFirst {
        case DatastoreTapDefinition(_, tapId, kind, _, _) if tapId == linkedTapId =>
          s"""Datastore(Project("$projectId"), Kind("$kind"))"""
      }.flatMap { tap =>
        config.schemas.find(_.id == schema).map(_.definition.name).map {
          scalaType => s"""val $id = Lookup[$scalaType]($tap)""".parse[Stat].get
        }
      }
    }
  } match {
    case ds if ds.isEmpty => ds
    case ds => Seq(
      "import parallelai.sot.engine.Project, parallelai.sot.engine.io.datastore.{Datastore, Kind}".parse[Stat].get
    ) ++ ds
  }

  private def getMonadTransformations(config: Config, dag: Topology[String, DAGMapping], q: Term): Term = {
    val (tsortedVertices, tsortedEdges) = dag.topologicalSort()
    val sources = getSources(config).zipWithIndex
    val sinks = getSinks(config)
    val opInputs: Map[String, scala.Seq[String]] = tsortedEdges.groupBy(_._2).map(x =>(x._1, x._2.map(_._1)))
    val idsStack = mutable.Map[String, Int]()
    val ops = tsortedVertices.map {opId =>
      val op = getOp(opId, config.steps)
      val stepParsed =  op match {
        case _: TransformationOp =>
          setNextStackId(idsStack, opId)
          val opParsed = parseOperation(op, dag, config).get
          (opParsed._2, opParsed._3)

        case _: TFPredictOp =>
          setNextStackId(idsStack, opId)
          val opParsed = parseOperation(op, dag, config).get
          (opParsed._2, opParsed._3)

        case sourceOp: SourceOp =>
          setNextStackId(idsStack, opId)
          val sourceDef = sources.find(_._1._1 == sourceOp.id).get
          (Term.Name("read"), List(List(Term.Name("sc"), buildTap(Term.Name("conf.sourceTaps"), sourceDef._1._2, sourceDef._1._3, sourceDef._2), Term.Name("sotUtils"))))

        case sinkOp: SinkOp =>
          val sinkDef = sinks.find(_._1 == sinkOp.id).get
          val writeMethod = if (sinkDef._2.isDefined) "write" else "writeSchemaless"
          (Term.Name(writeMethod), List(List(buildTap(Term.Name("conf.sinkTaps"), sinkDef._2, sinkDef._3, sinks.indexOf(sinkDef)), Term.Name("sotUtils"))))

      }
      val inputScolls = opInputs.getOrElse(opId, Nil).map {e1 =>
          val inEdgeIndex = idsStack(e1)
          val idTerm = Term.Apply(Term.Name("Nat" + inEdgeIndex), List())
          q"sColls.at($idTerm)"
      }.toList

      val params = if (inputScolls.isEmpty) stepParsed._2 else List(inputScolls) ::: stepParsed._2
      (stepParsed._1, params)

    }.toList

    parseStateMonadExpression(ops, q)
  }

  private def setNextStackId(idsStack: mutable.Map[String, Int], id: String) = {
    val values = idsStack.values
    if (!idsStack.contains(id)){
      val newId = if (values.isEmpty) 0 else values.max + 1
      idsStack.put(id, newId)
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
        case class $name ( ..$listSchema) extends parallelai.sot.engine.io.utils.annotations.HasDatastoreAnnotation
      """

    Seq(block)
  }

  def SOTFieldParser(fieldName: String, fieldType: String, fieldMode: String): Term.Param = fieldMode match {
    case "nullable-repeated" => s"$fieldName: Option[List[$fieldType]]".parse[Term.Param].get
    case "nullable" => s"$fieldName: Option[$fieldType]".parse[Term.Param].get
    case "repeated" => s"$fieldName: List[$fieldType]".parse[Term.Param].get
    case "required" => s"$fieldName: $fieldType".parse[Term.Param].get
  }

  def SOTCaseClassParser(fields: List[JSONDefinitionField], name: String): List[(String, Term.Param)] = fields.flatMap { f =>
    f.`type` match {
      case "record" =>
        val newName = NameProvider.getUniqueName(name)
        (name, SOTFieldParser(f.name, newName, f.mode)) :: SOTCaseClassParser(f.fields.get, newName)

      case _ => List((name, SOTFieldParser(f.name, f.`type`, f.mode)))
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
    val sinksDefs = taps.view.zipWithIndex.map {
      case ((_, sinkSchema, sinkTap), i) =>
        buildTap(term, sinkSchema, sinkTap, i)
    }

    val sinks = sinksDefs.tail.foldLeft(Term.ApplyInfix(sinksDefs.head, Term.Name("::"), List(), List(Term.Name("HNil"))))
                                       { (cumul: Term.ApplyInfix, t: Term.Apply) => Term.ApplyInfix(t, Term.Name("::"), List(), List(cumul)) }

    sinks
  }

  private def buildTap(term: Term.Name, sinkSchema: Option[Schema], sinkTap: TapDefinition, tapIndex: Int) = {
    val sinkConfigApply = if (sinkSchema.isDefined) {
      typedTap(sinkSchema, sinkTap)
    } else {
      schemalessTap(sinkSchema, sinkTap)
    }

    Term.Apply(sinkConfigApply, Seq(q"$term(${Lit.Int(tapIndex)})._3"))
  }

  private def schemalessTap(sinkSchema: Option[Schema], sinkTap: TapDefinition) =
    Term.ApplyType(Term.Name("SchemalessTapDef"),
      List(Type.Name(getTapType(sinkTap)),
        Type.Name("parallelai.sot.engine.config.gcp.SOTUtils"),
        Type.Name(getSchemaAnnotation(sinkSchema))))

  private def typedTap(maybeSchema: Option[Schema], tap: TapDefinition) =
    Term.ApplyType(Term.Name("TapDef"),
      List(Type.Name(getTapType(tap)),
        Type.Name("parallelai.sot.engine.config.gcp.SOTUtils"),
        Type.Name(getSchemaAnnotation(maybeSchema)),
        Type.Name(maybeSchema.get.definition.name)))

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