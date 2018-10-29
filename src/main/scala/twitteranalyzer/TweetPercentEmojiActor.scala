package twitteranalyzer

import akka.actor.Actor
import com.vdurmont.emoji.EmojiParser
import twitteranalyzer.TweetPercentEmojiActor.{RequestPercentEmoji, ResponsePercentEmoji}

import scala.collection.JavaConversions._

object TweetPercentEmojiActor {

  final case class RequestPercentEmoji(correlationId: String)

  final case class ResponsePercentEmoji(correlationId: String, percent: Double)

}

class TweetPercentEmojiActor extends Actor {
  override def receive: Receive = onMessage(0, 0)

  private def onMessage(accTotal: Long, accHasEmoji: Long): Receive = {
    case TweetMessage(tweet) =>
      val emojis = EmojiParser.extractEmojis(tweet.text).toList
      val step = if (emojis.nonEmpty) 1 else 0
      context become onMessage(accTotal + 1, accHasEmoji + step)
    case RequestPercentEmoji(id) =>
      val percent: Double = if (accTotal > 0) {
        accHasEmoji.toDouble / accTotal.toDouble * 100d
      } else 0d
      sender() ! ResponsePercentEmoji(id, BigDecimal(percent).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
  }
}
