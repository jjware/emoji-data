package twitteranalyzer

import akka.actor.Actor
import twitteranalyzer.TweetPercentUrlsActor.{RequestPercentUrl, ResponsePercentUrl}

object TweetPercentUrlsActor {

  final case class RequestPercentUrl(correlationId: String)

  final case class ResponsePercentUrl(correlationId: String, percent: Double)

}

class TweetPercentUrlsActor extends Actor {
  override def receive: Receive = onMessage(0, 0)

  private def onMessage(accTotal: Long, accHasUrl: Long): Receive = {
    case TweetMessage(tweet) =>
      val containsUrl = tweet.entities.urls.nonEmpty
      val step = if (containsUrl) 1 else 0
      context become onMessage(accTotal + 1, accHasUrl + step)
    case RequestPercentUrl(id) =>
      val percent: Double = if (accTotal > 0) {
        accHasUrl.toDouble / accTotal.toDouble * 100d
      } else 0d
      sender() ! ResponsePercentUrl(id, BigDecimal(percent).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
  }
}
