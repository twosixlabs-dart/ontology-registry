package com.twosixlabs.dart.ontologies.dao.sql

import com.twosixlabs.dart.ontologies.api.{OntologyArtifact, OntologyArtifactTable, OntologyRegistryException}
import com.twosixlabs.dart.ontologies.dao.sql.PgSlickProfile.api._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class SqlOntologyArtifactTable( db : Database, timeout : Duration, executionContext : ExecutionContext )
  extends OntologyArtifactTable {

    implicit val ec : ExecutionContext = executionContext

    private lazy val LOG : Logger = LoggerFactory.getLogger( getClass )

    override def insert( artifact : OntologyArtifact ) : Try[ OntologyArtifact ] = {
        val query =
            if ( artifact.tags.nonEmpty ) {
                (SlickPostgresSchema.ontologyRegistryInsert returning SlickPostgresSchema.ontologyRegistryQuery) +=
                (artifact.id, artifact.tenant, artifact.version, artifact.stagingVersion, artifact.ontology, artifact.tags, artifact.timestamp)
            } else {
                (SlickPostgresSchema.ontologyRegistryInsertWithoutTags returning SlickPostgresSchema.ontologyRegistryQuery) +=
                (artifact.id, artifact.tenant, artifact.version, artifact.stagingVersion, artifact.ontology, artifact.timestamp)
            }

        val future = db.run( query )
          .transform {
              case res@Success( _ ) => res
              case Failure( e ) =>
                  if ( LOG.isDebugEnabled() ) LOG.error( s"${e.getClass}", e )
                  Failure( new OntologyRegistryException( s"error inserting ontology artifact i${artifact.tenant}:${artifact.version} - ${e.getClass} : ${e.getMessage}", e ) )
          }

        Try( Await.result( future, timeout ) )
    }

    override def getLatestForTenant( tenant : String, useStagedVersion : Boolean = false ) : Try[ Option[ OntologyArtifact ] ] = {
        val stagingFilter : SlickPostgresSchema.OntologyRegistryTable => Rep[ Boolean ] =
            if ( useStagedVersion ) _.stagingVersion > 0
            else _.stagingVersion === 0

        val query =
            SlickPostgresSchema.ontologyRegistryQuery
              .filter( stagingFilter )
              .filter( _.tenant === tenant )
              .sortBy( t => (t.version.desc, t.stagingVersion.desc) )
              .take( 1 )

        val future : Future[ Option[ OntologyArtifact ] ] =
            db.run( query.result )
              .map( _.headOption )
              .transform {
                  case res@Success( _ ) => res
                  case Failure( e ) =>
                      val message = if ( useStagedVersion ) {
                          s"error getting latest staged ontology version for tenant=${tenant} - ${e.getClass} : ${e.getMessage}"
                      }
                      else {
                          s"error getting latest ontology version for tenant=${tenant} - ${e.getClass} : ${e.getMessage}"
                      }

                      Failure( new OntologyRegistryException( message, e ) )

              }

        Try( Await.result( future, timeout ) )
    }

    override def getVersion( tenant : String, version : Int ) : Try[ Option[ OntologyArtifact ] ] = {
        val query =
            SlickPostgresSchema.ontologyRegistryQuery
              .filter( _.tenant === tenant )
              .filter( _.version === version )

        val future =
            db.run( query.result )
              .map( _.headOption )
              .transform {
                  case res@Success( _ ) => res
                  case Failure( e ) =>
                      if ( LOG.isDebugEnabled() ) LOG.error( s"${e.getClass}", e )
                      Failure( new OntologyRegistryException( s"error looking up ontology version for tenant=${tenant} version=${version} - ${e.getClass} : ${e.getMessage}", e ) )
              }

        Try( Await.result( future, timeout ) )
    }

    override def getById( id : String ) : Try[ Option[ OntologyArtifact ] ] = {
        val query = SlickPostgresSchema.ontologyRegistryQuery.filter( _.id === id )

        val future = db.run( query.result )
          .map( _.headOption )
          .transform {
              case res@Success( _ ) => res
              case Failure( e ) =>
                  if ( LOG.isDebugEnabled() ) LOG.error( s"${e.getClass}", e )
                  throw new OntologyRegistryException( s"error looking up ontology with id=${id} ${e.getMessage}", e )
          }

        Try( Await.result( future, timeout ) )
    }

}
