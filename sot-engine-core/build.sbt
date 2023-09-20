import Dependencies._
import SotDependencies._

libraryDependencies ++= Seq(
  sotExecutorModel
)

libraryDependencies ++= Seq(
  typesafeConfig,
  pureConfig,
  grizzledLogging,
  circe,
  circeShapes,
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
  scalacheck,
  javaKafkaBeam
//  ,flinkBeam
  //, springExpression
)

libraryDependencies ++= Seq(
  scalatest % Test,
  mockito % Test,
  scalacheckShapeless % Test,
  spotifyTest % Test,
  spotifyCore % Test,
  testContainers % IntegrationTest,
  avro4sCore % IntegrationTest,
  avro4sJson % IntegrationTest,
  avro4sMacros % IntegrationTest,
  slfLoggingApi % IntegrationTest,
  slfLoggingSimple % IntegrationTest
)