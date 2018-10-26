package emojidata

import java.util.concurrent.LinkedBlockingQueue

import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint
import com.twitter.hbc.core.{Constants, Hosts, HttpHosts}
import com.twitter.hbc.core.event.Event
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.httpclient.auth.OAuth1


object Main {

  def main(args: Array[String]) {
    val shutdown = new Shutdown()
    Runtime.getRuntime.addShutdownHook(shutdown)

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
      println(message)
    }
  }
}
