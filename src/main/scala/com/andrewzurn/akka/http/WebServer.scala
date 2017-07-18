package com.andrewzurn.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory

import scala.io.StdIn

// Server definition
object WebServer {
  def main(args: Array[String]) {

    val logger = LoggerFactory.getLogger(this.getClass.getSimpleName)

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    var currentRequests = 0

    val route =
      path("hello") {
        get {
          complete {
            currentRequests += 1
            val wait = (scala.util.Random.nextInt(3) + 1) * 1000
            logger.info(s"Current requests now at: $currentRequests. Going to wait: ${wait}s")
            Thread.sleep(wait.toLong)
            HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Returning request after ${wait}s")
          }
        }
      }.map { result =>
        currentRequests = currentRequests - 1
        logger.info(s"Finishing request. Current requests now at: $currentRequests.")
        result
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    logger.info(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
