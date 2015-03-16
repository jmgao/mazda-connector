import android.Keys._

android.Plugin.androidBuild

platformTarget in Android := "android-17"

name := "mazda_connector"

javacOptions in Compile ++= Seq("-source", "1.6", "-target", "1.6")

scalacOptions in Compile ++= Seq("-feature")

libraryDependencies += "com.google.guava" % "guava" % "18.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.2" % "test"
