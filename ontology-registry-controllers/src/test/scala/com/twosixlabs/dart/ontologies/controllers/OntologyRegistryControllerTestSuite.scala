package com.twosixlabs.dart.ontologies.controllers

import com.twosixlabs.dart.json.JsonFormat
import com.twosixlabs.dart.ontologies.OntologyUpdatesNotifier
import com.twosixlabs.dart.ontologies.api.{OntologyArtifact, OntologyRegistry, OntologyRegistryException, OntologySubmission}
import com.twosixlabs.dart.test.base.StandardTestBase3x
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraSuite

import java.util.UUID
import scala.util.{Failure, Success}

class OntologyRegistryControllerTestSuite extends ScalatraSuite with StandardTestBase3x with BeforeAndAfterEach {
    override def header = null

    val notifier : OntologyUpdatesNotifier = mock[ OntologyUpdatesNotifier ]
    val registry : OntologyRegistry = mock[ OntologyRegistry ]
    val auth : AuthConfig = AuthConfig( None, useDartAuth = false )
    val controller = new OntologyRegistryController( registry, notifier, auth )

    val ontologyArtifactTemplate = OntologyArtifact( id = UUID.randomUUID().toString, tenant = "tenant-1", version = 1, ontology = "ontology", tags = List( "tag-1" ) )

    addServlet( controller, "/ontologies" )

    override def afterEach( ) = reset( registry, notifier )

    //================================
    // lookup published version by id
    //================================
    "Ontology Controller" should "get a published ontology by id" ignore {
        val publishedArtifact = ontologyArtifactTemplate.copy()
        when( registry.byId( publishedArtifact.id ) ).thenReturn( Success( Some( publishedArtifact ) ) )

        get( s"/ontologies?id=${publishedArtifact.id}" ) {
            Utils.fromJson( body ) shouldBe publishedArtifact
        }
    }

    "Ontology Controller" should "return nothing if there is no published ontology with the specified id" ignore {
        val missingArtifact = ontologyArtifactTemplate.copy()
        when( registry.byId( missingArtifact.id ) ).thenReturn( Success( None ) )

        val expectedMessage = s"there is no ontology with id=${missingArtifact.id}"
        get( s"/ontologies?id=${missingArtifact.id}" ) {
            body should include( expectedMessage )
        }
    }

    "Ontology Controller" should "return nothing if the requested version is a staged version of the ontology" ignore {
        val stagedArtifact = ontologyArtifactTemplate.copy( stagingVersion = 1 )
        when( registry.byId( stagedArtifact.id ) ).thenReturn( Success( Some( stagedArtifact ) ) )

        val expectedMessage = s"there is no published ontology with id=${stagedArtifact.id}"
        get( s"/ontologies?id=${stagedArtifact.id}" ) {
            body should include( expectedMessage )
        }
    }

    //================================
    // lookup latest version of the ontology by tenant
    //================================

    "Ontology Controller" should "get the latest published ontology for a tenant" ignore {
        val publishedArtifact = ontologyArtifactTemplate.copy()
        when( registry.latest( publishedArtifact.tenant ) ).thenReturn( Success( Some( publishedArtifact ) ) )

        get( s"/ontologies?tenant=${publishedArtifact.tenant}" ) {
            Utils.fromJson( body ) shouldBe publishedArtifact
        }
    }

    "Ontology Controller" should "return nothing if there is no published ontology for the tenant" ignore {
        val missingArtifact = ontologyArtifactTemplate.copy()
        when( registry.latest( missingArtifact.tenant ) ).thenReturn( Success( None ) )

        val expectedMessage = s"no ontologies are registered for tenant=${missingArtifact.tenant}"
        get( s"/ontologies?tenant=${missingArtifact.tenant}" ) {
            body should include( expectedMessage )
        }
    }

    //================================
    // lookup a specific version of the ontology
    //================================

