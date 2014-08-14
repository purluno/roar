package purluno.roar

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.view.{LayoutInflater, View}

/**
 * UI와 관련된 작업을 할 때 편리하게 사용할 수 있는 메소드가 있다.
 */
trait UiSupport {
  def getLayoutInflater(context: Context): LayoutInflater = {
    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]
  }

  def post(body: => Unit): Unit = {
    new Handler().post(new Runnable {
      override def run(): Unit = body
    })
  }

  def post(view: View)(body: => Unit): Unit = {
    view.post(new Runnable {
      override def run(): Unit = body
    })
  }

  def postDelayed(delayMillis: Long)(body: => Unit): Unit = {
    new Handler().postDelayed(new Runnable {
      override def run(): Unit = body
    }, delayMillis)
  }

  def postDelayed(view: View, delayMillis: Long)(body: => Unit): Unit = {
    view.postDelayed(new Runnable {
      override def run(): Unit = body
    }, delayMillis)
  }

  def runOnUiThread(activity: Activity)(body: => Unit): Unit = {
    activity.runOnUiThread(new Runnable {
      override def run(): Unit = body
    })
  }

}