package twitteranalyzer

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.fasterxml.jackson.databind.ObjectMapper
import twitteranalyzer.TweetPropagatingActor.Message

class TweetPropagatingActor(actors: List[ActorRef], mapper: ObjectMapper) extends Actor with ActorLogging {
  override def receive: Receive = {
    case Message(text) =>
      try {
        val tweet = mapper.readValue(text, classOf[Tweet])
        if (tweet.id != 0) {
          for (actor <- actors) {
            actor ! TweetMessage(tweet)
          }
        }
      } catch {
        case e: Exception => log.error("exception while deserializing tweet: {}", e)
      }
  }
}

object TweetPropagatingActor {
  def props(actors: List[ActorRef], mapper: ObjectMapper): Props = Props(new TweetPropagatingActor(actors, mapper))

  final case class Message(text: String)
}
