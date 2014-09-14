package actors

import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.ws._
import play.api.libs.ws.ning.{NingWSClient, NingAsyncHttpClientConfigBuilder}
import play.api.libs.ws.DefaultWSClientConfig

trait TweetAPI {

    implicit val ec: ExecutionContext

    class TweetWS extends WSAPI {
        val builder = new NingAsyncHttpClientConfigBuilder(DefaultWSClientConfig())
        override def client: WSClient = new NingWSClient(builder.build())
        override def url(url: String): WSRequestHolder = client.url(url)
    }
}

object TweetAPI extends TweetAPI {

    val TWEET_PROXY_URL = "http://search-twitter-proxy.herokuapp.com/search/tweets"

    val GEOCODE_URL = "http://maps.googleapis.com/maps/api/geocode/json"

    override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    def tweetWS(query: String):Future[WSResponse] =
        new TweetWS().url(TWEET_PROXY_URL).withQueryString("q" -> query).get()

    def geocodeWS(query: String):Future[WSResponse] =
        new TweetWS().url(GEOCODE_URL).withQueryString(
            "sensor" -> "false",
            "address" -> query)
            .get()

}