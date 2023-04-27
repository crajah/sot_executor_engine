import scala.language.postfixOps
import com.amazonaws.regions.{Region, Regions}

lazy val globalResources = file("config")

lazy val `sot-engine-core` = (project in file("./sot-engine-core"))
  .settings(
    Common.settings,
    Common.macroSettings,
    unmanagedResourceDirectories in Compile += globalResources
  )

lazy val `sot-macros` = (project in file("./sot-macros"))
  .dependsOn(`sot-engine-core`)
  .settings(
    Common.settings,
    Common.macroSettings,
    unmanagedResourceDirectories in Compile += globalResources
  )

lazy val `sot-executor` = (project in file("./sot-executor"))
  .dependsOn(`sot-macros`)
  .settings(
    Common.settings,
    Common.macroSettings,
    unmanagedResourceDirectories in Compile += globalResources
  )

lazy val `sot` = (project in file("."))
  .aggregate(`sot-engine-core`, `sot-macros`, `sot-executor`)
  .settings(
    name := "sot-executor-engine",
    Common.settings,
    s3region := Region.getRegion(Regions.EU_WEST_2),
    publishTo := {
      val prefix = if (isSnapshot.value) "snapshot" else "release"
      Some(s3resolver.value(s"Parallel AI $prefix S3 bucket", s3(s"$prefix.repo.parallelai.com")) withMavenPatterns)
    }
  )

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".proto" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case "unwanted.txt" => MergeStrategy.discard
  case "plugin.xml" => MergeStrategy.discard
  case "parquet.thrift" => MergeStrategy.discard

  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}