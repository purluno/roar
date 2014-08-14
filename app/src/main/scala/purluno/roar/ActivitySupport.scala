package purluno.roar

import android.app.{Activity, ProgressDialog}
import android.content.Intent
import android.widget.Toast

/**
 * ActivitySupport 클래스들이 공통으로 활용할만한 기능을 편하게 쓸 수 있도록 지원하는 trait.
 */
trait ActivitySupport extends UiSupport {
  protected val activity: Activity

  /**
   * 작업이 진행될 동안 사용자가 기다리게 하는 취소 불가 대화상자를 준비한다.
   *
   * @param message 표시할 메시지
   * @return 진행 대화상자 객체
   */
  def progressDialog(message: CharSequence): ProgressDialog = {
    val dialog = new ProgressDialog(activity)
    dialog.setIndeterminate(true)
    dialog.setCancelable(false)
    dialog.setMessage(message)
    dialog
  }

  /**
   * 화면에 일정 시간동안 간단한 메시지를 표시하는 객체를 준비한다.
   * @param text 표시할 메시지
   * @param duration 유지 시간 (Toast.LENGTH_LONG, Toast.LENGTH_SHORT 중 하나)
   * @return Toast 객체
   */
  def toast(text: CharSequence, duration: Int): Toast = {
    Toast.makeText(activity, text, duration)
  }

  /**
   * 해당 코드를 UI 스레드에서 실행한다.
   *
   * @param body 코드
   */
  def runOnUiThread[U](body: => U): Unit = {
    activity.runOnUiThread(new Runnable {
      override def run(): Unit = body
    })
  }

  /**
   * Activity를 간편하게 시작하는 메소드
   *
   * @param activityClass 시작할 activity의 클래스
   */
  def startActivity(activityClass: Class[_ <: Activity]): Unit = {
    activity.startActivity(new Intent(activity, activityClass))
  }
}
