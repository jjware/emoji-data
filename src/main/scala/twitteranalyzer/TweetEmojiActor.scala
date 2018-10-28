package twitteranalyzer

import akka.actor.Actor
import com.vdurmont.emoji.EmojiParser
import twitteranalyzer.TweetEmojiActor.{RequestPercentEmoji, RequestTopEmojis, ResponsePercentEmoji, ResponseTopEmojis}

import scala.annotation.tailrec
import scala.collection.JavaConversions._

object TweetEmojiActor {

  final case class RequestPercentEmoji(correlationId: String)

  final case class ResponsePercentEmoji(correlationId: String, percent: Double)

  final case class RequestTopEmojis(correlationId: String)

  final case class ResponseTopEmojis(correlationId: String, emojis: List[String])

}

class TweetEmojiActor extends Actor {
  override def receive = onMessage(Map.empty, 0, 0)

  private def onMessage(emojiCounts: Map[String, Long],
                totalEmojiTweets: Long,
                totalTweets: Long): Receive = {
    case TweetMessage(tweet) =>
      val newTotalTweets = totalTweets + 1
      val emojis = EmojiParser.extractEmojis(tweet.text).toList
      val newEmojiTweets = if (emojis.nonEmpty) totalEmojiTweets + 1 else totalEmojiTweets
      val newEmojiCounts = populateMap(emojiCounts, emojis)
      context become onMessage(newEmojiCounts, newEmojiTweets, newTotalTweets)
    case RequestPercentEmoji(id) =>
      val percent: Double = if (totalTweets > 0) {
        totalEmojiTweets.toDouble / totalTweets.toDouble * 100d
      } else 0d
      sender() ! ResponsePercentEmoji(id, BigDecimal(percent).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
    case RequestTopEmojis(id) =>
      val topEmojis = emojiCounts.toList.sortBy(x => x._2).reverse.take(5)
      sender() ! ResponseTopEmojis(id, topEmojis.map(x => x._1))
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
