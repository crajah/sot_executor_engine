import Dependencies._

libraryDependencies ++= Seq(
  typesafeConfig,
  pureConfig,
  grizzledLogging,
  shapeless,
  refined,
  gcloudDatastore
)

libraryDependencies ++= Seq(
  scalatest % "test, it",
  mockito % "test, it",
  testContainers % "test, it"
)