name := "MvnRepoMiner"

version := "1.0"

scalaVersion := "2.11.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "net.ruippeixotog" %% "scala-scraper" % "0.1.1",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3"
)