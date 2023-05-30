import Dependencies._
import SotDependencies._

libraryDependencies ++= Seq(
  sotExecutorModel
)

libraryDependencies ++= Seq(
  circe,
  typesafeConfig,
  pureConfig,
  grizzledLogging,
  monocle,
  monocleMacro,
  kafka,
  twitterBijectionCore,
  twitterBijectionAvro,
  spotifyCore,
  spotifyBigTable,
  spotifyExtra,
  avro,
  shapelessDataTypeDatastore,
  shapelessDataTypeCore,
  shapelessDataTypeBigQuery,
  gcloudDatastore,
  gcloudHttpClient,
  logbackClassic,
  scalacheck
)

libraryDependencies ++= Seq(
  scalatest % Test,
  mockito % Test,
  scalacheckShapeless % Test,
  spotifyTest % Test,
  avro4sCore % IntegrationTest,
  avro4sJson % IntegrationTest,
  avro4sMacros % IntegrationTest
)