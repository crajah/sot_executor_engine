import scala.language.postfixOps

lazy val IT = config("it") extend Test

lazy val configResources = file("config")

lazy val assembySettings = assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".proto" => MergeStrategy.first
  case "application.conf" => MergeStrategy.first
  case "unwanted.txt" => MergeStrategy.discard
  case "plugin.xml" => MergeStrategy.discard
  case "parquet.thrift" => MergeStrategy.discard

  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

lazy val shadingAssemblySettings = assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("org.objenesis.**" -> "paishade.org.objenesis.@1").inAll,
  ShadeRule.rename("com.esotericsoftware.**" -> "paishade.com.esotericsoftware.@1").inAll
)

lazy val `sot-containers` = (project in file("./sot-containers"))
  .configs(IntegrationTest)
  .settings(
    Common.settings,
    Defaults.itSettings,
    unmanagedResourceDirectories in Compile += configResources
  )

lazy val `sot-engine-core` = (project in file("./sot-engine-core"))
  .dependsOn(`sot-containers` % "it->it;test->test;compile->compile")
  .configs(IntegrationTest)
  .configs(IT)
  .settings(Defaults.itSettings: _*)
  .settings(inConfig(IT)(Defaults.testSettings): _*)
  .settings(
    Common.settings,
    Common.macroSettings,
    unmanagedResourceDirectories in Compile += configResources
  )

lazy val `sot-macros` = (project in file("./sot-macros"))
  .dependsOn(`sot-engine-core`)
  .settings(
    Common.settings,
    Common.macroSettings,
    unmanagedResourceDirectories in Compile += configResources
  )

lazy val `sot-executor` = (project in file("./sot-executor"))
  .dependsOn(`sot-containers` % "it->it;test->test;compile->compile", `sot-engine-core` % "it->it;test->test;compile->compile", `sot-macros`)
  .configs(IntegrationTest)
  .configs(IT)
  .settings(Defaults.itSettings: _*)
  .settings(inConfig(IT)(Defaults.testSettings): _*)
  .settings(
    Common.settings,
    Common.macroSettings,
    assembySettings,
    shadingAssemblySettings,
    unmanagedResourceDirectories in Compile += configResources,
    test in assembly := {},
    assemblyJarName in assembly := "sot_executor_rule.jar"
  )

lazy val `sot` = (project in file("."))
  .aggregate(`sot-containers`, `sot-engine-core`, `sot-macros`, `sot-executor`)
  .configs(IntegrationTest)
  .settings(
    name := "sot-executor-engine",
    version := "0.1.1-SNAPSHOT",
    Common.settings,
    Defaults.itSettings
  )