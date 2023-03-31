package parallelai.sot.types

import scala.annotation.StaticAnnotation
import spray.json.DefaultJsonProtocol._
import spray.json._
import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.executor.model.SOTMacroJsonConfig._

import scala.collection.immutable.Seq
import scala.meta._

trait HasProtoAnnotation

object ProtobufType {

  class fromSchema(arg: String) extends scala.annotation.StaticAnnotation {

    inline def apply(defn: Any): Any = meta {
      val definition = this match {
        case q"new $_(${Lit.String(arg)})" => arg.parseJson.convertTo[Definition]
        case _ => abort("@ProtobufType parameter should be a string.")
      }

      val protoDefinition = definition match {
        case d : ProtobufDefinition => d
        case _ => abort("Expected ProtobufDefinition")
      }

      defn match {
        case q"class $name" => ProtobufTypeImpl.expand(protoDefinition)

      }
    }
  }
}

object ProtobufTypeImpl {

  def expand(definition: ProtobufDefinition): Defn.Class = {
    val listSchema = definition.fields.map { sc =>
      sc.mode match {
        case "required" => s"${sc.name}: ${sc.`type`}".parse[Term.Param].get
        case "optional" => s"${sc.name}: Option[${sc.`type`}]".parse[Term.Param].get
        case "repeated" => s"${sc.name}: List[${sc.`type`}]".parse[Term.Param].get
        case _ => abort(s"Unknown mode ${sc.mode}")
      }
    }
    val name = Type.Name(definition.name)
    val block =
      q"""
        case class $name ( ..$listSchema) extends HasProtoAnnotation
        """
    block
  }
}
