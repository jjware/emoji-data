package twitteranalyzer

import akka.actor.{Actor, ActorLogging}

class TweetPrintingActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case TweetMessage(tweet) => log.info("{}", tweet)
  }
}
