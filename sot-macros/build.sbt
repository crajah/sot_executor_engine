import Dependencies._
import SotDependencies._

libraryDependencies ++= Seq(
  sotExecutorModel
)

libraryDependencies ++= Seq(
  typesafeConfig,
  grizzledLogging,
  sprayJson,
  shapeless
)

libraryDependencies ++= Seq(
  scalatest % Test,
  scalacheck % Test
)