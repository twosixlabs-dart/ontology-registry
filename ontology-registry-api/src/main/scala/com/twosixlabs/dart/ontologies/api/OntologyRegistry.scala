package com.twosixlabs.dart.ontologies.api

import scala.util.Try

trait OntologyRegistry {

    def commitOntology( artifact : OntologyArtifact ) : Try[ OntologyArtifact ]

    def stageOntology( artifact : OntologyArtifact ) : Try[ OntologyArtifact ]

    def latest( tenant : String ) : Try[ Option[ OntologyArtifact ] ]

    def latestStaged( tenant : String ) : Try[ Option[ OntologyArtifact ] ]

    def byId( id : String ) : Try[ Option[ OntologyArtifact ] ]

    def byVersion( tenant : String, version : Int ) : Try[ Option[ OntologyArtifact ] ]


}
