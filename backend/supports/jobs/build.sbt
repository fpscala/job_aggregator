import Dependencies.*

name := "jobs"

libraryDependencySchemes += "com.github.alonsodomin.cron4s" %% "cron4s-core" % "always"

libraryDependencies ++= Seq(
  com.github.cron4s,
  eu.timepit.cron4s,
)

dependsOn(LocalProject("common"))
