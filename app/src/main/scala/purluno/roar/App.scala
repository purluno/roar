package purluno.roar

import java.io.File

import akka.actor.{ActorRef, ActorSystem, Props}
import android.app.Application
import android.net.http.HttpResponseCache
import android.util.Log
import com.typesafe.config.{Config, ConfigFactory}
import purluno.roar.db.DatabaseActor
import purluno.roar.twitter.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

import scala.concurrent.ExecutionContext

/**
 * 앱 코드 어디에서나 접근할 수 있는 공용 객체를 가진다.
 */
object App {
  val config: Config = ConfigFactory.load()

  var actorSystem: ActorSystem = null

  var twitter: ActorRef = null

  var imageDownloader: ActorRef = null

  var databaseActor: ActorRef = null

  implicit def executionContext: ExecutionContext = actorSystem.dispatcher
}

/**
 * 앱 전반의 상태 관리를 위한 클래스.
 */
class App extends Application {

  import purluno.roar.App._

  /**
   * 앱 초기화 메소드. Akka actor system과 HTTP 응답 캐시를 초기화한다.
   */
  override def onCreate(): Unit = {
    initHttpResponseCache()
    initActors()
  }

  /**
   * Akka actor system 및 각종 actor 초기화.
   */
  def initActors(): Unit = {
    val conf = new ConfigurationBuilder()
      .setDebugEnabled(true)
      .setOAuthConsumerKey(config.getString("purluno.roar.oauth-consumer-key"))
      .setOAuthConsumerSecret(config.getString("purluno.roar.oauth-consumer-secret"))
      .build()
    val twitterFactory = new TwitterFactory(conf)

    actorSystem = ActorSystem()
    twitter = actorSystem.actorOf(Twitter.props(twitterFactory), "twitter")
    imageDownloader = actorSystem.actorOf(Props[ImageDownloader], "image-downloader")
    databaseActor = actorSystem.actorOf(DatabaseActor.props(this))
  }

  /**
   * HTTP 응답 캐시 초기화.
   */
  def initHttpResponseCache(): Unit = {
    try {
      val httpCacheDir = new File(getCacheDir, "http")
      val httpCacheSize = 10 * 1024 * 1024
      HttpResponseCache.install(httpCacheDir, httpCacheSize)
    } catch {
      case e: Throwable =>
        Log.w("App", "HTTP response cache installation failed", e)
    }
  }

}