    "Ontology Controller" should "get a specific published version of the ontology" ignore {
        val publishedArtifact = ontologyArtifactTemplate.copy()
        when( registry.byVersion( publishedArtifact.tenant, publishedArtifact.version ) ).thenReturn( Success( Some( publishedArtifact ) ) )

        get( s"/ontologies?tenant=${publishedArtifact.tenant}&version=${publishedArtifact.version}" ) {
            Utils.fromJson( body ) shouldBe publishedArtifact
        }
    }

    "Ontology Controller" should "return nothing if the requested published version does not exist" ignore {
        val missingArtifact = ontologyArtifactTemplate.copy()
        when( registry.byVersion( missingArtifact.tenant, missingArtifact.version ) ).thenReturn( Success( None ) )

        val expectedMessage = s"no ontology registered for tenant=${missingArtifact.tenant} version=${missingArtifact.version}"
        get( s"/ontologies?tenant=${missingArtifact.tenant}&version=${missingArtifact.version}" ) {
            body should include( expectedMessage )
        }
    }

    //================================
    // lookup staged version by id
    //================================
    "Ontology Controller" should "get a staged ontology by id" ignore {
        val stagedArtifact = ontologyArtifactTemplate.copy( stagingVersion = 1 )
        when( registry.byId( stagedArtifact.id ) ).thenReturn( Success( Some( stagedArtifact ) ) )

        get( s"/ontologies/staged?id=${stagedArtifact.id}" ) {
            Utils.fromJson( body ) shouldBe stagedArtifact
        }
    }

    "Ontology Controller" should "return nothing if there is no staged ontology with the specified id" ignore {
        val missingStagedArtifact = ontologyArtifactTemplate.copy()
        when( registry.byId( missingStagedArtifact.id ) ).thenReturn( Success( None ) )

        val expectedMessage = s"there is no ontology with id=${missingStagedArtifact.id}"
        get( s"/ontologies/staged?id=${missingStagedArtifact.id}" ) {
            body should include( expectedMessage )
        }
    }

    "Ontology Controller" should "return nothing if the ontology with the requested `id` is not a staged version" ignore {
        val publishedArtifact = ontologyArtifactTemplate.copy()
        when( registry.byId( publishedArtifact.id ) ).thenReturn( Success( Some( publishedArtifact ) ) )

        val expectedMessage = s"there is no staged ontology with id=${publishedArtifact.id}"
        get( s"/ontologies/staged?id=${publishedArtifact.id}" ) {
            body should include( expectedMessage )
        }
    }

    //================================
    // latest staged version of an ontology
    //================================

    "Ontology Controller" should "get the latest staged ontology for a tenant" ignore {
        val expected = ontologyArtifactTemplate.copy( stagingVersion = 1 )
        when( registry.latestStaged( expected.tenant ) ).thenReturn( Success( Some( expected ) ) )

        get( s"/ontologies/staged?tenant=${expected.tenant}" ) {
            Utils.fromJson( body ) shouldBe expected
        }
    }

    "Ontology Controller" should "return nothing if there is no staged ontology for that tenant" ignore {
        val missingStagedArtifact = ontologyArtifactTemplate.copy()
        when( registry.latestStaged( missingStagedArtifact.tenant ) ).thenReturn( Success( None ) )

        val expectedMessage = s"no ontologies are registered for tenant=${missingStagedArtifact.tenant}"
        get( s"/ontologies/staged?tenant=${missingStagedArtifact.tenant}" ) {
            body should include( expectedMessage )
        }
    }

    //================================
    // error handling
    //================================

    "Ontology Controller" should "report server errors that occurred looking up a published ontology by id" in {
        val publishedArtifact = ontologyArtifactTemplate.copy()
        when( registry.byId( publishedArtifact.id ) ).thenReturn( Failure( new OntologyRegistryException( "failed", null ) ) )

        val expectedMessage = s"internal error looking up ontology with id=${publishedArtifact.id}"
        get( s"/ontologies?id=${publishedArtifact.id}" ) {
            body should include( expectedMessage )
        }
    }

