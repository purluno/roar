package purluno.roar

import java.net.URL

import akka.actor.Actor
import akka.pattern._
import android.graphics.{Bitmap, BitmapFactory}
import purluno.roar.db.DatabaseActor
import purluno.roar.util.MutableLruMap

import scala.collection.mutable
import scala.util.Success

object ImageDownloader {

  case class Get(url: String, useCache: Boolean = true)

  case class Result(bitmap: Bitmap)

}

class ImageDownloader extends Actor {

  import purluno.roar.ImageDownloader._

  val cache: mutable.Map[String, Bitmap] = MutableLruMap.empty(100)

  def load(url: String): Bitmap = {
    val in = new URL(url).openStream()
    try {
      BitmapFactory.decodeStream(in)
    } finally {
      in.close()
    }
  }

  def store(url: String, bitmap: Bitmap): Unit = {
    cache += (url -> bitmap)
    App.databaseActor ! DatabaseActor.PutBitmap(url, bitmap)
  }

  def receive = {
    case Get(url, useCache) =>
      if (useCache) {
        cache.get(url) match {
          case Some(bitmap) =>
            sender() ! Result(bitmap)
          case None =>
            val sender = sender()
            App.databaseActor ? DatabaseActor.GetBitmap(url) onComplete {
              case Success(DatabaseActor.GetBitmapResult(Some(bitmap))) =>
                sender ! Result(bitmap)
              case Success(DatabaseActor.GetBitmapResult(None)) =>
                val bitmap = load(url)
                sender() ! Result(bitmap)
                store(url, bitmap)
            }
        }
      } else {
        val bitmap = load(url)
        sender() ! Result(bitmap)
        store(url, bitmap)
      }
  }
}
