import sbt._, Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "io.github.hunam",
    version := "1.0",
    scalaVersion := "2.10.2",
    resolvers += Resolver.sonatypeRepo("snapshots")
  )
}

object ScalaAdjectives extends Build {
  import BuildSettings._

  lazy val root = Project(
    "sample-externaldep",
    file("."),
    settings = buildSettings
  ) dependsOn(adjectives)


  lazy val adjectives = ProjectRef(uri("git://github.com/hunam/scala-adjectives#v1.0"), "scala-adjectives")

}
