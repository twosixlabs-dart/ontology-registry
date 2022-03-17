package com.twosixlabs.dart.ontologies.api

import com.fasterxml.jackson.annotation.JsonProperty
import sttp.tapir.Schema.annotations.{description, encodedName}

import java.util.UUID

case class OntologySubmission( @JsonProperty( "ontology" )
                               @encodedName( "ontology" )
                               @description( "Ontology in YML format" )
                               ontology : String,
                               @JsonProperty( "tags" )
                               @encodedName( "tags" )
                               @description( "optional keywords or phrases for describing or to help find a given version" )
                               tags : Option[ List[ String ] ] = None ) {
    def toArtifact( tenantId : String ) : OntologyArtifact =
        OntologyArtifact( id = UUID.randomUUID().toString,
                          tenant = tenantId,
                          version = -1,
                          stagingVersion = -1,
                          ontology,
                          tags.getOrElse( List.empty ) )
}
