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
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint
import com.twitter.hbc.core.{Constants, HttpHosts}
import com.twitter.hbc.core.event.Event
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.httpclient.auth.OAuth1
import org.apache.log4j.{ConsoleAppender, Level, PatternLayout}
import org.slf4j.LoggerFactory
import twitteranalyzer.TweetEmojiActor.{RequestPercentEmoji, RequestTopEmojis, ResponsePercentEmoji, ResponseTopEmojis}
import twitteranalyzer.TweetPercentPicActor.{RequestPercentPic, ResponsePercentPic}
import twitteranalyzer.TweetTopHashTagsActor.{RequestTopHashTags, ResponseTopHashTags}
import twitteranalyzer.TweetPercentUrlsActor.{RequestPercentUrl, ResponsePercentUrl}
import twitteranalyzer.TweetPropagatingActor.Message
import twitteranalyzer.TweetRateActor.{RequestByHour, RequestByMinute, RequestBySecond, ResponseRate}
import twitteranalyzer.TweetTopDomainsActor.{RequestTopDomains, ResponseTopDomains}
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
    mapper.registerModule(DefaultScalaModule)

    val shutdown = new Shutdown()
    Runtime.getRuntime.addShutdownHook(shutdown)

    implicit val system: ActorSystem = ActorSystem("twitterAnalyzer")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val printingActor = system.actorOf(Props[TweetPrintingActor])
    val tweetTotalActor = system.actorOf(Props[TweetTotalActor])
    val tweetEmojiActor = system.actorOf(Props[TweetEmojiActor])
    val tweetRateActor = system.actorOf(Props[TweetRateActor])
    val tweetHashTagActor = system.actorOf(Props[TweetTopHashTagsActor])
    val tweetPercentUrlActor = system.actorOf(Props[TweetPercentUrlsActor])
    val tweetTopDomainsActor = system.actorOf(Props[TweetTopDomainsActor])
    val tweetPercentPicActor = system.actorOf(Props[TweetPercentPicActor])

    val actorGroup = List(
      tweetTotalActor,
      tweetEmojiActor,
      tweetRateActor,
      tweetHashTagActor,
      tweetPercentUrlActor,
      tweetTopDomainsActor,
      tweetPercentPicActor
    )
    val propagatingActor = system.actorOf(TweetPropagatingActor.props(actorGroup, mapper))

    val routes =
      get {
        path("total") {
          implicit val timeout: Timeout = Timeout(3 seconds)
          val response = tweetTotalActor ? RequestTotal(java.util.UUID.randomUUID().toString)
          val totalResponse: Future[ResponseTotal] = response.mapTo[ResponseTotal]
          val result = Await.result(totalResponse, timeout.duration)
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, result.total.toString))
        }
      } ~
        pathPrefix("emojis") {
          get {
            path("percent") {
              implicit val timeout: Timeout = Timeout(3 seconds)
              val response = tweetEmojiActor ? RequestPercentEmoji(java.util.UUID.randomUUID().toString)
              val percentResponse: Future[ResponsePercentEmoji] = response.mapTo[ResponsePercentEmoji]
              val result = Await.result(percentResponse, timeout.duration)
              complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, result.percent.toString))
            }
          } ~
            get {
              path("top") {
                implicit val timeout: Timeout = Timeout(3 seconds)
                val response = tweetEmojiActor ? RequestTopEmojis(java.util.UUID.randomUUID().toString)
                val topEmojisResponse: Future[ResponseTopEmojis] = response.mapTo[ResponseTopEmojis]
                val result = Await.result(topEmojisResponse, timeout.duration)
                complete(HttpEntity(ContentTypes.`application/json`, mapper.writeValueAsString(result.emojis)))
              }
            }
        } ~
        pathPrefix("rate") {
          get {
            path("hour") {
              implicit val timeout: Timeout = Timeout(3 seconds)
              val response = tweetRateActor ? RequestByHour(java.util.UUID.randomUUID().toString)
              val hourlyResponse: Future[ResponseRate] = response.mapTo[ResponseRate]
              val result = Await.result(hourlyResponse, timeout.duration)
              complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, result.value.toString))
            }
          } ~
            get {
              path("minute") {
                implicit val timeout: Timeout = Timeout(3 seconds)
                val response = tweetRateActor ? RequestByMinute(java.util.UUID.randomUUID().toString)
                val minutelyResponse: Future[ResponseRate] = response.mapTo[ResponseRate]
                val result = Await.result(minutelyResponse, timeout.duration)
                complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, result.value.toString))
              }
            } ~
            get {
              path("second") {
                implicit val timeout: Timeout = Timeout(3 seconds)
                val response = tweetRateActor ? RequestBySecond(java.util.UUID.randomUUID().toString)
                val secondlyResponse: Future[ResponseRate] = response.mapTo[ResponseRate]
                val result = Await.result(secondlyResponse, timeout.duration)
                complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, result.value.toString))
              }
            }
        } ~
        pathPrefix("hashTags") {
          get {
            path("top") {
              implicit val timeout: Timeout = Timeout(3 seconds)
              val response = tweetHashTagActor ? RequestTopHashTags(java.util.UUID.randomUUID().toString, 5)
              val topHashTagsResponse: Future[ResponseTopHashTags] = response.mapTo[ResponseTopHashTags]
              val result = Await.result(topHashTagsResponse, timeout.duration)
              complete(HttpEntity(ContentTypes.`application/json`, mapper.writeValueAsString(result.hashTags)))
            }
          }
        } ~
        pathPrefix("urls") {
          get {
            path("percent") {
              implicit val timeout: Timeout = Timeout(3 seconds)
              val response = tweetPercentUrlActor ? RequestPercentUrl(java.util.UUID.randomUUID().toString)
              val percentResponse: Future[ResponsePercentUrl] = response.mapTo[ResponsePercentUrl]
              val result = Await.result(percentResponse, timeout.duration)
              complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, result.percent.toString))
            }
          } ~
          get {
            path("top") {
              implicit val timeout: Timeout = Timeout(3 seconds)
              val response = tweetTopDomainsActor ? RequestTopDomains(java.util.UUID.randomUUID().toString, 5)
              val topDomainsResponse: Future[ResponseTopDomains] = response.mapTo[ResponseTopDomains]
              val result = Await.result(topDomainsResponse, timeout.duration)
              complete(HttpEntity(ContentTypes.`application/json`, mapper.writeValueAsString(result.domains)))
            }
          } ~
          get {
            path("percent-pic") {
              implicit val timeout: Timeout = Timeout(3 seconds)
              val response = tweetPercentPicActor ? RequestPercentPic(java.util.UUID.randomUUID().toString)
              val percentResponse: Future[ResponsePercentPic] = response.mapTo[ResponsePercentPic]
              val result = Await.result(percentResponse, timeout.duration)
              complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, result.percent.toString))
            }
          }
        }

    val binding = Http().bindAndHandle(routes, "localhost", 8080)

    shutdown += (() => binding.flatMap(_.unbind()).onComplete(_ => system.terminate()))

    val msgQueue = new LinkedBlockingQueue[String](1000000)
    val eventQueue = new LinkedBlockingQueue[Event](1000)

    val hosebirdHosts = new HttpHosts(Constants.STREAM_HOST)
    val hosebirdEndpoint = new StatusesSampleEndpoint()
    val hosebirdAuth = new OAuth1(
      System.getenv("CONSUMER_KEY"),
      System.getenv("CONSUMER_SECRET"),
      System.getenv("ACCESS_TOKEN"),
      System.getenv("ACCESS_SECRET")
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
