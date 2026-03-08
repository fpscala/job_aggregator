name := "endpoints"

lazy val `endpoints-domain` = project
  .in(file("00-domain"))
  .dependsOn(
    LocalProject("common"),
    LocalProject("support_kafka"),
    LocalProject("support_services"),
  )

lazy val `endpoints-repos` =
  project
    .in(file("01-repos"))
    .settings(
      libraryDependencies ++=
        Seq(
          Dependencies.uz.scala.doobie
        )
    )
    .dependsOn(`endpoints-domain`)

lazy val `endpoints-core` =
  project
    .in(file("02-core"))
    .dependsOn(
      `endpoints-repos`,
      LocalProject("support_redis"),
      LocalProject("support_mailer"),
      LocalProject("support_kafka"),
      LocalProject("support_minio"),
    )

lazy val `endpoints-api` =
  project
    .in(file("03-api"))
    .dependsOn(
      `endpoints-core`
    )

lazy val `endpoints-jobs` =
  project
    .in(file("03-jobs"))
    .dependsOn(
      `endpoints-core`,
      LocalProject("support_jobs"),
    )

lazy val `endpoints-server` =
  project
    .in(file("04-server"))
    .dependsOn(
      `endpoints-api`,
      LocalProject("support_services"),
    )

lazy val `endpoints-runner` =
  project
    .in(file("05-runner"))
    .settings(
      libraryDependencies ++= Seq(
        Dependencies.uz.scala.flyway
      ),
      // JVM options for proper UTF-8 encoding support
      javaOptions ++= Seq(
        "-Dfile.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8",
      ),
      // Compiler options for UTF-8 source encoding
      scalacOptions ++= Seq(
        "-encoding",
        "utf-8",
      ),
    )
    .dependsOn(
      `endpoints-server`,
      `endpoints-jobs`,
    )
    .settings(DockerImagePlugin.serviceSetting("endpoints"))
    .enablePlugins(DockerImagePlugin, JavaAppPackaging, DockerPlugin)

aggregateProjects(
  `endpoints-domain`,
  `endpoints-repos`,
  `endpoints-core`,
  `endpoints-api`,
  `endpoints-jobs`,
  `endpoints-server`,
  `endpoints-runner`,
)
