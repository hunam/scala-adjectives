import sbt._, Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "io.github.hunam",
    version := "1.0",
    scalacOptions ++= Seq(),
    scalaVersion := "2.10.2",
    scalaOrganization := "org.scala-lang",
    resolvers += Resolver.sonatypeRepo("snapshots")
  )
}

object ScalaAdjectives extends Build {
  import BuildSettings._

  lazy val root = Project(
    "root",
    file("."),
    settings = buildSettings
  ) aggregate(sample, adjectives)


  lazy val adjectives = Project(
    "scala-adjectives",
    file("adjectives"),
    settings = buildSettings ++ Seq(
      scalaVersion := "2.10.3-SNAPSHOT",
      scalaOrganization := "org.scala-lang.macro-paradise",
      libraryDependencies <+= (scalaVersion)("org.scala-lang.macro-paradise" % "scala-reflect" % _)
    )
  )

  lazy val sample = Project(
    "scala-adjectives-sample",
    file("sample"),
    settings = buildSettings
  ) dependsOn(adjectives)

}
