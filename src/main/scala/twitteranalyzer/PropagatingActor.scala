package twitteranalyzer

import akka.actor.{Actor, ActorRef, Props}

class PropagatingActor(actors: List[ActorRef]) extends Actor {
  override def receive: Receive = ???
}

object PropagatingActor {
  def props(actors: List[ActorRef]): Props = Props(new PropagatingActor(actors))

  final case class Message(tweet: Tweet)
}
