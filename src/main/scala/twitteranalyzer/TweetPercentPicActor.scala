package twitteranalyzer

import akka.actor.Actor
import twitteranalyzer.TweetPercentPicActor.{RequestPercentPic, ResponsePercentPic}

import scala.annotation.tailrec

object TweetPercentPicActor {

  final case class RequestPercentPic(correlationId: String)

  final case class ResponsePercentPic(correlationId: String, percent: Double)

}

class TweetPercentPicActor extends Actor {
  override def receive: Receive = onMessage(0, 0)

  private def onMessage(accTotal: Long, accHasPic: Long): Receive = {
    case TweetMessage(tweet) =>
      val step = if (containsPicUrl(tweet.entities.urls)) 1 else 0
      context become onMessage(accTotal + 1, accHasPic + step)
    case RequestPercentPic(id) =>
      val percent: Double = if (accTotal > 0) {
        accHasPic.toDouble / accTotal.toDouble * 100d
      } else 0d
      sender() ! ResponsePercentPic(id, BigDecimal(percent).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
  }

  @tailrec
  private def containsPicUrl(tweetUrls: List[TweetURL]): Boolean = {
    if (tweetUrls.nonEmpty) {
      val tweetUrl = tweetUrls.head.expandedUrl
      if (tweetUrl.contains("pic.twitter.com") || tweetUrl.contains("instagram")) true
      else containsPicUrl(tweetUrls.drop(1))
    } else {
      false
    }
  }
}
