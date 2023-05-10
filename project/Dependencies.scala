import sbt._

object Dependencies {
  val scala211 = "2.11.11"
  val scala212 = "2.12.2"
  val metaVersion = "1.8.0"
  val paradiseVersion = "3.0.0-M9"
  val trueAccordVersion = "0.6.6"
  val twitterBijectionVersion = "0.9.5"
  val spotifyVersion = "0.4.4"
  val shapelessDataTypeDataVersion = "0.1.7"

  val scalameta = "org.scalameta" %% "scalameta" % metaVersion
  val contrib = "org.scalameta" %% "contrib" % metaVersion
  val testkit = "org.scalameta" %% "testkit" % metaVersion

  val paradise = "org.scalameta" % "paradise" % paradiseVersion cross CrossVersion.full
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.4"
  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.5"
  val scalacheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.6"
  val mockito = "org.mockito" % "mockito-all" % "1.10.19"
  val grizzledLogging = "org.clapper" %% "grizzled-slf4j" % "1.3.1"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val typesafeConfig = "com.typesafe" % "config" % "1.3.2"
  val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.8.0"

  val sprayJson = "io.spray" %% "spray-json" % "1.3.3"

  val trueAccordCompiler = "com.trueaccord.scalapb" %% "compilerplugin" % trueAccordVersion
  val trueAccordRuntime = "com.trueaccord.scalapb" % "scalapb-runtime_2.11" % trueAccordVersion

  val shapeless = "com.chuusai" %% "shapeless" % "2.3.2"

  val gcloudDatastore = "com.google.cloud" % "google-cloud-datastore" % "1.6.0"

  val kafka = ("org.apache.kafka" %% "kafka" % "0.10.1.1")
    .exclude("javax.jms", "jms")
    .exclude("com.sun.jdmk", "jmxtools")
    .exclude("com.sun.jmx", "jmxri")
    .exclude("org.slf4j", "slf4j-simple")
    .exclude("org.slf4j", "slf4j-log4j12")
    .exclude("log4j", "log4j")
    .exclude("org.apache.zookeeper", "zookeeper")
    .exclude("com.101tec", "zkclient")

  val protoc = "com.github.os72" % "protoc-jar" % "3.4.0"

  val twitterBijectionCore = "com.twitter" %% "bijection-core" % twitterBijectionVersion
  val twitterBijectionAvro = "com.twitter" %% "bijection-avro" % twitterBijectionVersion

  val spotifyCore = "com.spotify" %% "scio-core" % spotifyVersion
  val spotifyBigTable = "com.spotify" %% "scio-bigtable" % spotifyVersion
  val spotifyExtra = "com.spotify" %% "scio-extra" % spotifyVersion
  val spotifyTensorflow = "com.spotify" %% "scio-tensorflow" % spotifyVersion
  val spotifyTest = "com.spotify" %% "scio-test" % spotifyVersion

  val shapelessDataTypeBigQuery = "me.lyh" %% "shapeless-datatype-bigquery" % shapelessDataTypeDataVersion
  val shapelessDataTypeCore = "me.lyh" %% "shapeless-datatype-core" % shapelessDataTypeDataVersion
  val shapelessDataTypeDatastore = "me.lyh" %% "shapeless-datatype-datastore_1.3" % shapelessDataTypeDataVersion

  val avro = "org.apache.avro" % "avro" % "1.7.7"
}