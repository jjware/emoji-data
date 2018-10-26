package twitteranalyzer

import java.util.concurrent.LinkedBlockingQueue

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import com.fasterxml.jackson.databind.ObjectMapper
import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint
import com.twitter.hbc.core.{Constants, HttpHosts}
import com.twitter.hbc.core.event.Event
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.httpclient.auth.OAuth1
import org.apache.log4j.{ConsoleAppender, Level, PatternLayout}
import org.slf4j.LoggerFactory
import twitteranalyzer.TweetPropagatingActor.Message
import twitteranalyzer.TweetTotalActor.{RequestTotal, ResponseTotal}

import scala.concurrent.{Await, ExecutionContextExecutor, Future}


object Main {

  private val layoutPattern = new PatternLayout("[%d{ISO8601}] %5p [%t] (%C{1}) - %m%n")

  def main(args: Array[String]) {
    val console = new ConsoleAppender
    console.setName("ConsoleAppender")
    console.setThreshold(Level.WARN)
    console.setLayout(layoutPattern)
    console.activateOptions()
    org.apache.log4j.Logger.getRootLogger.addAppender(console)

    val logger = LoggerFactory.getLogger("Main")
    val mapper = new ObjectMapper()

    val shutdown = new Shutdown()
    Runtime.getRuntime.addShutdownHook(shutdown)

    implicit val system: ActorSystem = ActorSystem("twitterAnalyzer")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val printingActor = system.actorOf(Props[TweetPrintingActor])
    val totalActor = system.actorOf(Props[TweetTotalActor])

    val actorGroup = List(
      printingActor,
      totalActor
    )
    val propagatingActor = system.actorOf(TweetPropagatingActor.props(actorGroup, mapper))

    val routes =
      get {
        path("total") {
          implicit val timeout: Timeout = Timeout(3 seconds)
          val response = totalActor ? RequestTotal(java.util.UUID.randomUUID().toString)
          val totalResponse: Future[ResponseTotal] = response.mapTo[ResponseTotal]
          val result = Await.result(totalResponse, timeout.duration)
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, result.total.toString))
        }
      }

    val binding = Http().bindAndHandle(routes, "localhost", 8080)

    shutdown += (() => binding.flatMap(_.unbind()).onComplete(_ => system.terminate()))

    val msgQueue = new LinkedBlockingQueue[String](1000000)
    val eventQueue = new LinkedBlockingQueue[Event](1000)

    val hosebirdHosts = new HttpHosts(Constants.STREAM_HOST)
    val hosebirdEndpoint = new StatusesSampleEndpoint()
    val hosebirdAuth = new OAuth1(
      "",
      "",
      "",
      ""
    )

    val hosebirdClient = new ClientBuilder()
      .hosts(hosebirdHosts)
      .authentication(hosebirdAuth)
      .endpoint(hosebirdEndpoint)
      .processor(new StringDelimitedProcessor(msgQueue))
      .eventMessageQueue(eventQueue)
      .build()

    hosebirdClient.connect()

    shutdown += (() => hosebirdClient.stop())

    while (!hosebirdClient.isDone) {
      val message = msgQueue.take()
      propagatingActor ! Message(message)
    }
  }
}
