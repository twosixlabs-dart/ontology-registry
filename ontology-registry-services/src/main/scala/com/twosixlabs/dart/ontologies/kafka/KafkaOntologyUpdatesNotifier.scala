package com.twosixlabs.dart.ontologies.kafka

import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex
import com.twosixlabs.dart.json.JsonFormat
import com.twosixlabs.dart.ontologies.OntologyUpdatesNotifier
import com.twosixlabs.dart.ontologies.api.{DocumentUpdateNotification, TenantOntologyMapping}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.util.concurrent.{Future => JavaFuture}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.Try


class KafkaOntologyUpdatesNotifier( tenantIndex : CorpusTenantIndex,
                                    kafkaProducer : KafkaProducer[ String, String ],
                                    topic : String )
  extends OntologyUpdatesNotifier {

    implicit class ConvertibleJavaFuture[ T ]( jFut : JavaFuture[ T ] ) {
        lazy val asScala : Future[ T ] = Future {
            jFut.get()
        }
    }

    override def update( tenantId : TenantId, ontologyId : OntologyId ) : Try[ Boolean ] = {
        Try {
            val fut = tenantIndex.tenantDocuments( tenantId ) flatMap {
                ( docs : Seq[ String ] ) => {
                    val updateMessage = DocumentUpdateNotification( ontologies = Set( TenantOntologyMapping( tenantId, ontologyId ) ), document = None )
                    val updateMessageJson = JsonFormat.marshalFrom( updateMessage ).get

                    Future.sequence {
                        docs map { docId =>
                            kafkaProducer
                              .send( new ProducerRecord( topic, docId, updateMessageJson ) )
                              .asScala
                        }
                    }
                }
            } map ( _ => true ) recover {
                          case e : org.apache.kafka.common.errors.TimeoutException =>
                              throw new OntologyUpdatesNotifier.ServiceUnreachableException( "notifier (\"timed out\")", e )
                          case e =>
                              throw e
                      }

            Await.result( fut, 20.minutes )
        }
    }
}
