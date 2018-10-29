package twitteranalyzer

import akka.actor.Actor
import twitteranalyzer.TweetTopDomainsActor.{RequestTopDomains, ResponseTopDomains}

import scala.annotation.tailrec
import scala.collection.immutable.HashMap

object TweetTopDomainsActor {

  final case class RequestTopDomains(correlationId: String, num: Int)

  final case class ResponseTopDomains(correlationId: String, domains: List[String])

}

class TweetTopDomainsActor extends Actor {
  override def receive: Receive = onMessage(HashMap.empty)

  private def onMessage(domainMap: Map[String, Long]): Receive = {
    case TweetMessage(tweet) =>
      context become onMessage(populateMap(domainMap, tweet.entities.urls))
    case RequestTopDomains(id, num) =>
      val topDomains = domainMap.toList.sortBy(-_._2).take(num)
      sender() ! ResponseTopDomains(id, topDomains.map(_._1))
  }

  @tailrec
  private def populateMap(map: Map[String, Long], urls: List[TweetURL]): Map[String, Long] = {
    if (urls.nonEmpty) {
      val url = java.net.URI.create(urls.head.expandedUrl)
      val newMap: Map[String, Long] = map + (url.getHost -> map.get(url.getHost).map(_ + 1).getOrElse(1))
      populateMap(newMap, urls.tail)
    } else {
      map
    }
  }
}
