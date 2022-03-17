package com.twosixlabs.dart.ontologies.api

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import com.twosixlabs.dart.utils.{DatesAndTimes => DAT}
import sttp.tapir.Schema.annotations.{description, encodedName, validate}
import sttp.tapir.Validator

import java.time.OffsetDateTime

case class OntologyArtifact( @JsonProperty( "id" )
                             @encodedName( "id" )
                             id : String,
                             @JsonProperty( "tenant_id" )
                             @encodedName( "tenant_id" )
                             tenant : String,
                             @JsonProperty( "version" )
                             @encodedName( "version" )
                             @description( "version of the data as a 1-based integer index" )
                             @validate( Validator.min( 1 ) )
                             version : Int,
                             @JsonProperty( "staging_version" )
                             @encodedName( "staging_version" )
                             @description( "version of the data as a 1-based integer index (0 if not a staged version)" )
                             stagingVersion : Int = 0,
                             @JsonProperty( "ontology" )
                             @encodedName( "ontology" )
                             @description( "Ontology in YML format" )
                             ontology : String,
                             @JsonProperty( "tags" )
                             @encodedName( "tags" )
                             @description( "optional keywords or phrases for describing or to help find a given version" )
                             tags : List[ String ] = List(),
                             @JsonProperty( "timestamp" )
                             @encodedName( "timestamp" )
                             @description( "when this version was saved" )
                             timestamp : OffsetDateTime = DAT.timeStamp ) {

    @JsonIgnore
    def isStaged( ) : Boolean = stagingVersion > 0
}

