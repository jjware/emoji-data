package twitteranalyzer

import java.time.{Duration, Instant}

import akka.actor.Actor
import twitteranalyzer.TweetRateActor.{RequestByHour, RequestByMinute, RequestBySecond, ResponseRate}

object TweetRateActor {

  final case class RequestByHour(correlationId: String)

  final case class RequestByMinute(correlationId: String)

  final case class RequestBySecond(correlationId: String)

  final case class ResponseRate(correlationId: String, value: Long)

}

class TweetRateActor extends Actor {
  override def receive: Receive = onMessage(Instant.now(), 0)

  def onMessage(start: Instant, total: Long): Receive = {
    case TweetMessage(_) => context become onMessage(start, total + 1)
    case RequestByHour(id) =>
      val duration = Duration.between(start, Instant.now())
      val tweetsPerMilli = total.toDouble / duration.toMillis.toDouble
      val tweetsPerHour = tweetsPerMilli * (1000 * 60 * 60)
      sender() ! ResponseRate(id, tweetsPerHour.toLong)
    case RequestByMinute(id) =>
      val duration = Duration.between(start, Instant.now())
      val tweetsPerMilli = total.toDouble / duration.toMillis.toDouble
      val tweetsPerMinute = tweetsPerMilli * (1000 * 60)
      sender() ! ResponseRate(id, tweetsPerMinute.toLong)
    case RequestBySecond(id) =>
      val duration = Duration.between(start, Instant.now())
      val tweetsPerMilli = total.toDouble / duration.toMillis.toDouble
      val tweetsPerSecond = tweetsPerMilli * 1000
      sender() ! ResponseRate(id, tweetsPerSecond.toLong)
  }
}
