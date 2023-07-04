ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

// dependency version
val enumeratumVersion = "1.7.2"
val tigerSDKVersion = "2.0.4"
val catsEffectVersion = "3.5.0"
val mysqlConnectorVersion = "8.0.33"
val doobieVersion = "1.0.0-RC4"
val pureConfigVersion = "0.17.4"
val log4catsVersion = "2.6.0"

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
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      "org.typelevel" %% "log4cats-core"    % log4catsVersion,
      "org.typelevel" %% "log4cats-slf4j"   % log4catsVersion
    )
  )

lazy val storage = (project in file("tquant-storage"))
  .settings(
    name := "tquant-storage",
    libraryDependencies ++= Seq(
      // https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
      // "com.mysql" % "mysql-connector-j" % mysqlConnectorVersion,
      "org.tpolecat" %% "doobie-core"      % doobieVersion,
      "org.tpolecat" %% "doobie-hikari"    % doobieVersion,
      "org.tpolecat" %% "doobie-postgres"  % doobieVersion,
      "org.tpolecat" %% "doobie-scalatest" % doobieVersion % "test"
    )
  )
  .dependsOn(core)

lazy val gateway = (project in file("tquant-gateway"))
  .settings(
    name := "tquant-gateway",
    libraryDependencies ++= Seq(
      // https://mvnrepository.com/artifact/io.github.tigerbrokers/openapi-java-sdk
      "io.github.tigerbrokers" % "openapi-java-sdk" % tigerSDKVersion
    )
  )
  .dependsOn(core, storage)

lazy val algorithm = (project in file("tquant-algorithm"))
  .settings(
    name := "tquant-algorithm"
  )
  .dependsOn(core)

lazy val bootstrap = (project in file("tquant-bootstrap"))
  .settings(
    name := "tquant-bootstrap"
  )
  .dependsOn(core, storage, gateway, algorithm)