    "Ontology Controller" should "report server errors that occurred looking up a staged ontology by id" in {
        val stagedArtifact = ontologyArtifactTemplate.copy( stagingVersion = 1 )
        when( registry.byId( stagedArtifact.id ) ).thenReturn( Failure( new OntologyRegistryException( "failed", null ) ) )

        val expectedMessage = s"internal error looking up ontology with id=${stagedArtifact.id}"
        get( s"/ontologies/staged?id=${stagedArtifact.id}" ) {
            body should include( expectedMessage )
        }
    }

    "Ontology Controller" should "report server errors that occurred looking up the latest published ontology for a tenant" in {
        val publishedArtifact = ontologyArtifactTemplate.copy( stagingVersion = 1 )
        when( registry.latest( publishedArtifact.tenant ) ).thenReturn( Failure( new OntologyRegistryException( "failed", null ) ) )

        val expectedMessage = s"internal error looking up latest published ontology artifacts for tenant=${publishedArtifact.tenant}"
        get( s"/ontologies?tenant=${publishedArtifact.tenant}" ) {
            body should include( expectedMessage )
        }
    }

    "Ontology Controller" should "report server errors that occurred looking up a specific published version of the ontology" in {
        val publishedArtifact = ontologyArtifactTemplate.copy()
        when( registry.byVersion( publishedArtifact.tenant, publishedArtifact.version ) ).thenReturn( Failure( new OntologyRegistryException( "failed", null ) ) )

        val expectedMessage = s"internal error looking up published ontology artifacts for tenant=${publishedArtifact.tenant} version=${publishedArtifact.version}"
        get( s"/ontologies?tenant=${publishedArtifact.tenant}&version=${publishedArtifact.version}" ) {
            println( body )
            body should include( expectedMessage )
        }
    }

    "Ontology Controller" should "report server errors that occurred looking up the latest staged ontology for a tenant" in {
        val stagedArtifact = ontologyArtifactTemplate.copy( stagingVersion = 1 )
        when( registry.latestStaged( stagedArtifact.tenant ) ).thenReturn( Failure( new OntologyRegistryException( "failed", null ) ) )

        val expectedMessage = s"internal error looking up latest staged ontology artifacts for tenant=${stagedArtifact.tenant}"
        get( s"/ontologies/staged?tenant=${stagedArtifact.tenant}" ) {
            body should include( expectedMessage )
        }
    }

    "Ontology Controller" should "properly handle missing required query parameters when looking up a published ontology" in {
        val publishedArtifact = ontologyArtifactTemplate.copy()

        val expectedMessage = s"either `id` or `tenant` is required to look up a published ontology"
        get( s"/ontologies?version=${publishedArtifact.version}" ) {
            body should include( expectedMessage )
        }
    }

    "Ontology Controller" should "properly handle missing required query parameters when looking up a staged ontology" in {
        val stagedArtifact = ontologyArtifactTemplate.copy( stagingVersion = 1 )

        val expectedMessage = s"either `id` or `tenant` is required to look up a staged ontology"
        get( s"/ontologies/staged?version=${stagedArtifact.version}" ) {
            body should include( expectedMessage )
        }
    }

    behavior of "Ontology Controller"

    //================================
    // Publish latest staged version
    //================================
    val tenantId = "test-tenant"
    val stagedId = "test-staged-id"
    val committedId = "test-committed-id"
    val ontology = "test-ontology"
    val stagedArtifact = OntologyArtifact( stagedId,
                                           tenantId,
                                           1,
                                           3,
                                           ontology )

    val committedArtifact = OntologyArtifact( committedId,
                                              tenantId,
                                              2,
                                              0,
                                              ontology )

