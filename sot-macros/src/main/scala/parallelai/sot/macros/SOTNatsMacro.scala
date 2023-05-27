package parallelai.sot.macros

import scala.collection.immutable.Seq
import scala.collection.mutable.ArrayBuffer
import scala.meta._

class SOTNatsMacro extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {

    defn match {
      case q"object $name { ..$statements}" =>

        val nat0 = Seq(q"type Nat0 = Nat._0",  q"object Nat0 { def apply() = new Nat0}")
        val nats = ArrayBuffer(nat0:_*)
        for (i <- 1 to 100) {
          val natPrevType = Type.Name("Nat" + (i - 1))
          val natCurType = Type.Name("Nat" + i)
          val natCurTerm = Term.Name("Nat" + i)
          val natCurTemplate = Term.New(Template(List(), List(Ctor.Ref.Name("Nat" + i)), Term.Param(List(), Term.Name("self"), None, None), Some(List())))

          nats.append(q"type ${natCurType} =  Succ[${natPrevType}]")
          nats.append(q"object ${natCurTerm} { def apply() = ${natCurTemplate}}")
        }

        val syn = statements ++ nats
        val code = q"""object $name {
        ..$syn
        }"""
        code
      case _ =>
        abort("@main must annotate an object.")
    }
  }
}