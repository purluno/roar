package purluno.roar.util

import java.text.{DateFormat, SimpleDateFormat}
import java.util.{Calendar, Date}

object DateFormats {
  implicit lazy val datetimeDefault = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  implicit lazy val dateDefault = new SimpleDateFormat("yyyy-MM-dd")

  implicit lazy val dateMonthDay = new SimpleDateFormat("MM-dd")

  implicit lazy val timeDefault = new SimpleDateFormat("HH:mm:ss")

  def date2string(date: Date)(implicit format: DateFormat): String = format.format(date)

  def elapsedString(date: Date): String = {
    val now = System.currentTimeMillis
    val cal = Calendar.getInstance()
    cal.setTime(date)
    val point = cal.getTimeInMillis
    val d = now - point
    List(
      (d / 1000 / 60 / 60 / 24, "일"),
      (d / 1000 / 60 / 60, "시간"),
      (d / 1000 / 60, "분"),
      (d / 1000, "초")
    ).find { case (n, _) => n != 0} match {
      case Some((a, b)) =>
        a + b
      case None =>
        "지금"
    }
  }
}
