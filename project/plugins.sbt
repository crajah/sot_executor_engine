logLevel := Level.Warn

// Needed for importing the project into Eclipse
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.1.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

resolvers += "Era7 maven releases" at "https://s3-eu-west-1.amazonaws.com/releases.era7.com"
addSbtPlugin("ohnosequences" % "sbt-s3-resolver" % "0.16.0")
