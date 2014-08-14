package purluno.roar.twitter

import akka.actor.{Actor, Props}
import twitter4j._
import twitter4j.auth.{AccessToken, RequestToken}

object Twitter {

  trait Request

  trait Result

  case class GetOAuthAccessToken(requestToken: RequestToken, oauthVerifier: String) extends Request

  case class GetOAuthAccessTokenResult(accessToken: AccessToken) extends Result

  case class GetOAuthRequestToken(callbackUrl: String) extends Request

  case class GetOAuthRequestTokenResult(requestToken: RequestToken) extends Result

  case class GetScreenName() extends Request

  case class GetScreenNameResult(screenName: String) extends Result

  case class GetTimeline(timelineType: TimelineType,
                         paging: Paging = Paging(),
                         noCache: Boolean = false) extends Request

  case class GetTimelineResult(timelineType: TimelineType,
                               timeline: Seq[Status],
                               rateLimitStatus: Option[RateLimitStatus],
                               fromCache: Boolean = false) extends Result

  case class IsAuthEnabled() extends Request

  case class IsAuthEnabledResult(authEnabled: Boolean) extends Result

  case class SetOAuthAccessToken(accessToken: AccessToken) extends Request

  def props(twitterFactory: TwitterFactory): Props = Props(new Twitter(twitterFactory))
}

class Twitter(twitterFactory: TwitterFactory) extends Actor {

  import purluno.roar.twitter.Twitter._

  val gatherer = context.actorOf(Gatherer.props(twitterFactory))

  val timelineCachers = context.actorOf(Props[TimelineCachers])

  def receive = {
    case msg@GetTimeline(timelineType, _, noCache) =>
      if (noCache) {
        gatherer forward msg
      } else {
        timelineCachers forward msg
      }

    case TimelineCacher.LoadFailure(request) =>
      gatherer forward request

    case msg: TimelineCacher.Store =>
      timelineCachers forward msg

    case msg: Request =>
      gatherer forward msg

    case etc =>
      throw new IllegalArgumentException(s"Received an unknown message: $etc")
  }
}
