package twitteranalyzer

case class Tweet(id: Int, text: String)
case class TweetEntities(hashTags: List[TweetHashTag], urls: List[String])
case class TweetHashTag(text: String, indices: List[Int])
