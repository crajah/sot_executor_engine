import sbt._
import sbt.Keys._
import ohnosequences.sbt.SbtS3Resolver.autoImport._
import Dependencies._

object Common {
  lazy val macroSettings = Seq(
    addCompilerPlugin(paradise),
    scalacOptions += "-Xplugin-require:macroparadise",
    libraryDependencies += scalameta
  )

  val settings = Seq(
    version := "0.1.1-SNAPSHOT",
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
      "-language:postfixOps",
      "-language:higherKinds",
      "-language:existentials"
    ),
    parallelExecution in ThisBuild := false,
    fork in (Test, run) := true,
    resolvers ++= Seq[Resolver](
      "Typesafe" at "http://repo.typesafe.com/typesafe/releases",
      "Artima Maven Repository" at "http://repo.artima.com/releases",
      Resolver.sbtPluginRepo("releases"),
      s3resolver.value("Parallel AI S3 Releases resolver", s3("release.repo.parallelai.com")) withMavenPatterns,
      s3resolver.value("Parallel AI S3 Snapshots resolver", s3("snapshot.repo.parallelai.com")) withMavenPatterns
    )
  )
}