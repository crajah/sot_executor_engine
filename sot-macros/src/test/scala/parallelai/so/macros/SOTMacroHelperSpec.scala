package parallelai.so.macros

import org.scalatest.compatible.Assertion
import org.scalatest.{FlatSpec, Matchers}
import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.macros.{SOTMacroHelper, SOTMainMacroImpl}

import scala.meta.{Term, _}

class SOTMacroHelperSpec extends FlatSpec with Matchers {

  "SOTMacroHelper service" should "build correct term from single empty clauses" in {
    val applyTerm = SOTMacroHelper.applyTermClauses(parseTerm("func"), List())
    val expected = q"func()"
    expected.structure should ===(applyTerm.structure)
  }

  "SOTMacroHelper service" should "build correct term from multiple empty clauses" in {
    val applyTerm = SOTMacroHelper.applyTermClauses(parseTerm("func"), List(List(), List()))
    val expected = q"func()()"
    applyTerm.structure should ===(expected.structure)
  }

  "SOTMacroHelper service" should "build correct term from 1 clause" in {
    val applyTerm = SOTMacroHelper.applyTermClauses(parseTerm("func"), List(List(parseTerm("param1"), parseTerm("param2"), parseTerm("param3"))))
    val expected = q"func(param1, param2, param3)"
    applyTerm.structure should ===(expected.structure)
  }

  "SOTMacroHelper service" should "build correct term from multiple clauses" in {
    val applyTerm = SOTMacroHelper.applyTermClauses(parseTerm("func"),
      List(List(parseTerm("param1"), parseTerm("param2"), parseTerm("param3")),
           List(parseTerm("param3"), parseTerm("param4")),
           List(parseTerm("param5"), parseTerm("param6"))
    ))
    val expected = q"func(param1, param2, param3)(param3, param4)(param5, param6)"
    applyTerm.structure should ===(expected.structure)
  }

  "SOTMacroHelper service" should "sort topoligically" in {
    val sorted = SOTMacroHelper.topologicalSort(Seq(("in1", "op1"), ("in", "op1"), ("op1", "out1"), ("op1", "out2")))
    sorted._1 should ===(Seq("in", "in1", "op1", "out1", "out2"))
    sorted._2 should ===(Seq(("in", "op1"), ("in1", "op1"), ("op1", "out1"), ("op1", "out2")))
  }


  private def parseTerm(term: String) = {
    term.parse[Term].get
  }

  def assertEqualStructure(config: Config, expectedBlock: Term.Block): Assertion = {
    val rs = q"object Test { object conf { }; val x =1 }" match {
      case q"object $name {object conf { ..$confStatements };  ..$statements}" =>
        SOTMainMacroImpl.expand(name, confStatements, statements, config)
    }

    val stats = rs.templ.stats.get
    val expected = Term.Block.unapply(expectedBlock).get
    for (i <- 1 until expected.length) {
      expected(i).structure should ===(stats(i).structure)
    }
    expected.structure should ===(stats.structure)
  }


}