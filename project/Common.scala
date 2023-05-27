import sbt._
import sbt.Keys._
import Dependencies._
import ohnosequences.sbt.SbtS3Resolver.autoImport.{s3, s3region, s3resolver}
import com.amazonaws.regions.{Region, Regions}

object Common {
  lazy val macroSettings = Seq(
    addCompilerPlugin(paradise),
    scalacOptions += "-Xplugin-require:macroparadise",
//    scalacOptions += "-Xlog-implicits",
    libraryDependencies += scalameta
  )

  val settings = Seq(
    organization := "parallelai.sot",
    scalaVersion := scala211,
    scalacOptions ++= Seq(
      "-deprecation",           // Emit warning and location for usages of deprecated APIs.
      "-feature",               // Emit warning and location for usages of features that should be imported explicitly.
      "-unchecked",             // Enable additional warnings where generated code depends on assumptions.
      "-Xlint",                 // Enable recommended additional warnings.
      "-Ywarn-adapted-args",    // Warn if an argument list is modified to match the receiver.
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",   // Warn when non-Unit expression results are unused.
      "-Ypartial-unification",
      "-language:postfixOps",
      "-language:higherKinds",
      "-language:existentials"
    ),
    parallelExecution in ThisBuild := false,
    fork in Test := true,
    fork in IntegrationTest := true,
    s3region := Region.getRegion(Regions.EU_WEST_2),
    resolvers ++= Seq[Resolver](
      "Typesafe" at "http://repo.typesafe.com/typesafe/releases",
      "Artima Maven Repository" at "http://repo.artima.com/releases",
      Resolver.sbtPluginRepo("releases"),
      s3resolver.value("Parallel AI S3 Releases resolver", s3("release.repo.parallelai.com")) withMavenPatterns,
      s3resolver.value("Parallel AI S3 Snapshots resolver", s3("snapshot.repo.parallelai.com")) withMavenPatterns
    ),
    resolvers += sbtResolver.value,
    publishTo := {
      val prefix = if (isSnapshot.value) "snapshot" else "release"
      Some(s3resolver.value(s"Parallel AI $prefix S3 bucket", s3(s"$prefix.repo.parallelai.com")) withMavenPatterns)
    }
  )
}