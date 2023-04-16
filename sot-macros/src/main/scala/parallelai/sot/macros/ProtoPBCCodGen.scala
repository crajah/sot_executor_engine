package parallelai.sot.macros


import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

import org.apache.commons.io.FileUtils
import protocbridge.{JvmGenerator, Target}

import scalapb.ScalaPbCodeGenerator

object ProtoPBCCodGen {

  val scalapbOptions =
    """
      |import "scalapb/scalapb.proto";
      |
      |option (scalapb.options) = {
      |  package_name: "parallelai.sot.executor.builder.SOTBuilder"
      |  collection_type: "List"
      |  flat_package: false
      |  single_file: true
      |};
      |
    """.stripMargin

  private[this] def executeProtoc(protocCommand: Seq[String] => Int, schemas: Set[File], includePaths: Seq[File], protocOptions: Seq[String], targets: Seq[Target]): Int =
    try {
      val incPath = includePaths.map("-I" + _.getCanonicalPath)
      protocbridge.ProtocBridge.run(protocCommand, targets,
        incPath ++ protocOptions ++ schemas.map(_.getCanonicalPath))
    } catch {
      case e: Exception =>
        throw new RuntimeException("error occurred while compiling protobuf files", e)
    }

  def gen(
           flatPackage: Boolean = false,
           javaConversions: Boolean = false,
           grpc: Boolean = true,
           singleLineToString: Boolean = false): (JvmGenerator, Seq[String]) =
    (JvmGenerator(
      "scala",
      ScalaPbCodeGenerator),
      Seq(
        "flat_package" -> flatPackage,
        "java_conversions" -> javaConversions,
        "grpc" -> grpc,
        "single_line_to_string" -> singleLineToString
      ).collect { case (name, v) if v => name })


  def execute(targetPath: File, schemas: Set[File], includePath: Seq[File]): Int = {
    val target = Seq(Target(gen(), targetPath))
    val protocVersion = "-v330"
    val protocCommand: Seq[String] => Int =
      (args: Seq[String]) => com.github.os72.protocjar.Protoc.runProtoc(protocVersion +: args.toArray)
    val protocOptions = Seq()
    val result = executeProtoc(protocCommand, schemas, includePath, protocOptions, target)
    if (result == 0) result
    else throw new Exception("Unable to parse proto schema")
  }

  def main(args: Array[String]): Unit = {
    executeAll("cGFja2FnZSBjb3JlOw0KDQpvcHRpb24gamF2YV9wYWNrYWdlID0gInBhcmFsbGVsYWkuc290LmV4ZWN1dG9yLmJ1aWxkZXIuU09UQnVpbGRlciI7DQpvcHRpb24gamF2YV9vdXRlcl9jbGFzc25hbWUgPSAiQ29yZU1vZGVsIjsNCg0KbWVzc2FnZSBNZXNzYWdlIHsNCiAgICANCiAgIA0KICAgIHJlcXVpcmVkIHN0cmluZyB1c2VyID0gMTsNCiAgICByZXF1aXJlZCBzdHJpbmcgdGVhbU5hbWUgPSAyOw0KICAgIHJlcXVpcmVkIGludDY0IHNjb3JlID0gMzsNCiAgICByZXF1aXJlZCBpbnQ2NCBldmVudFRpbWUgPSA0Ow0KICAgIHJlcXVpcmVkIHN0cmluZyBldmVudFRpbWVTdHIgPSA1Ow0KDQp9")
  }

  def executeAll(schemaBase6: String): String = {
    val srcTarget = new File("/tmp/scalapb")
    if (!srcTarget.exists()) srcTarget.mkdirs()

    val protoPath = new File("sot-executor/target/protobuf/")
    if (!protoPath.exists()) protoPath.mkdirs()

    val protoFile = new File("sot-executor/target/protobuf/gen.proto")
    val decodedSchema = Base64.getDecoder.decode(schemaBase6)
    val protoSchema = new String(decodedSchema)
    val schema = scalapbOptions + protoSchema
    FileUtils.writeStringToFile(protoFile, schema, UTF_8)

    val scalapbPath = new File("sot-macros/src/main/resources/protobuf/")
    val thridPartyPath = new File("sot-macros/src/main/resources/protobuf/third_party")
    val result = ProtoPBCCodGen.execute(srcTarget, Set(protoFile), List(protoPath, scalapbPath, thridPartyPath))
    if (result != 0) throw new IllegalArgumentException(s"Failed to generate proto case classes, return code $result")
    val generatedCode = FileUtils.readFileToString(new File(srcTarget, "parallelai/sot/executor/builder/SOTBuilder/gen/GenProto.scala"), UTF_8)
    //    println(generatedCode)
    generatedCode
  }

}
