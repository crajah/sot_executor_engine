package parallelai.sot.macros

import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.executor.model.Topology

import scala.meta._
import scala.meta.Term

object SOTMacroHelper {

  /**
    * Lookup step for given operation
    */
  def getOp(name: String, steps: List[OpType]): OpType = {
    require(name.nonEmpty, "Operation name is empty")

    val step = steps.find(_.name == name)

    require(step.isDefined, s"Cannot find $name in list of operations")

    step.get
  }

  /**
    * Lookup schema for the given name
    */
  def getSchema(id: String, schemas: List[Schema]): Schema = {
    require(id.nonEmpty, "Schema name is empty")

    val schema = schemas.find(_.id == id)

    require(schema.isDefined, s"Cannot find $id in list of schemas")

    schema.get
  }

  /**
    * Lookup tap for the given name
    */
  def getTap(id: String, taps: List[TapDefinition]): TapDefinition = {
    require(id.nonEmpty, "Source name is empty")

    val tap = taps.find(_.id == id)

    require(tap.isDefined, s"Cannot find $id in list of taps")

    tap.get
  }

  def parseOperation(operation: OpType, dag: Topology[String, DAGMapping], config: Config): Option[(Term, Option[List[Term]])] = {

    checkExpectedType(operation, dag)

    operation match {
      case op: TransformationOp =>
        val name = op.op.parse[Term].get
        val code = if (op.func.nonEmpty) Some(List(op.func.parse[Term].get)) else None
        Some(name, code)
      case op: TFPredictOp =>
        val name = "predict".parse[Term].get
        val fetchLit : List[Lit.String] = op.fetchOps.map(f => Lit.String(f)).toList
        val fetchOps = Term.Assign(Term.Name("fetchOps"), Term.Apply(Term.Name("Seq"), fetchLit))
        val modelBucket = Term.Assign(Term.Name("modelBucket"), Lit.String(op.modelBucket))
        val modelPath = Term.Assign(Term.Name("modelPath"), Lit.String(op.modelPath))
        val inFn = Term.Assign(Term.Name("inFn"), op.inFn.parse[Term].get)
        val outFn = Term.Assign(Term.Name("outFn"), op.outFn.parse[Term].get)
        val code = Some(List(modelBucket, modelPath, fetchOps, inFn, outFn))
        Some(name, code)
      case _ => None
    }

  }

  def checkExpectedType(op: OpType, dag: Topology[String, DAGMapping]) = {

    if (dag.getSinkVertices().contains(op.name)) require(op.getClass == classOf[SinkOp], s"Operation ${op.name} should be a Sink")
    else if (dag.getSourceVertices().contains(op.name)) require(op.getClass == classOf[SourceOp], s"Operation ${op.name} should be a Source")
    else require(op.getClass == classOf[TransformationOp] | op.getClass == classOf[TFPredictOp], s"Operation ${op.name} should be a Transformation or a Predict")

  }

  /**
    * Walk through the dag from the tap to the last vertex and look up the
    */
  //TODO: implement for multiple edges
  def getOps(dag: Topology[String, DAGMapping],
             config: Config,
             tap: String,
             ops: List[Option[(Term, Option[List[Term]])]]): List[Option[(Term, Option[List[Term]])]] = {
    val nextStep = dag.edgeMap.get(tap)
    nextStep match {
      case Some(ns) =>
        val nextOperation = ns.head.to
        val operation = getOp(nextOperation, config.steps)
        val op = parseOperation(operation, dag, config)
        getOps(dag, config, nextOperation, ops ++ List(op))
      case None => ops
    }
  }

  /**
    * Parse expressions from a format List((ex1, a => b), (ex2, b => c))
    * to in.ex1(a => b).ex2(b => c)
    */
  def parseStateMonadExpression(ops: List[(Term, Option[List[Term]])], q: Term = q"in"): Term = {
    ops match {
      case Nil => q
      case head :: tail => {
        head match {
          case (name, Some(expression)) =>
            parseStateMonadExpression(tail, q"${q}.flatMap(sColl => ${Term.Apply(Term.Name(name.syntax), expression)})")
          case (name, _) => parseStateMonadExpression(tail, q"${q}.flatMap(sColl => ${Term.Name(name.syntax)})")
        }
      }
    }
  }

  def getSource(config: Config): (Option[Schema], TapDefinition) = {
    val dag = config.parseDAG()
    val sourceOperationName = dag.getSourceVertices().head
    val sourceOperation = SOTMacroHelper.getOp(sourceOperationName, config.steps) match {
      case s: SourceOp => s
      case _ => throw new Exception("Unsupported source operation")
    }

    (Some(SOTMacroHelper.getSchema(sourceOperation.schema, config.schemas)), SOTMacroHelper.getTap(sourceOperation.tap, config.taps))
  }

  def getSinks(config: Config): List[(Option[Schema], TapDefinition)] = {
    val dag = config.parseDAG()
    val sinkOperationNames = dag.getSinkVertices()
    sinkOperationNames.map(sinkOperationName => {
      val sinkOperation = SOTMacroHelper.getOp(sinkOperationName, config.steps) match {
        case s: SinkOp => s
        case _ => throw new Exception("Unsupported sink operation")
      }

      val sinkSchema = sinkOperation.schema match {
        case Some(schemaName) => Some(SOTMacroHelper.getSchema(schemaName, config.schemas))
        case None => None
      }
      (sinkSchema, SOTMacroHelper.getTap(sinkOperation.tap, config.taps))
    }).toList
  }

}
