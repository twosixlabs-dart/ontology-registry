import sbt._

object Dependencies {

    val scalaTestVersion = "3.2.5"
    val scalaMockVersion = "4.4.0"

    val betterFilesVersion = "3.8.0"

    val dartCommonsVersion = "3.0.30"
    val cdr4sVersion = "3.0.9"
    val dartAuthCommonsVersion = "3.1.3"
    val dartRestCommonsVersion = "3.0.4"

    val tapirVersion = "0.18.3"
    val circeVersion = "0.13.0"
    val upickleVersion = "1.4.2"

    val scalatraVersion = "2.7.1"
    val servletApiVersion = "3.1.0"

    val kafkaVersion = "2.8.0"
    val embeddedKafkaVersion = "3.0.0"

    val postgresDriverVersion = "42.3.1"
    val embeddedPostgresVersion = "1.3.1"
    val h2Version = "1.4.200"
    val c3p0Version = "0.9.5.5"
    val slickVersion = "3.3.3"
    val slickPgVersion = "0.19.7"

    val arangoRepoVersion = "3.0.16"

    val betterFiles = Seq( "com.github.pathikrit" %% "better-files" % betterFilesVersion )

    val scalaTest = Seq( "org.scalatest" %% "scalatest" % scalaTestVersion % "test" )

    val scalaMock = Seq( "org.scalamock" %% "scalamock" % scalaMockVersion % "test" )

    val dartCommons = Seq( "com.twosixlabs.dart" %% "dart-utils" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-sql" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-test-base" % dartCommonsVersion % Test )

    val cdr4s = Seq( "com.twosixlabs.cdr4s" %% "cdr4s-dart-json" % cdr4sVersion )

    val database = Seq( "org.postgresql" % "postgresql" % postgresDriverVersion,
                        "com.h2database" % "h2" % h2Version,
                        "com.mchange" % "c3p0" % c3p0Version,
                        "io.zonky.test" % "embedded-postgres" % embeddedPostgresVersion % Test,
                        "com.typesafe.slick" %% "slick" % slickVersion,
                        "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
                        "com.github.tminglei" %% "slick-pg" % slickPgVersion )

    val dartAuthCommons = Seq( "com.twosixlabs.dart.auth" %% "controllers" % dartAuthCommonsVersion,
                               "com.twosixlabs.dart.auth" %% "arrango-tenants" % dartAuthCommonsVersion )

    val dartRestCommons = Seq( "com.twosixlabs.dart.rest" %% "dart-scalatra-commons" % dartRestCommonsVersion )

    val scalatra = Seq( "org.scalatra" %% "scalatra" % scalatraVersion,
                        "javax.servlet" % "javax.servlet-api" % servletApiVersion,
                        "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % Test )

    val kafka = Seq( "org.apache.kafka" %% "kafka" % kafkaVersion,
                     "org.apache.kafka" % "kafka-clients" % kafkaVersion,
                     "io.github.embeddedkafka" %% "embedded-kafka" % embeddedKafkaVersion % Test,
                     "io.github.embeddedkafka" %% "embedded-kafka-streams" % embeddedKafkaVersion % Test,
                     "jakarta.ws.rs" % "jakarta.ws.rs-api" % "3.0.0" % Test ) //https://github.com/sbt/sbt/issues/361)

    val tapir = Seq( "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
                     "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % tapirVersion exclude("com.typesafe.akka", "akka-stream_2.12") exclude("com.typesafe.akka", "akka-http_2.12"),
                     "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
                     "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
                     "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion )

    val circe = Seq( "io.circe" %% "circe-core" % circeVersion,
                     "io.circe" %% "circe-generic" % circeVersion )

    val upickle = Seq( "com.lihaoyi" %% "upickle" % upickleVersion )

    val arangoRepo = Seq( "com.twosixlabs.dart" %% "dart-arangodb-datastore" % arangoRepoVersion exclude( "org.scala-lang.modules", "scala-java8-compat" ) )

}
