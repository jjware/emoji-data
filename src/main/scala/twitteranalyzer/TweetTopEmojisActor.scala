package twitteranalyzer

import akka.actor.Actor
import com.vdurmont.emoji.EmojiParser
import twitteranalyzer.TweetTopEmojisActor.{RequestTopEmojis, ResponseTopEmojis}

import scala.annotation.tailrec
import scala.collection.JavaConversions._

object TweetTopEmojisActor {

  final case class RequestTopEmojis(correlationId: String, num: Int)

  final case class ResponseTopEmojis(correlationId: String, emojis: List[String])

}

class TweetTopEmojisActor extends Actor {
  override def receive: Receive = onMessage(Map.empty)

  private def onMessage(emojiMap: Map[String, Long]): Receive = {
    case TweetMessage(tweet) =>
      val emojis = EmojiParser.extractEmojis(tweet.text).toList
      context become onMessage(populateMap(emojiMap, emojis))
    case RequestTopEmojis(id, num) =>
      val topEmojis = emojiMap.toList.sortBy(_._2).reverse.take(num)
      sender() ! ResponseTopEmojis(id, topEmojis.map(_._1))
  }

  @tailrec
  private def populateMap(emojiMap: Map[String, Long], emojis: List[String]): Map[String, Long] = {
    if (emojis.nonEmpty) {
      val emoji = emojis.head
      val newMap: Map[String, Long] = emojiMap + (emoji -> emojiMap.get(emoji).map(_ + 1).getOrElse(1))
      populateMap(newMap, emojis.tail)
    } else {
      emojiMap
    }
  }
}
