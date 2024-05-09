ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "http4s-bug"
  )
  .enablePlugins(BuildInfoPlugin, DockerPlugin, JavaAppPackaging)
  .settings(buildSettings("http4s_bug", "0.0.1") *)
  .settings(dockerPublishSettings *)


val http4sVersion = "1.0.0-M41"
val circeVersion = "0.14.7"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion,
  "org.typelevel" %% "log4cats-core" % "2.3.1",
  "org.typelevel" %% "log4cats-slf4j" % "2.3.1",
  "ch.qos.logback" % "logback-classic" % "1.4.12",
)


def buildSettings(module: String, moduleVersion: String = "0.0.1") = Seq(
  version := moduleVersion,
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := s"$module",
  buildInfoOptions += BuildInfoOption.BuildTime
)

def dockerPublishSettings = {
  Seq[Setting[?]](
    Docker / maintainer := "http4s-bug",
    dockerBaseImage := sys.env.getOrElse("BASE_IMAGE", "openjdk:21-jdk-slim"),
    dockerRepository := sys.env
      .get("DOCKER_REGISTRY")
      .orElse(None),
    dockerUpdateLatest := false,
    dockerExposedPorts ++= Seq(8080)
  )
}

Global / onChangedBuildSource := ReloadOnSourceChanges