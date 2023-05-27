package parallelai.sot.engine.serialization.protobuf

import java.io.{File, PrintWriter}
import java.nio.file.Files
import java.util.Base64

import protocbridge.{JvmGenerator, Target}

import scalapb.ScalaPbCodeGenerator

object ProtoPBCCodeGen {
  val scalapbOptions: String =
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

  def executeAll(schemaBase64: String): String = {
    val srcTarget = Files.createTempDirectory("scalapbCodeGen").toFile

    val protoPath = new File("sot-engine-core/target/protobuf/")
    if (!protoPath.exists()) protoPath.mkdirs()

    // Base64 decode the schema
    val decodedSchema = Base64.getDecoder.decode(schemaBase64)
    val protoSchema = new String(decodedSchema)
    val schema = scalapbOptions + protoSchema

    // Write the schema to a file
    val protoFile = new File("sot-engine-core/target/protobuf/gen.proto")
    new PrintWriter(protoFile) { write(schema); close }

    // Generate the code to a source file
    val scalapbPath = new File(this.getClass.getResource("/protobuf").toURI)
    val thirdPartyPath = new File(this.getClass.getResource("/protobuf/third_party").toURI)
    val result = execute(srcTarget, Set(protoFile), List(protoPath, scalapbPath, thirdPartyPath))

    // Read the generated code
    if (result != 0) throw new IllegalArgumentException(s"Failed to generate proto case classes, return code $result")
    scala.io.Source.fromFile(new File(srcTarget, "parallelai/sot/executor/builder/SOTBuilder/gen/GenProto.scala")).mkString
  }

  private[this] def execute(targetPath: File, schemas: Set[File], includePath: Seq[File]): Int = {
    val target = Seq(Target(gen(), targetPath))
    val protocVersion = "-v330"
    val protocCommand: Seq[String] => Int =
      (args: Seq[String]) => com.github.os72.protocjar.Protoc.runProtoc(protocVersion +: args.toArray)
    val protocOptions = Seq()
    val result = executeProtoc(protocCommand, schemas, includePath, protocOptions, target)
    if (result == 0) result
    else throw new Exception("Unable to parse proto schema")
  }

  private[this] def executeProtoc(protocCommand: Seq[String] => Int, schemas: Set[File], includePaths: Seq[File], protocOptions: Seq[String], targets: Seq[Target]): Int =
    try {
      val incPath = includePaths.map("-I" + _.getCanonicalPath)
      protocbridge.ProtocBridge.run(protocCommand, targets,
        incPath ++ protocOptions ++ schemas.map(_.getCanonicalPath))
    } catch {
      case e: Exception =>
        throw new RuntimeException("error occurred while compiling protobuf files", e)
    }

  private[this] def gen(
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
}
