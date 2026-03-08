import Dependencies.*

ThisBuild / version      := sys.env.getOrElse("IMAGE_TAG", "latest")
ThisBuild / scalaVersion := "2.13.15"
ThisBuild / libraryDependencySchemes += "com.github.alonsodomin.cron4s" %% "cron4s-core" % VersionScheme.Always
// Global encoding settings for proper UTF-8 support
ThisBuild / scalacOptions ++= Seq(
  "-encoding",
  "utf-8",
)

ThisBuild / javaOptions ++= Seq(
  "-Dfile.encoding=UTF-8",
  "-Dsun.jnu.encoding=UTF-8",
)

// Test-specific encoding settings
Test / javaOptions ++= Seq(
  "-Dfile.encoding=UTF-8",
  "-Dsun.jnu.encoding=UTF-8",
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "telegram-job-platform"
  )
  .aggregate(
    common,
    endpoints,
    supports,
  )

lazy val common =
  project
    .in(file("common"))
    .settings(
      name := "common"
    )
    .settings(
      libraryDependencies ++=
        Dependencies.io.circe.all ++
          eu.timepit.refined.all ++
          com.github.pureconfig.all ++
          com.beachape.enumeratum.all ++
          tf.tofu.derevo.all ++
          Dependencies.io.github.apimorphism.telegramium.all ++
          Seq(
            uz.scala.common,
            org.typelevel.cats.core,
            org.typelevel.cats.effect,
            org.typelevel.log4cats,
            ch.qos.logback,
            dev.optics.monocle,
            Dependencies.io.estatico.newtype,
            Dependencies.io.github.jmcardon.`tsec-password`,
            Dependencies.io.scalaland.chimney,
          )
    )
    .dependsOn(LocalProject("support_logback"))

lazy val supports = project
  .in(file("supports"))
  .settings(
    name := "supports"
  )

lazy val endpoints = project
  .in(file("endpoints"))
  .settings(
    name := "endpoints"
  )

addCommandAlias(
  "styleCheck",
  "all scalafmtSbtCheck; scalafmtCheckAll; Test / compile; scalafixAll --check",
)

addCommandAlias(
  "fmtCheck",
  "all scalafmtSbtCheck; scalafmtCheckAll",
)

addCommandAlias("fmtFix", "scalafmtSbt; scalafmtAll")

Global / lintUnusedKeysOnLoad := false
Global / onChangedBuildSource := ReloadOnSourceChanges

val runServer = inputKey[Unit]("Runs server")

runServer := {
  (LocalProject("endpoints-runner") / Compile / run).evaluated
}
