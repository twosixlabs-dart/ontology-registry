import Dependencies._
import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy

name := "ontology-registry"

scalaVersion := "2.12.7"

/*
   ##############################################################################################
   ##                                                                                          ##
   ##                                  SETTINGS DEFINITIONS                                    ##
   ##                                                                                          ##
   ##############################################################################################
 */

// integrationConfig and wipConfig are used to define separate test configurations for integration testing
// and work-in-progress testing
lazy val IntegrationConfig = config( "integration" ) extend Test
lazy val WipConfig = config( "wip" ) extend Test

lazy val commonSettings = {
    inConfig( IntegrationConfig )( Defaults.testTasks ) ++
    inConfig( WipConfig )( Defaults.testTasks ) ++
    Seq(
        organization := "com.twosixlabs.dart.ontologies",
        scalaVersion := "2.12.7",
        resolvers ++= Seq(
            "Maven Central" at "https://repo1.maven.org/maven2/",
            "JCenter" at "https://jcenter.bintray.com",
            "Local Ivy Repository" at s"file://${System.getProperty( "user.home" )}/.ivy2/local/default" ),
        addCompilerPlugin( "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full ),
        javacOptions ++= Seq( "-source", "1.8", "-target", "1.8" ),
        scalacOptions ++= Seq( "-target:jvm-1.8" ),
        useCoursier := false,
        libraryDependencies ++= scalaTest ++ scalaMock,
//        libraryDependencySchemes += "org.scala-lang.modules" %% "scala-java8-compat" % "always", // TODO: actually resolve this conflict
        dependencyOverrides ++= Seq( "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.10.5",
                                     "com.arangodb" %% "velocypack-module-scala" % "1.2.0",
                                     "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.10.5",
                                     "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.5" ),
        // `sbt test` should skip tests tagged IntegrationTest
        Test / testOptions := Seq( Tests.Argument( "-l", "annotations.IntegrationTest" ) ),
        Test / parallelExecution := false,
        //         `sbt integration:test` should run only tests tagged IntegrationTest
        IntegrationConfig / parallelExecution := false,
        IntegrationConfig / testOptions := Seq( Tests.Argument( "-n", "annotations.IntegrationTest" ) ),
        //         `sbt wip:test` should run only tests tagged WipTest
        WipConfig / testOptions := Seq( Tests.Argument( "-n", "annotations.WipTest" ) ),
        WipConfig / parallelExecution := false )
}

lazy val publishSettings = Seq(
    publishTo := {
	// TODO
	None
    },
    publishMavenStyle := true )

lazy val disablePublish = Seq( publish := {} )

lazy val assemblySettings = Seq(
    assembly / assemblyMergeStrategy := {
        case PathList( "META-INF", _* ) => MergeStrategy.discard
        case PathList( "reference.conf" ) => MergeStrategy.concat
        case _ => MergeStrategy.last
    },
    assembly / test := {},
    Compile / run / mainClass := Some( "Main" ) )


/*
   ##############################################################################################
   ##                                                                                          ##
   ##                                  PROJECT DEFINITIONS                                     ##
   ##                                                                                          ##
   ##############################################################################################
 */

lazy val root = ( project in file( "." ) )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .aggregate( ontologyRegistryApi,
              ontologyRegistryServices,
              ontologyRegistryControllers,
              ontologyRegistryMicroservice )
  .settings( name := "ontology-registry", disablePublish )

lazy val ontologyRegistryApi = ( project in file( "ontology-registry-api" ) )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings( commonSettings,
             libraryDependencies ++= tapir ++ dartRestCommons ++ dartCommons ++ cdr4s,
             publishSettings )

lazy val ontologyRegistryServices = ( project in file( "ontology-registry-services" ) )
  .configs( IntegrationConfig, WipConfig )
  .dependsOn( ontologyRegistryApi )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings( commonSettings,
             libraryDependencies ++= database ++ dartCommons ++ kafka ++ dartAuthCommons,
             publishSettings )

lazy val ontologyRegistryControllers = ( project in file( "ontology-registry-controllers" ) )
  .configs( IntegrationConfig, WipConfig )
  .dependsOn( ontologyRegistryApi, ontologyRegistryServices )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings( commonSettings,
             libraryDependencies ++= scalatra ++ tapir ++ dartRestCommons ++ dartAuthCommons ++ dartCommons,
             publishSettings )

lazy val ontologyRegistryMicroservice = ( project in file( "ontology-registry-microservice" ) )
  .configs( IntegrationConfig, WipConfig )
  .enablePlugins( JavaAppPackaging )
  .dependsOn( ontologyRegistryControllers )
  .settings( commonSettings,
             assemblySettings,
             libraryDependencies ++= kafka ++ scalatra ++ database ++ dartAuthCommons ++ dartRestCommons ++ arangoRepo,
             disablePublish )
