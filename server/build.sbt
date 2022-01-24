import play.core.PlayVersion.akkaVersion

Global / onChangedBuildSource := ReloadOnSourceChanges

name := """Entrepeques Appointments"""
organization := "mx.entrepeques"
version := "1.7.0"
scalaVersion := "2.13.3"
scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Xfatal-warnings",
  "-Wconf:cat=lint-multiarg-infix:s",
)

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

resolvers += Resolver.jcenterRepo

libraryDependencies += guice
libraryDependencies += "com.beachape" %% "enumeratum" % "1.6.0"
libraryDependencies += "com.beachape" %% "enumeratum-play-json" % "1.6.0"
libraryDependencies += "com.beachape" %% "enumeratum-slick" % "1.6.0"
libraryDependencies += "com.typesafe.play" %% "play-mailer" % "8.0.1"
libraryDependencies += "com.typesafe.play" %% "play-mailer-guice" % "8.0.1"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.14"
libraryDependencies += "com.typesafe.play" %% "play-slick" % "5.0.0"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0"
libraryDependencies += "io.github.nafg" %% "slick-migration-api" % "0.8.0"
libraryDependencies += "org.jsoup" % "jsoup" % "1.13.1"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "6.3"
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.8"
libraryDependencies += "com.github.t3hnar" %% "scala-bcrypt" % "4.1"
libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.9.1"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
// Adds additional packages into Twirl
// TwirlKeys.templateImports += "mx.entrepeques.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "mx.entrepeques.binders._"
