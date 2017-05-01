organization in ThisBuild := "com.ngthanhtrung"

lazy val `sbt-livereload` = project
  .in(file("."))
  .settings(
    sbtPlugin := true,

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-experimental" % "2.0.5",
      "io.monix" %% "monix" % "2.2.3"
    )
  )
