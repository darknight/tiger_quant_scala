ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

// dependency version
val enumeratumVersion = "1.7.2"
val tigerSDKVersion = "2.0.4"
val catsEffectVersion = "3.5.0"

// project definition

//lazy val root = (project in file("."))
//  .settings(
//    name := "tiger_quant_scala"
//  )

lazy val core = (project in file("tquant-core"))
  .settings(
    name := "tquant-core",
    libraryDependencies ++= Seq(
      "com.beachape" %% "enumeratum" % enumeratumVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion
    )
  )

lazy val storage = (project in file("tquant-storage"))
  .settings(
    name := "tquant-storage"
  )

lazy val gateway = (project in file("tquant-gateway"))
  .settings(
    name := "tquant-gateway",
    libraryDependencies ++= Seq(
      // https://mvnrepository.com/artifact/io.github.tigerbrokers/openapi-java-sdk
      "io.github.tigerbrokers" % "openapi-java-sdk" % tigerSDKVersion
    )
  )
  .dependsOn(core, storage)
