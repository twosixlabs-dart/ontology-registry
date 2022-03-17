package com.twosixlabs.dart.ontologies.controllers

import com.fasterxml.jackson.core.JsonProcessingException
import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.exceptions.{BadQueryParameterException, BadRequestBodyException, GenericServerException, ResourceNotFoundException, ServiceUnreachableException}
import com.twosixlabs.dart.json.JsonFormat
import com.twosixlabs.dart.ontologies.OntologyUpdatesNotifier
import com.twosixlabs.dart.ontologies.api.{OntologyArtifact, OntologyRegistry, OntologySubmission}
import com.twosixlabs.dart.rest.scalatra.DartScalatraServlet
import org.scalatra.{Created, Ok}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success}


class OntologyRegistryController( registry : OntologyRegistry,
                                  notifier : OntologyUpdatesNotifier,
                                  authConfig : AuthDependencies,
                                  serviceNameIn : String = "ontologies" ) extends DartScalatraServlet with SecureDartController {

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    override val serviceName : String = serviceNameIn
    override val secretKey : Option[ String ] = authConfig.secretKey
    override val bypassAuth : Boolean = authConfig.bypassAuth

    setStandardConfig()

    get( "/" ) {
        handleOutput {
            AuthenticateRoute.withUser { _ =>
                if ( params.get( "id" ).isDefined ) getById( params.get( "id" ).get, useStagedVersion = false )
                else if ( params.get( "tenant" ).isDefined ) {
                    val tenant = params( "tenant" )
                    if ( params.get( "version" ).isDefined ) {
                        val version = params( "version" ).toInt
                        registry.byVersion( tenant, version ) match {
                            case Success( result ) => {
                                result match {
                                    case Some( ontology ) => ontology
                                    case None => throw new ResourceNotFoundException( s"no published ontology exists tenant=${tenant} version=${version}" )
                                }
                            }
                            case Failure( e ) => {
                                if ( LOG.isDebugEnabled() ) LOG.error( s"${e.getClass} : ${e.getMessage}", e )
                                throw new GenericServerException( s"internal error looking up published ontology artifacts for tenant=${tenant} version=${version}" )
                            }
                        }
                    } else {
                        registry.latest( tenant ) match {
                            case Success( result ) => {
                                result match {
                                    case Some( ontology ) => ontology
                                    case None => throw new ResourceNotFoundException( s"no ontologies are registered for tenant=${tenant}" )
                                }
                            }
                            case Failure( e ) => {
                                if ( LOG.isDebugEnabled() ) LOG.error( s"${e.getClass} : ${e.getMessage}", e )
                                throw new GenericServerException( s"internal error looking up latest published ontology artifacts for tenant=${tenant}" )
                            }
                        }
                    }
                } else throw new BadQueryParameterException( "either `id` or `tenant` is required to look up a published ontology" )
            }
        }
    }

    get( "/staged" ) {
        handleOutput {
            AuthenticateRoute.withUser { _ =>
                if ( params.get( "id" ).isDefined ) getById( params.get( "id" ).get, useStagedVersion = true )
                else if ( params.get( "tenant" ).isDefined ) {
                    val tenant = params( "tenant" )
                    registry.latestStaged( tenant ) match {
                        case Success( result ) => {
                            result match {
                                case Some( artifact ) => artifact
                                case None => throw new ResourceNotFoundException( s"no staged ontologies are registered for tenant=${tenant}" )
                            }
                        }
                        case Failure( e ) => {
                            if ( LOG.isDebugEnabled() ) LOG.error( s"${e.getClass} : ${e.getMessage}", e )
                            throw new GenericServerException( s"internal error looking up latest staged ontology artifacts for tenant=${tenant}" )
                        }
                    }
                } else {
                    throw new BadQueryParameterException( "either `id` or `tenant` is required to look up a staged ontology" )
                }
            }
        }
    }

    post( "/publish/:tenantId" )( handleOutput( AuthenticateRoute.withUser { _ =>
        val tenantId = params( "tenantId" )
        registry.latestStaged( tenantId ) match {
            case Failure( e ) =>
                LOG.error( s"${e.getClass.getSimpleName}: ${e.getMessage}" )
                throw new ServiceUnreachableException( "ontology registry", Some( s"unable to retrieve staged version of $tenantId" ) )

            case Success( None ) =>
                throw new ResourceNotFoundException( "staged version of tenant", Some( tenantId ) )

            case Success( Some( stagedArtifact : OntologyArtifact ) ) =>
                registry.commitOntology( stagedArtifact ) match {
                    case Failure( e ) =>
                        LOG.error( s"${e.getClass.getSimpleName}: ${e.getMessage}" )
                        throw new ServiceUnreachableException( "ontology registry", Some( s"unable to publish staged ontology for $tenantId" ) )

                    case Success( publishedArtifact ) =>
                        val ontologyId = publishedArtifact.id
                        notifier.update( tenantId, ontologyId ) match {
                            case Success( true ) =>
                                Created()
                            case Success( false ) =>
                                throw new GenericServerException( "notifier returned false" )
                            case Failure( e : OntologyUpdatesNotifier.ServiceUnreachableException ) =>
                                throw new ServiceUnreachableException( e.serviceName, Some( e.getMessage ) )
                            case Failure( e ) =>
                                throw e
                        }
                }
        }
    } ) )

    post( "/stage/:tenantId" )( handleOutput( AuthenticateRoute.withUser { _ =>
        val tenantId = params( "tenantId" )
        val artifactString = request.body

        if ( artifactString.trim.isEmpty ) throw new BadRequestBodyException( "no ontology provided (empty request body)" )

        JsonFormat.unmarshalTo[ OntologySubmission ]( artifactString, classOf[ OntologySubmission ] ) match {
            case Failure( e : JsonProcessingException ) =>
                LOG.error( s"${e.getClass.getSimpleName}: ${e.getMessage}" )
                throw new BadRequestBodyException( "unable to parse ontology submission" )
            case Success( ontologySubmission : OntologySubmission ) =>
                val ontologyArtifact = ontologySubmission.toArtifact( tenantId )
                registry.stageOntology( ontologyArtifact ) match {
                    case Success( stagedArtifact ) =>
                        Created( stagedArtifact )
                    case Failure( e ) =>
                        LOG.error( s"${e.getClass.getSimpleName}: ${e.getMessage}" )
                        throw new ServiceUnreachableException( "ontology registry", Some( "unable to stage submission" ) )
                }
        }
    } ) )

    get( "/health" ) {
        Ok()
    }


    private def getById( id : String, useStagedVersion : Boolean ) : Option[ OntologyArtifact ] = {
        registry.byId( id ) match {
            case Success( record ) => {
                record match {
                    case response@Some( artifact ) => {
                        if ( artifact.isStaged() == useStagedVersion ) response
                        else throw {
                            val ontologyType = if ( useStagedVersion ) "staged" else "published"
                            new ResourceNotFoundException( s"there is no ${ontologyType} ontology with id=${id}" )
                        }
                    }
                    case None => throw new ResourceNotFoundException( s"there is no ontology with id=${id}" )
                }
            }
            case Failure( e : Exception ) => {
                if ( LOG.isDebugEnabled() ) LOG.debug( s"${e.getClass}", e )
                throw new GenericServerException( s"internal error looking up ontology with id=${id}", e )
            }
        }
    }

}
