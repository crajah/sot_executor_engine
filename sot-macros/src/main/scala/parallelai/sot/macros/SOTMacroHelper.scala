package parallelai.sot.macros

import scala.annotation.tailrec
import scala.meta.{Term, _}
import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.executor.model.Topology

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

  def parseOperation(operation: OpType, dag: Topology[String, DAGMapping], config: Config): Option[(String, Term, List[List[Term]])] = {
    checkExpectedType(operation, dag)

    operation match {
      case op: TransformationOp =>
        val methodName = op.op.parse[Term].get
        val params = op.params.map(paramClause => paramClause.map(_.parse[Term].get).toList).toList
        Some(op.id, methodName, params)

      case op: TFPredictOp =>
        val name = "predict".parse[Term].get
        val fetchLit : List[Lit.String] = op.fetchOps.map(f => Lit.String(f)).toList
        val fetchOps = Term.Assign(Term.Name("fetchOps"), Term.Apply(Term.Name("Seq"), fetchLit))
        val modelBucket = Term.Assign(Term.Name("modelBucket"), Lit.String(op.modelBucket))
        val modelPath = Term.Assign(Term.Name("modelPath"), Lit.String(op.modelPath))
        val inFn = Term.Assign(Term.Name("inFn"), op.inFn.parse[Term].get)
        val outFn = Term.Assign(Term.Name("outFn"), op.outFn.parse[Term].get)
        val code = List(List(modelBucket, modelPath, fetchOps, inFn, outFn))
        Some(op.id, name, code)

      case _ => None
    }
  }

  def checkExpectedType(op: OpType, dag: Topology[String, DAGMapping]): Unit = {
    if (dag.getSinkVertices().contains(op.id)) require(op.getClass == classOf[SinkOp], s"Operation ${op.id} should be a Sink")
    else if (dag.getSourceVertices().contains(op.id)) require(op.getClass == classOf[SourceOp], s"Operation ${op.id} should be a Source")
    else require(op.getClass == classOf[TransformationOp] | op.getClass == classOf[TFPredictOp], s"Operation ${op.id} should be a Transformation or a Predict")
  }

  /**
    * Walk through the dag from the tap to the last vertex and look up the
    */
  def getOps(dag: Topology[String, DAGMapping],
             config: Config,
             tap: String,
             ops: List[Option[(String, Term, List[List[Term]])]]): List[Option[(String, Term, List[List[Term]])]] = {
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

  @tailrec
  def parseStateMonadExpression(ops: List[(Term, List[List[Term]])], q: Term = q"in"): Term = ops match {
    case Nil => q

    case head :: tail => head match {
      case (methodName, paramClauses) => parseStateMonadExpression(tail, q"$q.flatMap(sColls => ${applyTermClauses(methodName, paramClauses)})")
      case (methodName, _) => parseStateMonadExpression(tail, q"$q.flatMap(sColls => ${Term.Name(methodName.syntax)})")
    }
  }

  def applyTermClauses(methodName: Term, paramClauses: List[List[Term]]): Term = {
    def applyTermClausesRecur(methodName: Term, paramClauses: List[List[Term]]): Term = paramClauses match {
      case firstClause :: Nil => Term.Apply(methodName, firstClause)
      case h :: tail => Term.Apply(applyTermClausesRecur(methodName, tail), h)
      case Nil => Term.Apply(methodName, List())
    }

    applyTermClausesRecur(methodName, paramClauses.reverse)
  }

  def getSources(config: Config): List[(String, Option[Schema], TapDefinition)] = {
    val dag = config.parseDAG()
    val sourceIdsSorted = dag.topologicalSort()._1.intersect(dag.getSourceVertices().toSeq) // topological order preserved

    sourceIdsSorted.map { id =>
      val sourceOp = SOTMacroHelper.getOp(id, config.steps) match {
        case s: SourceOp => s
        case s => throw new Exception(s"${s.getClass.getCanonicalName} is not a source operation for $id id. " +
          s"This likely to happen when an operation is a leaf node in the DAG but it is not a supported source.")
      }

      (sourceOp.id, Some(SOTMacroHelper.getSchema(sourceOp.schema, config.schemas)), SOTMacroHelper.getTap(sourceOp.tap, config.taps))

    }.toList
  }

  def getSinks(config: Config): List[(String, Option[Schema], TapDefinition)] = {
    val dag = config.parseDAG()
    val sinkIdsSorted = dag.topologicalSort()._1.intersect(dag.getSinkVertices().toSeq)

    sinkIdsSorted.map { sinkOpId =>
      val sinkOp = SOTMacroHelper.getOp(sinkOpId, config.steps) match {
        case s: SinkOp => s
        case s => throw new Exception(s"${s.getClass.getCanonicalName} is not a sink operation for $sinkOpId id. " +
          s"This likely to happen when an operation is a leaf node in the DAG but it is not a supported sink.")
      }
      val sinkSchema = sinkOp.schema match {
        case Some(schemaId) => Some(SOTMacroHelper.getSchema(schemaId, config.schemas))
        case None => None
      }

      (sinkOp.id, sinkSchema, SOTMacroHelper.getTap(sinkOp.tap, config.taps))

    }.toList
  }
}