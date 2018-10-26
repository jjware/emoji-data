package twitteranalyzer

import akka.actor.Actor
import com.vdurmont.emoji.EmojiParser
import twitteranalyzer.TweetEmojiActor.{RequestPercentEmoji, ResponsePercentEmoji}

import scala.collection.mutable

object TweetEmojiActor {
  final case class RequestPercentEmoji(correlationId: String)
  final case class ResponsePercentEmoji(correlationId: String, percent: Int)
  final case class RequestTopEmojis(correlationId: String)
  final case class ResponseTopEmojis(correlationId: String, emojis: List[String])
}

class TweetEmojiActor extends Actor {
  private val emojiCounts: mutable.Map[String, Long] = new mutable.HashMap()
  private var totalTweetsWithEmoji = 0
  private var totalTweets: Long = 0

  override def receive: Receive = {
    case TweetMessage(tweet) => {
      totalTweets += 1
      val emojis = EmojiParser.extractEmojis(tweet.text)
      if (!emojis.isEmpty) {
        totalTweetsWithEmoji += 1
      }

      for (emoji <- emojis) {
        if (emojiCounts.contains(emoji)) {
          emojiCounts.put(emoji, emojiCounts.get(emoji).map(_ + 1).getOrElse(1))
        }
      }
    }
    case RequestPercentEmoji(id) => {
      val percent = (totalTweetsWithEmoji / totalTweets) * 100
      sender() ! ResponsePercentEmoji(id, percent.toInt)
    }
  }
}
