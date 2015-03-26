import android.Keys._

android.Plugin.androidBuild

platformTarget in Android := "android-18"

name := "mazda_connector"

javacOptions in Compile ++= Seq("-source", "1.6", "-target", "1.6")

scalacOptions in Compile ++= Seq("-feature", "-unchecked", "-deprecation", "-Xlint")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.2" % "test"

libraryDependencies += "com.squants"  %% "squants"  % "0.4.2"

libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.0"

libraryDependencies += "org.spire-math" %% "spire" % "0.9.0"

proguardOptions in Android ++= Seq(
    "-dontwarn de.robv.android.xposed.**",
    "-dontwarn android.app.**",
    "-dontwarn android.content.**",
    "-dontobfuscate",
    "-keep,includedescriptorclasses class us.insolit.connector.** { *; }",
    "-keep,includedescriptorclasses class de.robv.android.xposed.** { *; }"
)
