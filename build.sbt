name := "microservice-akkaStreams"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"  % "2.6.3",
  "com.typesafe.akka" %% "akka-stream" % "2.6.3",
  "com.typesafe.akka" %% "akka-http"   % "10.1.11",
  "com.typesafe.play" %% "play-json" % "2.8.1"
)
