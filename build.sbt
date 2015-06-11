name := "MvnRepoMiner"

version := "1.0"

scalaVersion := "2.11.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "net.ruippeixotog" %% "scala-scraper" % "0.1.1",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.h2database" % "h2" % "1.3.166",
  "org.xerial" % "sqlite-jdbc" % "3.6.20",
  "org.slf4j" % "slf4j-nop" % "1.6.4"
)