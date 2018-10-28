package twitteranalyzer

import akka.actor.Actor
import twitteranalyzer.TweetTopHashTagsActor.{RequestTopHashTags, ResponseTopHashTags}

import scala.annotation.tailrec
import scala.collection.immutable.HashMap

object TweetTopHashTagsActor {
  final case class RequestTopHashTags(correlationId: String, num: Int)
  final case class ResponseTopHashTags(correlationId: String, hashTags: List[String])
}

class TweetTopHashTagsActor extends Actor {
  override def receive: Receive = onMessage(HashMap.empty)

  private def onMessage(hashMap: Map[String, Long]): Receive = {
    case TweetMessage(tweet) =>
      context become onMessage(populateMap(hashMap, tweet.entities.hashTags))
    case RequestTopHashTags(id, num) =>
      val topHashTags = hashMap.toList.sortBy(- _._2).take(num)
      sender() ! ResponseTopHashTags(id, topHashTags.map(_._1))
  }

  @tailrec
  private def populateMap(hashMap: Map[String, Long], hashTags: List[TweetHashTag]): Map[String, Long] = {
    if (hashTags.nonEmpty) {
      val hashTag = hashTags.head
      val newMap: Map[String, Long] = hashMap + (hashTag.text -> hashMap.get(hashTag.text).map(_ + 1).getOrElse(1))
      populateMap(newMap, hashTags.tail)
    } else {
      hashMap
    }
  }
}
