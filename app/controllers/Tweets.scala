package controllers

import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current

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
      tweetsFuture
          .filter(response => response.status == play.api.http.Status.OK)
          .map { response =>
                  response.json
              } recover {
                  case _ => Json.obj("statuses" -> Json.arr(Json.obj("text" -> "Error retrieving tweets")))
              }
  }
}
