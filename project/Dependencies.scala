import sbt._

object Dependencies {
  val scala211 = "2.11.11"
  val scala212 = "2.12.2"
  val metaVersion = "1.8.0"
  val paradiseVersion = "3.0.0-M9"
  val monocleVersion = "1.4.0"
  val trueAccordVersion = "0.6.6"
  val twitterBijectionVersion = "0.9.5"
  val spotifyScioVersion = "0.4.5"
  val shapelessDataTypeDataVersion = "0.1.7"
  val tensorflowVersion = "1.3.0"
  val gcloudVersion = "1.6.0"
  val avro4sVersion = "1.8.0"

  val scalameta = "org.scalameta" %% "scalameta" % metaVersion
  val contrib = "org.scalameta" %% "contrib" % metaVersion
  val testkit = "org.scalameta" %% "testkit" % metaVersion

  val paradise = "org.scalameta" % "paradise" % paradiseVersion cross CrossVersion.full
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.4"
  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.5"
  val scalacheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.6"
  val mockito = "org.mockito" % "mockito-all" % "1.10.19"
  val grizzledLogging = "org.clapper" %% "grizzled-slf4j" % "1.0.2"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val testContainers = "com.dimafeng" %% "testcontainers-scala" % "0.11.0"

  val typesafeConfig = "com.typesafe" % "config" % "1.3.2"
  val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.8.0"

  val monocle = "com.github.julien-truffaut" %% "monocle-core" % monocleVersion
  val monocleMacro = "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.16"

  val sprayJson = "io.spray" %% "spray-json" % "1.3.3"
  val circe = "io.circe" %% "circe-core" % "0.9.1"
  val circeShapes = "io.circe" %% "circe-shapes" % "0.9.1"

  val trueAccordCompiler = "com.trueaccord.scalapb" %% "compilerplugin" % trueAccordVersion
  val trueAccordRuntime = "com.trueaccord.scalapb" % "scalapb-runtime_2.11" % trueAccordVersion

  val shapeless = "com.chuusai" %% "shapeless" % "2.3.2"

  val gcloudDatastore = "com.google.cloud" % "google-cloud-datastore" % gcloudVersion
  val gcloudStorage = "com.google.cloud" % "google-cloud-storage" % gcloudVersion
  val gcloudHttpClient = "com.google.http-client" % "google-http-client" % "1.22.0"

  val kafka = ("org.apache.kafka" %% "kafka" % "0.10.1.1")
    .exclude("javax.jms", "jms")
    .exclude("com.sun.jdmk", "jmxtools")
    .exclude("com.sun.jmx", "jmxri")
    .exclude("org.slf4j", "slf4j-simple")
    .exclude("org.slf4j", "slf4j-log4j12")
    .exclude("log4j", "log4j")
    .exclude("org.apache.zookeeper", "zookeeper")
    .exclude("com.101tec", "zkclient")

  val protoc = "com.github.os72" % "protoc-jar" % "3.4.0.1"

  val twitterBijectionCore = "com.twitter" %% "bijection-core" % twitterBijectionVersion
  val twitterBijectionAvro = "com.twitter" %% "bijection-avro" % twitterBijectionVersion

  val spotifyCore = "com.spotify" %% "scio-core" % spotifyScioVersion

  val spotifyBigTable = "com.spotify" %% "scio-bigtable" % spotifyScioVersion
  val spotifyExtra = "com.spotify" %% "scio-extra" % spotifyScioVersion
  val spotifyTensorflow = "com.spotify" %% "scio-tensorflow" % spotifyScioVersion
  val spotifyTest = "com.spotify" %% "scio-test" % spotifyScioVersion

  val tensorflow = "org.tensorflow" % "tensorflow" % tensorflowVersion

  val shapelessDataTypeBigQuery = "me.lyh" %% "shapeless-datatype-bigquery" % shapelessDataTypeDataVersion
  val shapelessDataTypeCore = "me.lyh" %% "shapeless-datatype-core" % shapelessDataTypeDataVersion
  val shapelessDataTypeDatastore = "me.lyh" %% "shapeless-datatype-datastore_1.3" % shapelessDataTypeDataVersion

  val avro = "org.apache.avro" % "avro" % "1.8.2"

  val avro4sCore = "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion
  val avro4sJson = "com.sksamuel.avro4s" %% "avro4s-json" % avro4sVersion
  val avro4sMacros = "com.sksamuel.avro4s" %% "avro4s-macros" % avro4sVersion

  val javaKafkaBeam = "org.apache.beam" % "beam-sdks-java-io-kafka" % "2.1.0"
  val flinkBeam = "org.apache.beam" % "beam-runners-flink_2.11" % "2.4.0"
  val slfLoggingApi = "org.slf4j" % "slf4j-api" % "1.7.5"
  val slfLoggingSimple =  "org.slf4j" % "slf4j-simple" % "1.7.5"
  val springExpression = "org.springframework" % "spring-expression" % "4.3.5.RELEASE"
}