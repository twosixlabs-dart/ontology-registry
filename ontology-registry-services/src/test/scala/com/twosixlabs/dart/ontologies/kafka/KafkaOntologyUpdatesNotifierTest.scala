package com.twosixlabs.dart.ontologies.kafka

import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex
import com.twosixlabs.dart.json.JsonFormat
import com.twosixlabs.dart.ontologies.api.{DocumentUpdateNotification, TenantOntologyMapping}
import com.twosixlabs.dart.test.base.StandardTestBase3x
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.{Deserializer, Serdes, Serializer}

import java.util.Properties
import scala.concurrent.Future
import scala.util.Success

class KafkaOntologyUpdatesNotifierTest extends StandardTestBase3x with EmbeddedKafka {
    private implicit val EMBEDDED_KAFKA_CONFIG = EmbeddedKafkaConfig( kafkaPort = 6310, zooKeeperPort = 6311 )

    private implicit val serializer : Serializer[ String ] = Serdes.String.serializer()
    private implicit val deserializer : Deserializer[ String ] = Serdes.String.deserializer()

    val tenantIndex = mock[ CorpusTenantIndex ]

    val testTopic = "test-topic"

    behavior of "KafkaOntologyUpdatesNotifier"

    it should "send a notification to the specified topic" in {
        val props : Properties = {
            val p = new Properties()
            p.setProperty( "bootstrap.servers", "localhost:6310" )
            p.setProperty( "key.serializer", "org.apache.kafka.common.serialization.StringSerializer" )
            p.setProperty( "value.serializer", "org.apache.kafka.common.serialization.StringSerializer" )
            p
        }

        val producer : KafkaProducer[ String, String ] = new KafkaProducer[ String, String ]( props )
        val notifier = new KafkaOntologyUpdatesNotifier( tenantIndex, producer, testTopic )

        val testTenantId = "test-tenant"
        val testOntologyId = "test-ontology"
        val testDocIds = Seq( "test-doc-1", "test-doc-2", "test-doc-3" )

        when( tenantIndex.tenantDocuments( testTenantId ) ).thenReturn( Future.successful( testDocIds ) )

        withRunningKafka {

            notifier.update( testTenantId, testOntologyId ) shouldBe Success( true )

            val result : Seq[ (String, String) ] = consumeNumberKeyedMessagesFrom( testTopic, 3, true )

            result should have size ( 3 )

            val docIds = result.map( _._1 )

            docIds.toSet shouldBe testDocIds.toSet

            val messages = result.map( v => JsonFormat.unmarshalTo[ DocumentUpdateNotification ]( v._2, classOf[ DocumentUpdateNotification ] ).get )
            all( messages ) shouldBe DocumentUpdateNotification( document = None, ontologies = Set( TenantOntologyMapping( testTenantId, testOntologyId ) ) )
        }
    }

}
