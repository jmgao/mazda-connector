import android.Keys._

android.Plugin.androidBuild

platformTarget in Android := "android-17"

name := "mazda_connector"

javacOptions in Compile ++= Seq("-source", "1.6", "-target", "1.6")

scalacOptions in Compile ++= Seq("-feature", "-unchecked", "-deprecation", "-Xlint")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.2" % "test"

libraryDependencies += "com.squants"  %% "squants"  % "0.4.2"

libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.0"
