package com.twosixlabs.dart.ontologies

import scala.util.Try

trait OntologyUpdatesNotifier {
    final type TenantId = String
    final type OntologyId = String

    def update( tenantId : TenantId, ontologyId : OntologyId ) : Try[ Boolean ]

    def update( tenantOntologies : Map[ TenantId, OntologyId ] ) : Map[ TenantId, Try[ Boolean ] ] = {
        tenantOntologies map {
            case (tenantId, ontologyId) =>
                tenantId -> update( tenantId, ontologyId )
        }
    }

}

object OntologyUpdatesNotifier {

    class OntologyNotificationException( msg : String, cause : Throwable = null )
      extends Exception( s"Failure to notify: $msg", cause )

    class ServiceUnreachableException( val serviceName : String, cause : Throwable = null )
      extends Exception( s"unable to reach ${serviceName}", cause )

}
