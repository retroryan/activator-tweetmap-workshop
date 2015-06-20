package controllers

import actors.UserActor
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, JsValue, Json, __}
import play.api.libs.ws._
import play.api.mvc.{Action, Controller, WebSocket}

import scala.concurrent.Future
import scala.util.Random


object Tweets extends Controller {

  def index = Action {
    Ok(views.html.index("Hello Play Framework"))
  }

  /**
   * A reactive request is made in Play by returning a Future[Result].  This makes the request asynchronous
   * since the server doesn't block waiting for the response.  This frees the server thread to handle other requests.
   * The callback to the Future is added as a map, which maps the results to a new value when the Future completes.
   * The results of the Future in this example are mapped to a result (HTTP 200 OK) that gets returned to the client.
   **/
  def search(query: String) = Action.async {
    fetchTweets(query).map(tweets => Ok(tweets))
  }

  /**
   * Fetch the latest tweets and return the Future[JsValue] of the results.
   * This fetches the tweets asynchronously and fulfills the Future when the results are returned by calling the function.
   * The results are first filtered and only returned if the result status was OK.
   * Then the results are mapped (or transformed) to JSON.
   **/
  def fetchTweets(query: String): Future[JsValue] = {
    val tweetsFuture = WS.url("http://twitter-search-proxy.herokuapp.com/search/tweets").withQueryString("q" -> query).get()
    tweetsFuture.flatMap { response =>
      tweetLatLon((response.json \ "statuses").as[Seq[JsValue]])
    } recover {
      case _ => Seq.empty[JsValue]
    } map { tweets =>
      Json.obj("statuses" -> tweets)
    }
  }

  def ws = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    UserActor.props(out)
  }

  private def putLatLonInTweet(latLon: JsValue) = __.json.update(__.read[JsObject].map(_ + ("coordinates" -> Json.obj("coordinates" -> latLon))))

  private def tweetLatLon(tweets: Seq[JsValue]): Future[Seq[JsValue]] = {
    val tweetsWithLatLonFutures = tweets.map { tweet =>
      if ((tweet \ "coordinates" \ "coordinates").asOpt[Seq[Double]].isDefined) {
        Future.successful(tweet)
      } else {
        val latLonFuture: Future[(Double, Double)] = (tweet \ "user" \ "location").asOpt[String].map(lookupLatLon).getOrElse(Future.successful(randomLatLon))
        latLonFuture.map { latLon =>
          tweet.transform(putLatLonInTweet(Json.arr(latLon._2, latLon._1))).getOrElse(tweet)
        }
      }
    }

    Future.sequence(tweetsWithLatLonFutures)
  }

  private def randomLatLon: (Double, Double) = ((Random.nextDouble * 180) - 90, (Random.nextDouble * 360) - 180)

  private def lookupLatLon(query: String): Future[(Double, Double)] = {
    val locationFuture = WS.url("http://maps.googleapis.com/maps/api/geocode/json").withQueryString(
      "sensor" -> "false",
      "address" -> query
    ).get()

    locationFuture.map { response =>
      (response.json \\ "location").headOption.map { location =>
        ((location \ "lat").as[Double], (location \ "lng").as[Double])
      }.getOrElse(randomLatLon)
    }
  }


}
