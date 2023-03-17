package parallelai.sot.macros

import parallelai.sot.executor.model.SOTMacroConfig.{Source => SOTSource, _}
import parallelai.sot.executor.model.Topology

import scala.meta._
import scala.meta.Term

object SOTMacroHelper {

  /**
    * Parse and validate DAG from config file
    */
  def validateDag(c: Config) = {

    val dag = c.parseDAG()

    //no in or out-going branches
    require(dag.findWithOutgoingEdges().isEmpty)
    require(dag.findWithIncomingEdges().isEmpty)

    //no unconnected vertices
    require(dag.unconnected == 0)

    //one sink and one source only
    require(dag.getSourceVertices().size == 1)
    require(dag.getSinkVertices().size == 1)

    dag
  }

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
  def getSchema(name: String, schemas: List[Schema]): Schema = {
    require(name.nonEmpty, "Schema name is empty")

    val schema = schemas.find(_.name == name)

    require(schema.isDefined, s"Cannot find $name in list of schemas")

    schema.get
  }

  /**
    * Lookup source for the given name
    */
  def getSource(name: String, sources: List[SOTSource]): SOTSource = {
    require(name.nonEmpty, "Source name is empty")

    val source = sources.find(_.name == name)

    require(source.isDefined, s"Cannot find $name in list of sources")

    source.get
  }

  def parseOperation(operation: OpType, dag: Topology[String, DAGMapping], config: Config): Option[(Term, Option[Term])] = {

    checkExpectedType(operation, dag)

    operation match {
      case op: TransformationOp =>
        val name = op.op.parse[Term].get
        val code = if (op.func.nonEmpty) Some(op.func.parse[Term].get) else None
        Some(name, code)
      case _ => None
    }

  }

  def checkExpectedType(op: OpType, dag: Topology[String, DAGMapping]) = {

    if (dag.getSinkVertices().contains(op.name)) require(op.getClass == classOf[SinkOp], s"Operation ${op.name} should be a Sink")
    else if (dag.getSourceVertices().contains(op.name)) require(op.getClass == classOf[SourceOp], s"Operation ${op.name} should be a Source")
    else require(op.getClass == classOf[TransformationOp], s"Operation ${op.name} should be a Transformation")

  }

  /**
    * Walk through the dag from the source to the last vertex and look up the
    */
  //TODO: implement for multiple edges
  def getOps(dag: Topology[String, DAGMapping],
             config: Config,
             source: String,
             ops: List[Option[(Term, Option[Term])]]): List[Option[(Term, Option[Term])]] = {
    val nextStep = dag.edgeMap.get(source)
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
  def parseExpression(ops: List[(Term, Option[Term])], q: Term = q"in"): Stat = {
    ops match {
      case Nil => q
      case head :: tail => {
        head match {
          case (name, Some(expression)) => parseExpression(tail, q"${q}.${Term.Name(name.syntax)}(${expression})")
          case (name, _) => parseExpression(tail, q"${q}.${Term.Name(name.syntax)}")
        }
      }
    }
  }

}
