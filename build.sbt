import Dependencies._
import com.amazonaws.regions.{Region, Regions}

name := "SOT"

version := "0.0.1"

lazy val commonSettings = Seq(
  organization := "parallelai.sot",
  scalaVersion := scala211
)


val macwire = "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % Test

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
resolvers += "Typesafe" at "http://repo.typesafe.com/typesafe/releases"
resolvers += Resolver.sbtPluginRepo("releases")

resolvers ++= Seq[Resolver](
  s3resolver.value("Parallel AI S3 Releases resolver", s3("release.repo.parallelai.com")),
  s3resolver.value("Parallel AI S3 Snapshots resolver", s3("snapshot.repo.parallelai.com"))
)

//publishMavenStyle := false
s3region := Region.getRegion(Regions.EU_WEST_2)
publishTo := {
  val prefix = if (isSnapshot.value) "snapshot" else "release"
  Some(s3resolver.value("Parallel AI "+prefix+" S3 bucket", s3(prefix+".repo.parallelai.com")) withMavenPatterns)
}


lazy val macroSettings = Seq(
  addCompilerPlugin(paradise),
  scalacOptions += "-Xplugin-require:macroparadise"
)

// Macro setting is any module that has macros, or manipulates meta trees
lazy val macroSettingWithDepdendency = macroSettings ++ Seq(libraryDependencies += scalameta)

lazy val `sot-macros` = (project in file("sot-macros"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      scalaTest,
      "io.spray" %% "spray-json" % "1.3.3",
      "parallelai" %% "sot_executor_model" % "0.1.21",
      "com.typesafe" % "config" % "1.3.1"
    ),
    resolvers ++= Seq[Resolver](
      s3resolver.value("Parallel AI S3 Releases resolver", s3("release.repo.parallelai.com")) withMavenPatterns,
      s3resolver.value("Parallel AI S3 Snapshots resolver", s3("snapshot.repo.parallelai.com")) withMavenPatterns
    ),
    macroSettingWithDepdendency
  )

lazy val `sot-executor` = (project in file("sot-executor"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(scalaTest),
    macroSettings,
    resolvers += Resolver.bintrayRepo("beyondthelines", "maven")
  ).dependsOn(`sot-macros`)

lazy val `sot` = (project in file("."))
  .aggregate(`sot-executor`, `sot-macros`)


