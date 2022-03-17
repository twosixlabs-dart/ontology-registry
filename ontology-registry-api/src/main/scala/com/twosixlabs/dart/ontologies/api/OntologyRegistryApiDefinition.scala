//package com.twosixlabs.dart.ontologies.api
//
//import com.twosixlabs.dart.rest.scalatra.models.FailureResponse
//import sttp.tapir._
//import sttp.tapir.generic.auto._
//import io.circe.generic.auto._
//import sttp.tapir.json.circe._
//import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
//
//import scala.concurrent.Future
//
//object OntologyRegistryApiDefinition extends DartServiceApiDefinition {
//    /**
//     * Name of the service
//     */
//    override val serviceName : String = "ontology-repository"
//
//    /**
//     * Slug to be used as base path of the service if different from servicename
//     */
//    override val servicePathName : Option[String ] = Some( "ontologies" )
//
//    val getAllKeys : Endpoint[ (String, Unit), (FailureResponse, Unit), Map[String, Int ], Any ] = endpoint
//      .description( "Get all keys" )
//      .get
//      .out( jsonBody[ Map[ String, Int ] ].description( "Map of existing keys to number of versions for each key" ) )
//      .addToDart()
//
//    val getKey : Endpoint[ (String, (Option[ FailureResponse ], Option[ Boolean ], String)), (FailureResponse, Unit), Seq[DataVersion ], Any ] = endpoint
//      .description( "View versions of keys" )
//      .get
//      .in( jsonBody[ Option[ FailureResponse ] ].description( "optional json payload" ) )
//      .in( query[ Option[ Boolean ] ]( "includeData" ).description( "Include ontology data in response, in addition to version metadata" ) )
//      .in( path[ String ].name( "key" ).validate( Validator.pattern( "[a-zA-Z0-9-]+" ) ) )
//      .out( jsonBody[ Seq[ OntologyArtifact ] ].description( "Metadata of all versions in queried key, with or without ontology data" ) )
//      .addToDart( notFoundErr( "Key not found" ), badRequestErr( "invalid key or includeData parameter" ) )
//
//}
