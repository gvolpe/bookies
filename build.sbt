name := "code-challenge"

organization := "bookies"

scalaVersion := "2.13.1"

scalacOptions += "-Ymacro-annotations"

libraryDependencies ++= Seq(
  compilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
  "co.fs2"        %% "fs2-core"       % "2.2.2",
  "co.fs2"        %% "fs2-io"         % "2.2.2",
  "com.monovore"  %% "decline-effect" % "1.0.0",
  "io.estatico"   %% "newtype"        % "0.4.3",
  "org.scalatest" %% "scalatest"      % "3.1.0" % Test
)
