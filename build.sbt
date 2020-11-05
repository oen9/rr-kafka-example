ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "0.1"
ThisBuild / organization := "oen9"
ThisBuild / organizationName := "oen9"

val Ver = new {
  val circe     = "0.13.0"
  val http4s    = "0.21.8"
  val logback   = "1.2.3"
  val zio       = "1.0.3"
  val zioConfig = "1.0.0-RC29"
}

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.typelevel"  %% "cats-core"           % "2.2.0",
    "dev.zio"        %% "zio"                 % Ver.zio,
    "dev.zio"        %% "zio-interop-cats"    % "2.1.4.0",
    "dev.zio"        %% "zio-logging-slf4j"   % "0.4.0",
    "dev.zio"        %% "zio-config"          % Ver.zioConfig,
    "dev.zio"        %% "zio-config-magnolia" % Ver.zioConfig,
    "dev.zio"        %% "zio-config-typesafe" % Ver.zioConfig,
    "dev.zio"        %% "zio-streams"         % Ver.zio,
    "dev.zio"        %% "zio-kafka"           % "0.13.0",
    "org.json4s"     %% "json4s-jackson"      % "3.6.10",
    "ch.qos.logback" % "logback-classic"      % Ver.logback
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.2"
  ),
  scalacOptions ++= Seq(
    "-Xlint",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:higherKinds",
    "-Ymacro-annotations",
    "-Ywarn-unused:imports"
  ),
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.11.0").cross(CrossVersion.full)),
  dockerExposedPorts := Seq(8080),
  dockerBaseImage := "oracle/graalvm-ce:20.2.0-java11"
)

lazy val shared = (project in file("shared"))
  .settings(
    name := "shared",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-parser"         % Ver.circe,
      "io.circe" %% "circe-generic-extras" % Ver.circe,
      "io.circe" %% "circe-generic"        % Ver.circe,
      "io.circe" %% "circe-literal"        % Ver.circe,
      "dev.zio"  %% "zio-kafka"            % "0.13.0"
    )
  )

lazy val gateway = (project in file("gateway"))
  .settings(
    name := "gateway",
    commonSettings,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Ver.http4s,
      "org.http4s" %% "http4s-dsl"          % Ver.http4s,
      "org.http4s" %% "http4s-blaze-client" % Ver.http4s
    )
  )
  .dependsOn(shared)
  .enablePlugins(JavaAppPackaging)

lazy val microHandler = (project in file("micro-handler"))
  .settings(
    name := "micro-handler",
    commonSettings,
    libraryDependencies ++= Seq(
      )
  )
  .dependsOn(shared)
  .enablePlugins(JavaAppPackaging)
