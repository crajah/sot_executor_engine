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
  tensorflow,
  avro,
  shapelessDataTypeDatastore,
  shapelessDataTypeCore,
  shapelessDataTypeBigQuery,
  scalaz,
  gcloudDatastore,
  gcloudStorage,
  logbackClassic,
  scalacheck
)

libraryDependencies ++= Seq(
  scalatest % Test,
  mockito % Test,
  scalacheckShapeless % Test,
  spotifyTest % Test,
  spotifyCore % Test,
  spotifyTensorflow % Test
)