
name := """tweetmap-workshop"""

version := "0.42"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  ws,
  "org.webjars" % "bootstrap" % "3.1.1",
  "org.webjars" % "jquery" % "2.1.0-2",
  "org.webjars" % "angularjs" % "1.2.16",
  "org.webjars" % "angular-leaflet-directive" % "0.7.6",
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "com.typesafe.akka" %% "akka-contrib" % "2.3.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.3" % "test"
)

// Apply digest calculation and gzip compression to assets
pipelineStages := Seq(digest, gzip)

