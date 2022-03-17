package com.twosixlabs.dart.ontologies

import com.twosixlabs.dart.ontologies.api.{OntologyArtifact, OntologyArtifactTable, OntologyRegistry}

import java.util.UUID
import scala.util.{Failure, Success, Try}

class OntologyRegistryService( table : OntologyArtifactTable ) extends OntologyRegistry {

    def commitOntology( artifact : OntologyArtifact ) : Try[ OntologyArtifact ] = {
        val result : Either[ OntologyArtifact, Throwable ] = latest( artifact.tenant ) match {
            case Success( value ) => {
                value match {
                    case Some( existing : OntologyArtifact ) => {
                        Left( artifact.copy( id = generateUuid(), version = existing.version + 1, stagingVersion = 0 ) )
                    }
                    case None => {
                        Left( artifact.copy( id = generateUuid(), version = 1, stagingVersion = 0 ) )
                    }
                }
            }
            case Failure( e : Throwable ) => Right( e )
        }

        doInsert( result )
    }

    def stageOntology( artifact : OntologyArtifact ) : Try[ OntologyArtifact ] = {
        val result : Either[ OntologyArtifact, Throwable ] = {
            latestStaged( artifact.tenant ) match {
                case Success( value ) => {
                    value match {
                        case Some( existing : OntologyArtifact ) => {
                            Left( artifact.copy( id = generateUuid(), stagingVersion = existing.stagingVersion + 1 ) )
                        }
                        case None => {
                            Left( artifact.copy( id = generateUuid(), stagingVersion = 1 ) )
                        }
                    }
                }
                case Failure( e : Throwable ) => Right( e )
            }
        }

        doInsert( result )
    }

    def latest( tenant : String ) : Try[ Option[ OntologyArtifact ] ] = {
        table.getLatestForTenant( tenant )
    }

    def latestStaged( tenant : String ) : Try[ Option[ OntologyArtifact ] ] = {
        table.getLatestForTenant( tenant, useStagedVersion = true )
    }

    def byId( id : String ) : Try[ Option[ OntologyArtifact ] ] = {
        table.getById( id )
    }

    def byVersion( tenant : String, version : Int ) : Try[ Option[ OntologyArtifact ] ] = {
        table.getVersion( tenant, version )
    }

    private def doInsert( result : Either[ OntologyArtifact, Throwable ] ) : Try[ OntologyArtifact ] = {
        result match {
            case Left( value ) => table.insert( value )
            case Right( e ) => Failure( e )
        }
    }

    private def generateUuid( ) : String = UUID.randomUUID().toString

}
