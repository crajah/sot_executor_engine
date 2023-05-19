package parallelai.sot.macros

import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.executor.model.Topology

import scala.meta._
import scala.meta.Term

object SOTMacroHelper {

  /**
    * Lookup step for given operation
    */
  def getOp(id: String, steps: List[OpType]): OpType = {
    require(id.nonEmpty, "Operation id is empty")

    val step = steps.find(_.id == id)

    require(step.isDefined, s"Cannot find $id in list of operations")

    step.get
  }

  /**
    * Lookup schema for the given id
    */
  def getSchema(id: String, schemas: List[Schema]): Schema = {
    require(id.nonEmpty, "Schema id is empty")

    val schema = schemas.find(_.id == id)

    require(schema.isDefined, s"Cannot find $id in list of schemas")

    schema.get
  }

  /**
    * Lookup tap for the given id
    */
  def getTap(id: String, taps: List[TapDefinition]): TapDefinition = {
    require(id.nonEmpty, "Source id is empty")

    val tap = taps.find(_.id == id)

    require(tap.isDefined, s"Cannot find $id in list of taps")

    tap.get
  }

  def parseOperation(operation: OpType, dag: Topology[String, DAGMapping], config: Config): Option[(Term, List[List[Term]])] = {

    checkExpectedType(operation, dag)

    operation match {
      case op: TransformationOp =>
        val methodName = op.op.parse[Term].get
        val params = op.params.map(paramClause => paramClause.map(_.parse[Term].get).toList).toList
        Some(methodName, params)
      case _ => None
    }

  }

  def checkExpectedType(op: OpType, dag: Topology[String, DAGMapping]) = {

    if (dag.getSinkVertices().contains(op.id)) require(op.getClass == classOf[SinkOp], s"Operation ${op.id} should be a Sink")
    else if (dag.getSourceVertices().contains(op.id)) require(op.getClass == classOf[SourceOp], s"Operation ${op.id} should be a Source")
    else require(op.getClass == classOf[TransformationOp], s"Operation ${op.id} should be a Transformation")

  }

  /**
    * Walk through the dag from the tap to the last vertex and look up the
    */
  //TODO: implement for multiple edges
  def getOps(dag: Topology[String, DAGMapping],
             config: Config,
             tap: String,
             ops: List[Option[(Term, List[List[Term]])]]): List[Option[(Term, List[List[Term]])]] = {
    val nextStep = dag.edgeMap.get(tap)
    nextStep match {
      case Some(ns) =>
        val nextOperation = ns.head.to
        val operation = getOp(nextOperation, config.steps)
        val op = parseOperation(operation, dag, config)
        getOps(dag, config, nextOperation, ops :+ op)
      case None => ops
    }
  }

  def parseStateMonadExpression(ops: List[(Term, List[List[Term]])], q: Term = q"in"): Term = {
    ops match {
      case Nil => q
      case head :: tail => {
        head match {
          case (methodName, paramClauses) => parseStateMonadExpression(tail, q"${q}.flatMap(sColl => ${applyTermClauses(methodName, paramClauses)})")
          case (methodName, _) => parseStateMonadExpression(tail, q"${q}.flatMap(sColl => ${Term.Name(methodName.syntax)})")
        }
      }
    }
  }

  def applyTermClauses(methodName: Term, paramClauses: List[List[Term]]): Term = {
    applyTermClausesRecur(methodName, paramClauses.reverse)
  }

  def applyTermClausesRecur(methodName: Term, paramClauses: List[List[Term]]): Term = {
    paramClauses match {
      case firstClause :: Nil => Term.Apply(methodName, firstClause)
      case h :: tail => Term.Apply(applyTermClausesRecur(methodName, tail), h)
      case Nil => Term.Apply(methodName, List())
    }
  }

  def getSource(config: Config): (Option[Schema], TapDefinition) = {
    val dag = config.parseDAG()
    val sourceOpId = dag.getSourceVertices().head
    val sourceOperation = SOTMacroHelper.getOp(sourceOpId, config.steps) match {
      case s: SourceOp => s
      case _ => throw new Exception("Unsupported source operation")
    }

    (Some(SOTMacroHelper.getSchema(sourceOperation.schema, config.schemas)), SOTMacroHelper.getTap(sourceOperation.tap, config.taps))
  }

  def getSinks(config: Config): List[(Option[Schema], TapDefinition)] = {
    val dag = config.parseDAG()
    val sinkOperationIds = dag.getSinkVertices()
    sinkOperationIds.map(sinkOpId => {
      val sinkOperation = SOTMacroHelper.getOp(sinkOpId, config.steps) match {
        case s: SinkOp => s
        case _ => throw new Exception("Unsupported sink operation")
      }

      val sinkSchema = sinkOperation.schema match {
        case Some(schemaId) => Some(SOTMacroHelper.getSchema(schemaId, config.schemas))
        case None => None
      }
      (sinkSchema, SOTMacroHelper.getTap(sinkOperation.tap, config.taps))
    }).toList
  }

}
