name := "bitcoin-akka-node"

organization := "oohish.com"

version := "1.0.1-SNAPSHOT"

scalaVersion := "2.10.3"

scalacOptions += "-feature"

scalacOptions += "-deprecation"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.2.4",
    "joda-time" % "joda-time" % "2.3",
    "org.joda" % "joda-convert" % "1.5",
    "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
    "org.reactivemongo" %% "reactivemongo" % "0.10.0",
    "com.typesafe.play" %% "play" % "2.2.2",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2"
)