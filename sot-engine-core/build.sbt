import Dependencies._
import SotDependencies._

libraryDependencies ++= Seq(
  sotExecutorModel
)

libraryDependencies ++= Seq(
  typesafeConfig,
  pureConfig,
  grizzledLogging,
  sprayJson,
  trueAccordCompiler,
  trueAccordRuntime,
  protoc,
  kafka,
  spotifyCore,
  spotifyBigTable,
  spotifyExtra,
  avro,
  shapelessDataTypeDatastore,
  shapelessDataTypeCore,
  shapelessDataTypeBigQuery,
  scalaz,
  gcloudDatastore,
  logbackClassic,
  scalacheck
)

libraryDependencies ++= Seq(
  scalatest % Test,
  mockito % Test,
  scalacheckShapeless % Test,
  spotifyTest % Test
)