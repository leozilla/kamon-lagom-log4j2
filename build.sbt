import java.io

import com.typesafe.sbt.packager.docker._
import sbt.Def

organization in ThisBuild := "at.leonhart"
version in ThisBuild := "1.0.1"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.8"
scalacOptions += "-Ypartial-unification"

lazy val commonServiceSettings = commonSettings

val serviceLocator = "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % "1.0.0"
val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
val kamonBundle = "io.kamon" %% "kamon-bundle" % "2.0.2"
val kamonPrometheus = "io.kamon" %% "kamon-prometheus" % "2.0.0"
val kamonZipkin = "io.kamon" %% "kamon-zipkin" % "2.0.0"

resolvers in ThisBuild += Resolver.bintrayRepo("lunaryorn", "maven")
resolvers in ThisBuild += Resolver.jcenterRepo

lazy val root = (project in file("."))
  .aggregate(
    `sample-service`,
    `sample-play-app`,
  )
  .settings(skip in publish := true)

lazy val `sample-service` = (project in file("sample-service"))
  .enablePlugins(LagomScala, LagomLog4j2, JavaAgent)
  .disablePlugins(LagomLogback)
  .settings(
    commonServiceSettings("sample-service"),
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceJdbc,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      serviceLocator,
    )
  )
  .settings(lagomForkedTestSettings)

lazy val `sample-play-app` = (project in file("sample-play-app"))
  .enablePlugins(PlayScala, LagomPlay, LagomLog4j2, JavaAgent)
  .disablePlugins(LagomLogback, PlayLogback)
  .settings(
    commonServiceSettings("sample-play-app"),
    libraryDependencies ++= Seq(
      lagomScaladslServer,
      lagomScaladslTestKit,
      serviceLocator,
    ),
    excludeDependencies += "ch.qos.logback" % "logback-classic"
  )

lagomKafkaPort in ThisBuild := 9092
lagomKafkaZookeeperPort in ThisBuild := 2181

lagomCassandraEnabled in ThisBuild := false

lazy val commonSettings = Seq(
  scalacOptions ++= Seq("-encoding", "utf-8", "-Ypartial-unification"),
  libraryDependencies ++= Seq(
    scalaLogging,
    kamonBundle,
    kamonPrometheus,
    kamonZipkin
  )
)

def commonServiceSettings(module: String): Seq[Def.Setting[_]] =
  commonSettings ++ Seq(
    javaOptions in Universal ++= Seq(
      // LOG4J2
      "-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector", // make all loggers async
      "-Dlog4j2.asyncQueueFullPolicy=Discard", // discard log events lower then 'log4j2.discardThreshold' when logger queue is full -> important to never block the application threads
      "-Dlog4j2.discardThreshold=ERROR" // drop ERROR, WARN, INFO, DEBUG, TRACE events when queue is full. To ensure application threads are never blocked when log queue is full.
    )
  )
