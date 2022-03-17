package com.twosixlabs.dart.ontologies.dao.sql

import annotations.IntegrationTest
import com.twosixlabs.dart.ontologies.api.OntologyArtifact
import com.twosixlabs.dart.sql.SqlClient
import com.twosixlabs.dart.test.base.StandardTestBase3x
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import java.util.UUID
import scala.util.{Failure, Success, Try}
import PgSlickProfile.api._
import com.mchange.v2.c3p0.ComboPooledDataSource
import com.typesafe.config.ConfigFactory

import java.time.{OffsetDateTime, ZoneId, ZoneOffset}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@IntegrationTest
class SqlOntologyTableTestSuite extends StandardTestBase3x with BeforeAndAfterEach with BeforeAndAfterAll {

    val config = ConfigFactory.load( "pg-test.conf" ).resolve()

    val ds = new ComboPooledDataSource()
    ds.setDriverClass( config.getString( "postgres.driver.class" ) )
    val pgHost = config.getString( "postgres.host" )
    val pgPort = config.getInt( "postgres.port" )
    val pgDb = config.getString( "postgres.database" )
    ds.setJdbcUrl( s"jdbc:postgresql://$pgHost:$pgPort/$pgDb" )
    ds.setUser( config.getString( "postgres.user" ) )
    ds.setPassword( config.getString( "postgres.password" ) )
    Try( config.getInt( "postgres.minPoolSize" )  ).foreach( v => ds.setMinPoolSize( v ) )
    Try( config.getInt( "postgres.acquireIncrement" )  ).foreach( v => ds.setAcquireIncrement( v ) )
    Try( config.getInt( "postgres.maxPoolSize" )  ).foreach( v => ds.setMaxPoolSize( v ) )

    val maxConns = Try( config.getInt( "postgres.max.connections" ) ).toOption

    val db = Database.forDataSource( ds, maxConns )

    val table = new SqlOntologyArtifactTable( db, 1.minute, scala.concurrent.ExecutionContext.global )

    override def afterEach( ) : Unit = {
        Await.result( db.run( SlickPostgresSchema.ontologyRegistryQuery.schema.truncate ), 20.seconds )
    }

    "Ontology Table" should "insert and retrieve a record without tags" in {
        val id = UUID.randomUUID.toString

        val expected = OntologyArtifact( id = id, tenant = "tenant-1", version = 1, ontology = "ontology" )

        table.insert( expected ).get
        val result = table.getById( id )

        result match {
            case Success( value ) => {
                value match {
                    case Some( actual ) => {
                        actual.id shouldBe id
                        actual.tenant shouldBe expected.tenant
                        actual.version shouldBe expected.version
                        actual.stagingVersion shouldBe 0
                    }
                    case None => fail()
                }
            }
            case Failure( e ) => fail( s"${e.getClass.getSimpleName} - ${e.getMessage}" )
        }

    }

    "Ontology Table" should "insert and retrieve a record with tags" in {
        val id = UUID.randomUUID.toString

        val expected = OntologyArtifact( id = id, tenant = "tenant-1", version = 1, ontology = "ontology", tags = List( "tag-1" ) )

        table.insert( expected )
        val result = table.getById( id )

        result match {
            case Success( value ) => {
                value match {
                    case Some( actual ) => {
                        actual.id shouldBe id
                        actual.tenant shouldBe expected.tenant
                        actual.version shouldBe expected.version
                        actual.stagingVersion shouldBe 0
                        actual.tags shouldBe List( "tag-1" )
                    }
                    case None => fail()
                }
            }
            case Failure( e ) => fail( e )
        }
    }

    "Ontology Table" should "not find an ontology that does not exist" in {
        val id = UUID.randomUUID.toString

        val result = table.getById( id )

        result match {
            case Success( value ) => {
                value match {
                    case None => succeed
                    case _ => fail()
                }
            }
            case Failure( e ) => fail( e )
        }
    }

    "Ontology Table" should "get the latest published ontology for a tenant" in {
        val expected = OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, ontology = "ontology", tags = List( "tag-1" ) )

        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 1, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, stagingVersion = 1, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( expected )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-2", version = 3, ontology = "ontology", tags = List( "tag-1" ) ) )

