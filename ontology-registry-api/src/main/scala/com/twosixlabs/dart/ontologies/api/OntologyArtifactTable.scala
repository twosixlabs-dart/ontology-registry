package com.twosixlabs.dart.ontologies.api

import scala.util.Try

trait OntologyArtifactTable {

    protected val table : String = "ontology_registry"

    @throws[ OntologyRegistryException ]
    def insert( artifact : OntologyArtifact ) : Try[ OntologyArtifact ]

    @throws[ OntologyRegistryException ]
    def getLatestForTenant( tenant : String, useStagedVersion : Boolean = false ) : Try[ Option[ OntologyArtifact ] ]

    @throws[ OntologyRegistryException ]
    def getVersion( tenant : String, version : Int ) : Try[ Option[ OntologyArtifact ] ]

    @throws[ OntologyRegistryException ]
    def getById( id : String ) : Try[ Option[ OntologyArtifact ] ]

}
