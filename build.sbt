name := "akka-streams-kafka-test"
organization := "com.andrewzurn"
scalaVersion := "2.12.1"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "0.14"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.9"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.2" % Runtime

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
dockerBaseImage := "java:openjdk-8-jre"
dockerRepository := Some("awzurn")