        val result = table.getLatestForTenant( expected.tenant )

        result match {
            case Success( value ) => {
                value match {
                    case Some( actual ) => {
                        actual.id shouldBe expected.id
                        actual.tenant shouldBe expected.tenant
                        actual.version shouldBe expected.version
                        actual.stagingVersion shouldBe 0
                        actual.tags shouldBe List( "tag-1" )
                    }
                    case None => fail()
                }
            }
            case Failure( e ) => fail( e )
        }
    }

    "Ontology Table" should "get the latest staged ontology for a tenant" in {
        val expected = OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, stagingVersion = 2, ontology = "ontology", tags = List( "tag-1" ) )

        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 1, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, stagingVersion = 1, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( expected )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-2", version = 3, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-2", version = 3, stagingVersion = 1, ontology = "ontology", tags = List( "tag-1" ) ) )

        val result = table.getLatestForTenant( "tenant-1", useStagedVersion = true )

        result match {
            case Success( value ) => {
                value match {
                    case Some( actual ) => {
                        actual.id shouldBe expected.id
                        actual.tenant shouldBe expected.tenant
                        actual.version shouldBe expected.version
                        actual.stagingVersion shouldBe 2
                        actual.tags shouldBe List( "tag-1" )
                    }
                    case None => fail()
                }
            }
            case Failure( e ) => fail( e )
        }
    }

    "Ontology Table" should "not find a staged version if it does not exist" in {
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 1, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, stagingVersion = 1, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, stagingVersion = 2, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-2", version = 3, ontology = "ontology", tags = List( "tag-1" ) ) )

        val result = table.getLatestForTenant( "tenant-2", useStagedVersion = true )

        result match {
            case Success( value ) => {
                value match {
                    case None => succeed
                    case _ => fail()
                }
            }
            case Failure( e ) => fail( e )
        }
    }

    "Ontology Table" should "get a specific previous version of the ontology" in {
        val expected = OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 1, ontology = "ontology", tags = List( "tag-1" ) )
        table.insert( expected )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, stagingVersion = 1, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, stagingVersion = 2, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-2", version = 3, ontology = "ontology", tags = List( "tag-1" ) ) )

        val result = table.getVersion( tenant = "tenant-1", version = 1 )

        result match {
            case Success( value ) => {
                value match {
                    case Some( actual ) => {
                        actual.id shouldBe expected.id
                        actual.tenant shouldBe expected.tenant
                        actual.version shouldBe expected.version
                        actual.stagingVersion shouldBe 0
                    }
                    case None => fail()
                }
            }
            case Failure( e ) => fail( e )
        }
    }

    "Ontology Table" should "not find the specified version of the ontology if it does not exist" in {
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 1, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, stagingVersion = 1, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, stagingVersion = 2, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-2", version = 3, ontology = "ontology", tags = List( "tag-1" ) ) )

        val result = table.getVersion( tenant = "tenant-1", version = 4 )

        result match {
            case Success( actual ) => {
                actual match {
                    case None => succeed
                    case _ => fail()
                }
            }
            case Failure( e ) => fail( e )
        }
    }

    "Ontology Table" should "not return staged versions when published versions are requested" in {
        val expected = OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, ontology = "ontology", tags = List( "tag-1" ) )

        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 1, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( expected )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, stagingVersion = 1, ontology = "ontology", tags = List( "tag-1" ) ) )
        table.insert( OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 2, stagingVersion = 2, ontology = "ontology", tags = List( "tag-1" ) ) )

        val result = table.getLatestForTenant( "tenant-1" )

        result match {
            case Success( value ) => {
                value match {
                    case Some( artifact ) =>
                        artifact.id shouldBe expected.id
                        artifact.tenant shouldBe expected.tenant
                        artifact.version shouldBe expected.version
                        artifact.stagingVersion shouldBe expected.stagingVersion
                        artifact.ontology shouldBe expected.ontology
                        artifact.tags shouldBe expected.tags
                        ( artifact.timestamp.toEpochSecond - expected.timestamp.toEpochSecond ) shouldBe 0
                    case None => fail()
                }
            }
        }
    }

}
