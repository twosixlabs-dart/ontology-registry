package com.twosixlabs.dart.configuration

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.twosixlabs.dart.arangodb.tables.CanonicalDocsTable
import com.twosixlabs.dart.arangodb.{ Arango, ArangoConf }
import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.auth.tenant.indices.ArangoCorpusTenantIndex
import com.twosixlabs.dart.ontologies.OntologyRegistryService
import com.twosixlabs.dart.ontologies.api.OntologyArtifactTable
import com.twosixlabs.dart.ontologies.dao.sql.{ PgSlickProfile, SqlOntologyArtifactTable }
import com.twosixlabs.dart.ontologies.providers.KafkaProvider
import com.twosixlabs.dart.sql.SqlClient
import com.typesafe.config.Config

import scala.util.Try
import com.twosixlabs.dart.ontologies.dao.sql.PgSlickProfile.api._

import scala.concurrent.duration.DurationDouble


trait ConfigConstructor[ C, T ] {
    def buildFromConfig( config : C ) : T
}

object ConfigConstructors {

    /**
     * The idea here is that if you have a set of implicit ConfigConstructors in scope,
     * you can just call config.build[ Result ] where Result is the type being built
     */
    implicit class FromConfig[ C ]( config : C ) {
        def build[ T ]( implicit constructor : ConfigConstructor[ C, T ] ) : T = constructor.buildFromConfig( config )
    }

    implicit object AuthFromConfig extends ConfigConstructor[ Config, AuthDependencies ] {
        override def buildFromConfig( config : Config ) : AuthDependencies = {
            SecureDartController.authDeps( config )
        }
    }

    implicit object DataSourceFromConfig extends ConfigConstructor[ Config, ComboPooledDataSource ] {
        override def buildFromConfig( config : Config ) : ComboPooledDataSource = {
            val ds = new ComboPooledDataSource()
            ds.setDriverClass( config.getString( "postgres.driver.class" ) )
            val pgHost = config.getString( "postgres.host" )
            val pgPort = config.getInt( "postgres.port" )
            val pgDb = config.getString( "postgres.database" )
            ds.setJdbcUrl( s"jdbc:postgresql://$pgHost:$pgPort/$pgDb" )
            ds.setUser( config.getString( "postgres.user" ) )
            ds.setPassword( config.getString( "postgres.password" ) )
            Try( config.getInt( "postgres.minPoolSize" ) ).foreach( v => ds.setMinPoolSize( v ) )
            Try( config.getInt( "postgres.acquireIncrement" ) ).foreach( v => ds.setAcquireIncrement( v ) )
            Try( config.getInt( "postgres.maxPoolSize" ) ).foreach( v => ds.setMaxPoolSize( v ) )

            ds
        }
    }

    implicit object DatabaseFromConfig extends ConfigConstructor[ Config, Database ] {
        override def buildFromConfig( config : Config ) : Database = {
            val ds = config.build[ ComboPooledDataSource ]
            val maxConns = Try( config.getInt( "postgres.max.connections" ) ).toOption

            Database.forDataSource( ds, maxConns )
        }
    }

    implicit object SqlClientFromConfig extends ConfigConstructor[ Config, SqlClient ] {
        override def buildFromConfig( config : Config ) : SqlClient = {
            new SqlClient( config.build[ ComboPooledDataSource ] )
        }
    }

    implicit object OntologyArtifactTableFromConfig extends ConfigConstructor[ Config, OntologyArtifactTable ] {
        override def buildFromConfig( config : Config ) : OntologyArtifactTable = {
            new SqlOntologyArtifactTable( config.build[ Database ],
                                          config.getDouble( "postgres.timeout.minutes" ).minutes,
                                          scala.concurrent.ExecutionContext.global )
        }
    }

    implicit object OntologyRegistryServiceFromConfig extends ConfigConstructor[ Config, OntologyRegistryService ] {
        override def buildFromConfig( config : Config ) : OntologyRegistryService = {
            new OntologyRegistryService( config.build[ OntologyArtifactTable ] )
        }
    }

    implicit object ArangoFromConfig extends ConfigConstructor[ Config, Arango ] {
        override def buildFromConfig( config : Config ) : Arango = {
            new Arango( ArangoConf(
                host = config.getString( "arangodb.host" ),
                port = config.getInt( "arangodb.port" ),
                database = config.getString( "arangodb.database" )
                ) )
        }
    }

    implicit object CanonicalDocsFromConfig extends ConfigConstructor[ Config, CanonicalDocsTable ] {
        override def buildFromConfig( config : Config ) : CanonicalDocsTable = {
            val arango = config.build[ Arango ]
            new CanonicalDocsTable( arango )
        }
    }

    implicit object TenantIndexFromConfig extends ConfigConstructor[ Config, ArangoCorpusTenantIndex ] {
        override def buildFromConfig( config : Config ) : ArangoCorpusTenantIndex = {
            ArangoCorpusTenantIndex( config.build[ Arango ] )
        }
    }

    implicit object KafkaProviderFromConfig extends ConfigConstructor[ Config, KafkaProvider ] {
        override def buildFromConfig( config : Config ) : KafkaProvider = {
            new KafkaProvider( config.getConfig( "kafka" ) )
        }
    }

    //    implicit object OperationsFromConfig extends ConfigConstructor[ Config, PipelineStatusUpdateClient ] {
    //        override def buildFromConfig( config : Config ) : PipelineStatusUpdateClient = {
    //            val engine = "postgresql"
    //            val host = config.getString( "postgres.host" )
    //            val port = config.getInt( "postgres.port" )
    //            val user = config.getString( "postgres.user" )
    //            val password = config.getString( "postgres.password" )
    //            val name = config.getString( "postgres.database" )
    //
    //            val client = SqlClient.newClient( engine, name, host, port, Some( user ), Some( password ) )
    //
    //            new SqlPipelineStatusUpdateClient( client, "pipeline_status" )
    //        }
    //    }

}
