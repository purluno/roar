package purluno.roar.twitter

import java.util.concurrent.TimeUnit

import akka.pattern._
import akka.util.Timeout
import purluno.roar.App
import purluno.roar.twitter.Twitter.{IsAuthEnabled, IsAuthEnabledResult}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object TwitterUtil {
  implicit val timeout = Timeout(10, TimeUnit.SECONDS)

  def isAuthEnabled: Boolean = {
    val f = App.twitter ? IsAuthEnabled()
    Await.result(f, Duration.Inf) match {
      case IsAuthEnabledResult(result) =>
        result
    }
  }
}
