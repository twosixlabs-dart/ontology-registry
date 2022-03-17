package com.twosixlabs.dart.ontologies

import com.twosixlabs.dart.ontologies.api.{OntologyArtifact, OntologyArtifactTable, OntologyRegistryException}
import com.twosixlabs.dart.test.base.StandardTestBase3x
import org.scalatest.BeforeAndAfterEach

import java.util.UUID
import scala.util.{Failure, Success}

class OntologyRegistryServiceTestSuite extends StandardTestBase3x with BeforeAndAfterEach {

    val table = mock[ OntologyArtifactTable ]
    val service : OntologyRegistryService = new OntologyRegistryService( table )

    val artifactInputTemplate = OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 1, ontology = "ontology", tags = List( "tag-1" ) )

    override def afterEach( ) : Unit = {
        reset( table )
    }

    "Ontology Registry Service" should "return the latest ontology for a tenant" in {
        when( table.getLatestForTenant( artifactInputTemplate.tenant ) ).thenReturn( Success( Some( artifactInputTemplate ) ) )

        val result = service.latest( artifactInputTemplate.tenant )

        result.isSuccess shouldBe true
        result.get.isDefined shouldBe true
        result.get.get shouldBe artifactInputTemplate
    }

    "Ontology Registry Service" should "return nothing if there is no ontology published for the tenant" in {
        when( table.getLatestForTenant( artifactInputTemplate.tenant ) ).thenReturn( Success( None ) )

        val result = service.latest( artifactInputTemplate.tenant )

        result.isSuccess shouldBe true
        result.get.isDefined shouldBe false
    }

    "Ontology Registry Service" should "return the latest staged version of an ontology" in {
        val expected = artifactInputTemplate.copy( stagingVersion = 1 )
        when( table.getLatestForTenant( expected.tenant, useStagedVersion = true ) ).thenReturn( Success( Some( expected ) ) )

        val result = service.latestStaged( expected.tenant )

        result.isSuccess shouldBe true
        result.get.isDefined shouldBe true
        result.get.get shouldBe expected
    }

    "Ontology Registry Service" should "nothing if there are no staged versions" in {
        when( table.getLatestForTenant( artifactInputTemplate.tenant, useStagedVersion = true ) ).thenReturn( Success( None ) )

        val result = service.latestStaged( artifactInputTemplate.tenant )

        result.isSuccess shouldBe true
        result.get.isDefined shouldBe false
    }

    "Ontology Registry Service" should "return an ontology by tenant and version" in {
        val expected = artifactInputTemplate
        when( table.getVersion( artifactInputTemplate.tenant, artifactInputTemplate.version ) ).thenReturn( Success( Some( expected ) ) )

        val result = service.byVersion( artifactInputTemplate.tenant, artifactInputTemplate.version )

        result.isSuccess shouldBe true
        result.get.isDefined shouldBe true
        result.get.get shouldBe expected
    }

    "Ontology Registry Service" should "return an ontology by id" in {
        val expected = artifactInputTemplate
        when( table.getById( expected.id ) ).thenReturn( Success( Some( expected ) ) )

        val result = service.byId( expected.id )

        result.isSuccess shouldBe true
        result.get.isDefined shouldBe true
        result.get.get shouldBe expected
    }

    "Ontology Registry Service" should "commit a brand new ontology" in {
        val expected = artifactInputTemplate.copy( version = 1 ) // if new ontology the version should be set to 1

        when( table.getLatestForTenant( artifactInputTemplate.tenant ) ).thenReturn( Success( None ) )
        when( table.insert( * ) ).thenReturn( Success( expected ) )

        val result = service.commitOntology( artifactInputTemplate )

        result.isSuccess shouldBe true
        result.get shouldBe expected

    }

    "Ontology Registry Service" should "commit a new version of an existing ontology" in {
        val input = artifactInputTemplate
        val existing = input.copy( version = 1 )
        val expected = input.copy( version = 2 ) // the version should be incremented from the latest

        when( table.getLatestForTenant( input.tenant ) ).thenReturn( Success( Some( existing ) ) )
        when( table.insert( * ) ).thenReturn( Success( expected ) )

        val result = service.commitOntology( input )

        result.isSuccess shouldBe true
        result.get shouldBe expected

    }

    "Ontology Registry Service" should "handle failures looking up the latest version of an ontology" in {
        val input = artifactInputTemplate
        val expectedException = new OntologyRegistryException( "lookup failed", null )

        when( table.getLatestForTenant( input.tenant ) ).thenReturn( Failure( expectedException ) )

        val result = service.commitOntology( input )

        result.isSuccess shouldBe false
        result.failed.get shouldBe expectedException
    }

    "Ontology Registry Service" should "handle failures looking up the latest staged version of an ontology" in {
        val input = artifactInputTemplate
        val expectedException = new OntologyRegistryException( "lookup failed", null )

        when( table.getLatestForTenant( input.tenant, useStagedVersion = true ) ).thenReturn( Failure( expectedException ) )

        val result = service.stageOntology( input )

        result.isSuccess shouldBe false
        result.failed.get shouldBe expectedException
    }

    "Ontology Registry Service" should "handle failures from writing a new ontology version" in {
        val input = artifactInputTemplate
        val expectedException = new OntologyRegistryException( "write failed", null )

        when( table.getLatestForTenant( input.tenant ) ).thenReturn( Success( Some( input ) ) )
        when( table.insert( * ) ).thenReturn( Failure( expectedException ) )

        val result = service.commitOntology( input )

        result.isSuccess shouldBe false
        result.failed.get shouldBe expectedException
    }

    "Ontology Registry Service" should "handle failures from writing a new staged ontology version" in {
        val input = artifactInputTemplate
        val expectedException = new OntologyRegistryException( "write failed", null )

        when( table.getLatestForTenant( input.tenant, useStagedVersion = true ) ).thenReturn( Success( Some( input ) ) )
        when( table.insert( * ) ).thenReturn( Failure( expectedException ) )

        val result = service.stageOntology( input )

        result.isSuccess shouldBe false
        result.failed.get shouldBe expectedException
    }

    "Ontology Registry Service" should "create the first staged version of an ontology" in {
        val input = artifactInputTemplate
        val existing = input.copy( stagingVersion = 0 )
        val expected = input.copy( stagingVersion = artifactInputTemplate.stagingVersion + 1 ) // staging version should be incremented

        when( table.getLatestForTenant( input.tenant, useStagedVersion = true ) ).thenReturn( Success( Some( existing ) ) )
        when( table.insert( * ) ).thenReturn( Success( expected ) )

        val result = service.stageOntology( input )

        result.isSuccess shouldBe true
        result.get shouldBe expected
    }

    "Ontology Registry Service" should "increment the staged version of an ontology" in {
        val input = artifactInputTemplate
        val existing = input.copy( stagingVersion = 0 )
        val updated = input.copy( stagingVersion = artifactInputTemplate.stagingVersion + 1 ) // staging version should be incremented
        val expected = input.copy( stagingVersion = updated.stagingVersion + 1 )

        when( table.getLatestForTenant( input.tenant, useStagedVersion = true ) ).thenReturn( Success( Some( existing ) ) )
        when( table.insert( * ) ).thenReturn( Success( updated ) )

        when( table.getLatestForTenant( updated.tenant, useStagedVersion = true ) ).thenReturn( Success( Some( updated ) ) )
        when( table.insert( * ) ).thenReturn( Success( expected ) )

        service.stageOntology( input )
        val result = service.stageOntology( updated )

        result.isSuccess shouldBe true
        result.get shouldBe expected
    }
}
