package actors

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import play.api.libs.json.JsValue
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import controllers.Tweets

/** The out actor is wired in by Play Framework when this Actor is created.
  *   When a message is sent to out the Play Framework then sends it to the client WebSocket.
  *
  **/
class UserActor(out: ActorRef) extends Actor with ActorLogging {

  //The query is optional so that it starts as a None until the user issues the first query.
  var maybeQuery: Option[String] = None

  //Simulate events by periodically sending a message to self to fetch tweets.
  val tick = context.system.scheduler.schedule(Duration.Zero, 5.seconds, self, UserActor.FetchTweets)

  def receive = {
    //Handle the FetchTweets message to periodically fetch tweets if there is a query available.
    case UserActor.FetchTweets =>
      maybeQuery.map { query =>
        //sending a message to out sends it to the client websocket out by the Play Framework.
        Tweets.fetchTweets(query).map(tweetUpdate =>  out ! tweetUpdate)
      }

    case message: JsValue =>
      maybeQuery = (message \ "query").asOpt[String]
  }

  override def postStop() {
    tick.cancel()
  }

}

object UserActor {
  case object FetchTweets

  def props(out: ActorRef) = Props(new UserActor(out))
}
