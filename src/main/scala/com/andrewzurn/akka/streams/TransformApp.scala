package com.andrewzurn.akka.streams

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.kafka.scaladsl._
import akka.kafka._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization._
import org.slf4j.LoggerFactory

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object TransformApp extends App {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName)
  private val config = ConfigFactory.load()
  private val inputTopic = config.getString("input-topic")
  private val outputTopic = config.getString("output-topic")
  private implicit val actorSystem =
    ActorSystem("kafka-mirror-system", config)

  try {
    val producerSettings = ProducerSettings(actorSystem, new ByteArraySerializer, new StringSerializer)
    val consumerSettings = ConsumerSettings(actorSystem, new ByteArrayDeserializer, new StringDeserializer)
    implicit val actorMaterializer = ActorMaterializer()

    /**
      * Using a committableSource and a committableSink,
      * we can resume from where we left off if our process crashes.
      */
    val completion: Future[Done] =
      Consumer
        .committableSource(consumerSettings, Subscriptions.topics(inputTopic))
        .mapAsync(4) { msg =>
          Http().singleRequest(HttpRequest(uri = "http://localhost:8080/hello"))
            .flatMap { result =>
              val bodyFuture = result.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
                logger.info("Got response, body: " + body.utf8String)
                body
              }

              bodyFuture map { body =>
                val newTimestamp: java.lang.Long = {
                  if (msg.record.timestamp() < 0) null
                  else msg.record.timestamp()
                }
                ProducerMessage.Message(new ProducerRecord(
                  outputTopic,
                  null,
                  newTimestamp,
                  msg.record.key,
                  "Produced message: " + msg.record.value + " -- " + body.utf8String
                ),
                  msg.committableOffset)
              }
            }
        }
        .runWith(Producer.commitableSink(producerSettings))

    Await.result(completion, Duration.Inf)
  } finally {
    Await.result(actorSystem.terminate(), Duration.Inf)
  }

}
