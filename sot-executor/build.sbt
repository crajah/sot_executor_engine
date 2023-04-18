libraryDependencies ++= Seq(
  "org.apache.kafka" %% "kafka" % "0.10.1.1"
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri")
    exclude("org.slf4j", "slf4j-simple")
    exclude("org.slf4j", "slf4j-log4j12")
    exclude("log4j", "log4j")
    exclude("org.apache.zookeeper", "zookeeper")
    exclude("com.101tec", "zkclient"),
  "com.twitter" %% "bijection-core" % "0.9.5",
  "com.twitter" %% "bijection-avro" % "0.9.5",
  "com.spotify" %% "scio-core" % "0.4.3",
  "com.spotify" %% "scio-bigtable" % "0.4.3",
  "com.spotify" %% "scio-extra" % "0.4.3",
  "org.apache.avro"  %  "avro"  %  "1.7.7",
  "me.lyh" %% "shapeless-datatype-core" % "0.1.7",
  "me.lyh" %% "shapeless-datatype-datastore_1.3" % "0.1.7",
  "com.google.cloud" % "google-cloud-datastore" % "1.6.0",
  "parallelai" %% "sot_executor_model" % "0.1.20",
  "org.scalacheck" %% "scalacheck" % "1.13.5",
  // Logback with slf4j facade
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "com.spotify" %% "scio-test" % "0.3.5" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.5" % "test",
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.6" % "test"
)

scalacOptions in Compile ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code",
  "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
)

parallelExecution in ThisBuild := false

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".class" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".properties" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".proto" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case "unwanted.txt" => MergeStrategy.discard
  case "plugin.xml" => MergeStrategy.discard
  case "parquet.thrift" => MergeStrategy.discard

  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}