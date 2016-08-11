import ScalaxbKeys.{packageName => sxbPackageName, _}
// TODO do we need this?
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

lazy val dispatchV = "0.11.2" // change this to appropriate dispatch version
lazy val scalatestVersion = "3.0.0"
lazy val scalmockVersion = "3.2.2"
val logbackVersion = "1.1.7"
val args4jVersion = "2.33"
val grizzledVersion = "1.1.0"

lazy val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
lazy val scalaParser = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
lazy val dispatch = "net.databinder.dispatch" %% "dispatch-core" % dispatchV

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  organization := "uk.co.mailmemydoc",
  version := "0.1-SNAPSHOT",
  scalacOptions := Seq("-feature", "-deprecation", "-Xfatal-warnings")
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .aggregate(docmaillibrary)
  .dependsOn(docmaillibrary)
  .settings(commonSettings: _*)
  .settings(name := """mail-me-my-doc""")
  .settings(

    unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "main" / "scala",

    libraryDependencies ++= Seq(
      jdbc,
      cache,
      ws,
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test
    )
  )

lazy val docmaillibrary = project.settings(commonSettings: _*)
  .settings(name := "docmaillibrary")
  .settings(scalaxbSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.11" % scalatestVersion % "test",
      "org.scalamock" %% "scalamock-scalatest-support" % scalmockVersion % "test",
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "args4j" % "args4j" % args4jVersion,
      "org.clapper" % "grizzled-slf4j_2.11" % grizzledVersion
    ),
    libraryDependencies ++= Seq(scalaXml, scalaParser, dispatch),
    sxbPackageName in (Compile, scalaxb) := "generated.docmail",
    packageNames in (Compile, scalaxb) := Map(uri("http://schemas.microsoft.com/2003/10/Serialization/") -> "microsoft.serialization"),
    dispatchVersion in (Compile, scalaxb) := dispatchV,
    async in (Compile, scalaxb) := true,
    sourceGenerators in Compile <+= scalaxb in Compile,
    sourceManaged in (Compile, scalaxb) := baseDirectory.value / "src" / "main" / "generated-sources"
    // logLevel in (Compile, scalaxb) := Level.Debug
  )

