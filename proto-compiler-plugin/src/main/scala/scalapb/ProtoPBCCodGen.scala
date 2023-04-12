package scalapb

import java.io.File

import protocbridge.{JvmGenerator, Target}

object ProtoPBCCodGen extends App {
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


  val target = Seq(Target(gen(), new File("proto-compiler-plugin/src/main/protobuf/")))
  val protocVersion = "-v330"
  val protocCommand: Seq[String] => Int =
    (args: Seq[String]) => com.github.os72.protocjar.Protoc.runProtoc(protocVersion +: args.toArray)
  val schemas = Set(new File("proto-compiler-plugin/src/main/protobuf/core.proto"))
  val includePath = Seq(new File("proto-compiler-plugin/src/main/protobuf/"))
  val protocOptions = Seq()
  print(executeProtoc(protocCommand, schemas, includePath, Seq(), target))
}
