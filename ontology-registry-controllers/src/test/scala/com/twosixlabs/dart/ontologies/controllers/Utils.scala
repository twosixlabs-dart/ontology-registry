package com.twosixlabs.dart.ontologies.controllers

import com.twosixlabs.dart.json.JsonFormat
import com.twosixlabs.dart.ontologies.api.OntologyArtifact

object Utils {

    def toJson( artifact : OntologyArtifact ) : String = {
        JsonFormat.marshalFrom( artifact ).get
    }

    def fromJson( json : String ) : OntologyArtifact = {
        JsonFormat.unmarshalTo[ OntologyArtifact ]( json, classOf[ OntologyArtifact ] ).get
    }

}
