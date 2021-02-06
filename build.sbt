import scala.collection.immutable.Seq

name := "zio3d"

version := "0.1"

scalaVersion := "2.13.0"

lazy val zioVersion   = "1.0.4"
lazy val lwjglVersion = "3.2.1"

lazy val os = Option(System.getProperty("os.name", ""))
  .map(_.substring(0, 3).toLowerCase) match {
  case Some("win") => "windows"
  case Some("mac") => "macos"
  case _           => "linux"
}

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "dev.zio"   %% "zio"         % zioVersion,
  "dev.zio"   %% "zio-streams" % zioVersion,
  "org.lwjgl" % "lwjgl"        % lwjglVersion,
  "org.lwjgl" % "lwjgl-opengl" % lwjglVersion,
  "org.lwjgl" % "lwjgl-glfw"   % lwjglVersion,
  "org.lwjgl" % "lwjgl-stb"    % lwjglVersion,
  "org.lwjgl" % "lwjgl-assimp" % lwjglVersion,
  "org.lwjgl" % "lwjgl-nanovg" % lwjglVersion,
  "org.lwjgl" % "lwjgl"        % lwjglVersion classifier s"natives-$os",
  "org.lwjgl" % "lwjgl-opengl" % lwjglVersion classifier s"natives-$os",
  "org.lwjgl" % "lwjgl-glfw"   % lwjglVersion classifier s"natives-$os",
  "org.lwjgl" % "lwjgl-stb"    % lwjglVersion classifier s"natives-$os",
  "org.lwjgl" % "lwjgl-assimp" % lwjglVersion classifier s"natives-$os",
  "org.lwjgl" % "lwjgl-nanovg" % lwjglVersion classifier s"natives-$os"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint:_,-missing-interpolator",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Yrangepos",
  "-target:jvm-1.8"
)

javaOptions ++= {
  if (os == "macos")
    Seq("-XstartOnFirstThread")
  else
    Nil
}

fork in run := true
