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
  logbackClassic,
  scalacheck
)

libraryDependencies ++= Seq(
  scalatest % Test,
  mockito % Test,
  scalacheckShapeless % Test,
  spotifyTest % Test
)