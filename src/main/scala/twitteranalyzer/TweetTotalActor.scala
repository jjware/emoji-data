package twitteranalyzer

import akka.actor.{Actor, ActorLogging}
import twitteranalyzer.TweetTotalActor.{RequestTotal, ResponseTotal}

object TweetTotalActor {
  final case class RequestTotal(correlationId: String)
  final case class ResponseTotal(correlationId: String, total: Long)
}

class TweetTotalActor extends Actor with ActorLogging {
  private var total: Long = 0

  override def receive: Receive = {
    case TweetMessage(_) => total += 1
    case RequestTotal(id) => sender() ! ResponseTotal(id, total)
  }
}
