package purluno.roar.db

import java.io.File

import android.content.Context
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import purluno.roar.db.DatabaseOpenHelper._

object DatabaseOpenHelper {
  val DATABASE_NAME = "roar.db"
  val DATABASE_VERSION = 1

  val TABLE_NAME_BITMAPS = "bitmaps"
  val CREATE_TABLE_BITMAPS =
    s"""CREATE TABLE $TABLE_NAME_BITMAPS (
       |  url TEXT,
       |  bitmap BLOB,
       |  PRIMARY KEY (url)
       |)
     """.stripMargin

  def databasePath(context: Context): String = {
    val dir = context.getExternalFilesDir(null)
    val file = new File(dir, DATABASE_NAME)
    file.getAbsolutePath
  }
}

class DatabaseOpenHelper(context: Context)
  extends SQLiteOpenHelper(context, databasePath(context), null, DATABASE_VERSION) {

  override def onCreate(db: SQLiteDatabase): Unit = {
    db.execSQL(CREATE_TABLE_BITMAPS)
  }

  override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int): Unit = {
    db.execSQL(s"DROP TABLE $TABLE_NAME_BITMAPS")
    db.execSQL(CREATE_TABLE_BITMAPS)
  }
}
