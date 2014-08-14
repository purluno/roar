package purluno.roar

import android.os.Bundle
import purluno.roar.twitter.TwitterUtil

/**
 * 인트로 화면 activity 지원 클래스
 */
class IntroActivitySupport(protected val activity: IntroActivity) extends ActivitySupport {
  activity.setContentView(R.layout.activity_intro)

  /**
   * 로그인 상태에 따라 2초 후 로그인 화면 또는 메인 화면으로 이동한다.
   */
  def onCreate(savedInstanceState: Bundle): Unit = {
    RoarDroid.load(activity)
    val nextActivity =
      if (TwitterUtil.isAuthEnabled) classOf[LoginActivity]
      else classOf[MainActivity]
    postDelayed(2000) {
      startActivity(nextActivity)
      activity.finish()
    }
  }
}
