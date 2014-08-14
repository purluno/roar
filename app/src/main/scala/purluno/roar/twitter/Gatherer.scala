package purluno.roar.twitter

import akka.actor.{Actor, Props}
import android.util.Log
import purluno.roar.twitter.Twitter._
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken

object Gatherer {
  def props(twitterFactory: TwitterFactory): Props = Props(new Gatherer(twitterFactory))
}

class Gatherer(twitterFactory: TwitterFactory) extends Actor {

  var accessToken: Option[AccessToken] = None

  def getTwitter: twitter4j.Twitter = {
    accessToken match {
      case Some(token) =>
        twitterFactory.getInstance(token)
      case None =>
        twitterFactory.getInstance()
    }
  }

  def getTimeline(timelineType: TimelineType, paging: Paging): StatusResponse = {
    timelineType match {
      case TimelineType.Home =>
        if (paging.isEmpty) {
          StatusResponse(getTwitter.getHomeTimeline())
        } else {
          StatusResponse(getTwitter.getHomeTimeline(paging.asTwitter4j))
        }
      case _ =>
        throw new IllegalArgumentException(s"Unknown timeline type: $timelineType")
    }
  }

  def receive = {
    case msg@GetTimeline(timelineType, paging, _) =>
      val StatusResponse(timeline, rateLimitStatus) = getTimeline(timelineType, paging)
      val result = GetTimelineResult(timelineType, timeline, rateLimitStatus)
      Log.d("Gatherer", s"A work is complete; type=$timelineType, size=${timeline.size}")
      sender() ! result
      context.parent ! TimelineCacher.Store(msg, result)

    case GetOAuthRequestToken(callbackUrl) =>
      val requestToken = getTwitter.getOAuthRequestToken(callbackUrl)
      sender() ! GetOAuthRequestTokenResult(requestToken)

    case GetOAuthAccessToken(requestToken, oauthVerifier) =>
      val accessToken = getTwitter.getOAuthAccessToken(requestToken, oauthVerifier)
      sender() ! GetOAuthAccessTokenResult(accessToken)

    case GetScreenName() =>
      val screenName = getTwitter.getScreenName
      sender() ! GetScreenNameResult(screenName)

    case IsAuthEnabled() =>
      val isAuthEnabled = getTwitter.getAuthorization.isEnabled
      sender() ! IsAuthEnabledResult(isAuthEnabled)

    case SetOAuthAccessToken(accessToken) =>
      this.accessToken = Some(accessToken)

    case msg =>
      throw new IllegalArgumentException(s"Received an unknown message: $msg")
  }
}
