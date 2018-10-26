package twitteranalyzer

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}

@JsonIgnoreProperties(ignoreUnknown = true)
case class Tweet(
                  @JsonProperty("id") id: Long,
                  @JsonProperty("text") text: String,
                  @JsonProperty("entities") entities: TweetEntities
                )

@JsonIgnoreProperties(ignoreUnknown = true)
case class TweetEntities(
                          @JsonProperty("hashtags") hashTags: Array[TweetHashTag],
                          @JsonProperty("urls") urls: Array[TweetURL],
                          @JsonProperty("media") media: Array[TweetMedia]
                        )

@JsonIgnoreProperties(ignoreUnknown = true)
case class TweetHashTag(
                         @JsonProperty("text") text: String,
                         @JsonProperty("indices") indices: Array[Int]
                       )

@JsonIgnoreProperties(ignoreUnknown = true)
case class TweetURL(
                     @JsonProperty("url") url: String,
                     @JsonProperty("display_url") displayUrl: String,
                     @JsonProperty("expanded_url") expandedUrl: String,
                     @JsonProperty("text") indices: Array[Int]
                   )

@JsonIgnoreProperties(ignoreUnknown = true)
case class TweetMedia(
                       @JsonProperty("media_url") mediaUrl: String,
                       @JsonProperty("type") typ: String
                     )
