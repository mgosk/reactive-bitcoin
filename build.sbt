name := "bitcoin-akka-node"

organization := "oohish.com"

version := "1.0.1-SNAPSHOT"

scalaVersion := "2.11.2"

scalacOptions += "-feature"

scalacOptions += "-deprecation"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "yzernik repo" at "http://dl.bintray.com/yzernik/maven/"
 
libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.6",
    "com.oohish" %% "bitcoin-scodec" % "0.1.3"
)