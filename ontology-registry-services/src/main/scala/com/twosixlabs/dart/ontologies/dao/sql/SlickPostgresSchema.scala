package com.twosixlabs.dart.ontologies.dao.sql

import com.twosixlabs.dart.ontologies.api.OntologyArtifact
import slick.lifted.ProvenShape

import java.time.OffsetDateTime

object SlickPostgresSchema extends PgSlickProfile {

    import api._

    class OntologyRegistryTable( tag : Tag ) extends Table[ OntologyArtifact ]( tag : Tag, "ontology_registry" ) {

        //  id, tenant, version, staging_version, ontology, tags, timestamp

        def id : Rep[ String ] = column[ String ]( "id" )

        def tenant : Rep[ String ] = column[ String ]( "tenant" )

        def version : Rep[ Int ] = column[ Int ]( "version" )

        def stagingVersion : Rep[ Int ] = column[ Int ]( "staging_version" )

        def ontology : Rep[ String ] = column[ String ]( "ontology" )

        def tags : Rep[ List[ String ] ] = column[ List[ String ] ]( "tags", O.Default( Nil ) )

        def timestamp : Rep[ OffsetDateTime ] = column[ OffsetDateTime ]( "timestamp" )

        override def * : ProvenShape[ OntologyArtifact ] =
            (id, tenant, version, stagingVersion, ontology, tags, timestamp) <>
            (OntologyArtifact.tupled, OntologyArtifact.unapply)
    }

    lazy val ontologyRegistryQuery = TableQuery[ OntologyRegistryTable ]

    lazy val ontologyRegistryInsert =
        ontologyRegistryQuery
          .map( t => (t.id, t.tenant, t.version, t.stagingVersion, t.ontology, t.tags, t.timestamp) )

    lazy val ontologyRegistryInsertWithoutTags =
        ontologyRegistryQuery
          .map( t => (t.id, t.tenant, t.version, t.stagingVersion, t.ontology, t.timestamp) )

}
