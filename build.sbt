name := "twitter-analyzer"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.17"
libraryDependencies += "com.twitter" % "hbc-core" % "2.2.0"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.7"
libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % "2.9.7"
libraryDependencies += "com.vdurmont" % "emoji-java" % "4.0.0"
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.25"
libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.5"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.12"
