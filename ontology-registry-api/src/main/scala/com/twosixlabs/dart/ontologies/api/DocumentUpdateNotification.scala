package com.twosixlabs.dart.ontologies.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.twosixlabs.cdr4s.json.dart.ThinCdrDocumentDto

import scala.beans.BeanProperty

case class TenantOntologyMapping( @BeanProperty @JsonProperty( "tenant" ) tenant : String,
                                  @BeanProperty @JsonProperty( "ontology" ) ontology : String )

case class DocumentUpdateNotification( @BeanProperty @JsonProperty( "document" ) document : Option[ ThinCdrDocumentDto ],
                                       @BeanProperty @JsonProperty( "ontologies" ) ontologies : Set[ TenantOntologyMapping ] )
