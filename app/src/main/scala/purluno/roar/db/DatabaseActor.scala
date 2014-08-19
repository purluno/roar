package purluno.roar.db

import java.io.ByteArrayOutputStream

import akka.actor.{Props, Actor}
import android.content.{ContentValues, Context}
import android.graphics.Bitmap.CompressFormat
import android.graphics.{Bitmap, BitmapFactory}
import purluno.roar.db.DatabaseActor.{GetBitmap, GetBitmapResult, PutBitmap}
import purluno.roar.db.DatabaseOpenHelper._

object DatabaseActor {

  case class GetBitmap(url: String)

  case class GetBitmapResult(bitmap: Option[Bitmap])

  case class PutBitmap(url: String, bitmap: Bitmap)

  def props(context: Context): Props = Props(new DatabaseActor(context))

}

class DatabaseActor(context: Context) extends Actor {
  val databaseOpenHelper = new DatabaseOpenHelper(context)

  def receive = {
    case GetBitmap(url) =>
      val db = databaseOpenHelper.getReadableDatabase
      try {
        val cursor = db.query(TABLE_NAME_BITMAPS, Array("bitmap"), "url = ?", Array(url), null, null, null)
        if (cursor.getCount == 0) {
          sender() ! GetBitmapResult(None)
        } else {
          cursor.moveToNext()
          val blob = cursor.getBlob(0)
          val bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length)
          sender() ! GetBitmapResult(Some(bitmap))
        }
      } finally {
        db.close()
      }

    case PutBitmap(url: String, bitmap: Bitmap) =>
      val db = databaseOpenHelper.getWritableDatabase
      try {
        val stream = new ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 0, stream)
        val values = new ContentValues()
        values.put("url", url)
        values.put("bitmap", stream.toByteArray)
        db.insert(TABLE_NAME_BITMAPS, null, values)
      } finally {
        db.close()
      }
  }
}