    it should "call registry.latestStaged, submit result to registry.commitOntology, call notifer.update when successful, and return 201" in {
        when( registry.latestStaged( tenantId ) ).thenReturn( Success( Some( stagedArtifact ) ) )
        when( registry.commitOntology( stagedArtifact ) ).thenReturn( Success( committedArtifact ) )
        when( notifier.update( tenantId, committedId ) ).thenReturn( Success( true ) )

        post( s"/ontologies/publish/${tenantId}" ) {
            body shouldBe ""
            status shouldBe 201
        }

        verify( registry, times( 1 ) ).latestStaged( tenantId )
        verify( registry, times( 1 ) ).commitOntology( stagedArtifact )
        verify( notifier, times( 1 ) ).update( tenantId, committedId )
    }

    it should "return 404 if registry.latestStaged comes back empty" in {
        when( registry.latestStaged( tenantId ) ).thenReturn( Success( None ) )

        post( s"/ontologies/publish/${tenantId}" ) {
            body should ( include( "Resource not found" ) and include( tenantId ) )
            status shouldBe 404
        }

        verify( registry, times( 1 ) ).latestStaged( tenantId )
        verify( registry, times( 0 ) ).commitOntology( * )
        verify( notifier, times( 0 ) ).update( *, * )
    }

    //================================
    // Submit ontology for staging
    //================================

    val submission = OntologySubmission(
        ontology = ontology,
        )

    it should "create an OntologyArtifact from the submission, pass it to registry.stageOntology, and return 201 with the artifact when stageOntology returns Success( true )" in {
        when( registry.stageOntology( * ) ).thenReturn( Success( stagedArtifact ) )

        post( s"/ontologies/stage/${tenantId}", body = JsonFormat.marshalFrom( submission ).get.getBytes() ) {
            response.body shouldBe JsonFormat.marshalFrom( stagedArtifact ).get
            status shouldBe 201
        }

        verify( registry, times( 0 ) ).latestStaged( * )
        verify( registry, times( 0 ) ).commitOntology( * )
        verify( registry, times( 1 ) ).stageOntology( * )
        verify( notifier, times( 0 ) ).update( *, * )
    }

    it should "return 400 if submission is missing" in {
        when( registry.latestStaged( tenantId ) ).thenReturn( Success( None ) )

        post( s"/ontologies/stage/${tenantId}" ) {
            body should ( include( "Bad request" ) and include( "no ontology provided (empty request body)" ) )
            status shouldBe 400
        }

        verify( registry, times( 0 ) ).latestStaged( * )
        verify( registry, times( 0 ) ).commitOntology( * )
        verify( registry, times( 0 ) ).stageOntology( * )
        verify( notifier, times( 0 ) ).update( *, * )
    }

    it should "return 400 if submission is valid json but wrong format" in {
        when( registry.latestStaged( tenantId ) ).thenReturn( Success( None ) )

        post( s"/ontologies/stage/${tenantId}", body = "{\"valid_json\":2,\"but\":\"unrecognized\"}" ) {
            body should ( include( "Bad request" ) and include( "unable to parse ontology submission" ) )
            status shouldBe 400
        }

        verify( registry, times( 0 ) ).latestStaged( * )
        verify( registry, times( 0 ) ).commitOntology( * )
        verify( registry, times( 0 ) ).stageOntology( * )
        verify( notifier, times( 0 ) ).update( *, * )
    }

    it should "return 400 if submission is invalid json" in {
        when( registry.latestStaged( tenantId ) ).thenReturn( Success( None ) )

        post( s"/ontologies/stage/${tenantId}", body = "{invalid: json}" ) {
            body should ( include( "Bad request" ) and include( "unable to parse ontology submission" ) )
            status shouldBe 400
        }

        verify( registry, times( 0 ) ).latestStaged( * )
        verify( registry, times( 0 ) ).commitOntology( * )
        verify( registry, times( 0 ) ).stageOntology( * )
        verify( notifier, times( 0 ) ).update( *, * )
    }

}
