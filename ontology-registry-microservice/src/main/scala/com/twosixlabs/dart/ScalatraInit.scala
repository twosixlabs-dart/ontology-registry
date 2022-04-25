package com.twosixlabs.dart

import com.twosixlabs.dart.arangodb.tables.CanonicalDocsTable
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.auth.tenant.indices.ArangoCorpusTenantIndex
import com.twosixlabs.dart.ontologies.OntologyRegistryService
import com.twosixlabs.dart.ontologies.controllers.OntologyRegistryController
import com.twosixlabs.dart.ontologies.kafka.KafkaOntologyUpdatesNotifier
import com.twosixlabs.dart.ontologies.providers.KafkaProvider
import com.twosixlabs.dart.rest.ApiStandards
import com.twosixlabs.dart.rest.scalatra.DartRootServlet
import com.typesafe.config.ConfigFactory
import org.scalatra.LifeCycle
import org.slf4j.{ Logger, LoggerFactory }

import javax.servlet.ServletContext

class ScalatraInit extends LifeCycle {
    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    import com.twosixlabs.dart.configuration.ConfigConstructors._

    val config = ConfigFactory.defaultApplication().resolve()

    private val ontologyRegistry = config.build[ OntologyRegistryService ]
    private val docsTable = config.build[ CanonicalDocsTable ]
    private val tenantIndex = config.build[ ArangoCorpusTenantIndex ]
    private val kafkaProvider = config.build[ KafkaProvider ]
    private val authDeps = config.build[ AuthDependencies ]

    private val notificationTopic = config.getString( "updates.topic" )

    val notifier = new KafkaOntologyUpdatesNotifier( tenantIndex, docsTable, kafkaProvider.newProducer[ String, String ]( config ), notificationTopic )

    val ontologiesController = new OntologyRegistryController( ontologyRegistry, notifier, authDeps )

    val basePath : String = ApiStandards.DART_API_PREFIX_V1 + "/ontologies"

    val rootController = new DartRootServlet( Some( basePath ),
                                              Some( getClass.getPackage.getImplementationVersion ) )

    // Initialize scalatra: mounts servlets
    override def init( context : ServletContext ) : Unit = {
        context.mount( rootController, "/*" )
        context.mount( ontologiesController, basePath + "/" )
    }

    // Scalatra callback to close out resources
    override def destroy( context : ServletContext ) : Unit = {
        super.destroy( context )
    }
}
