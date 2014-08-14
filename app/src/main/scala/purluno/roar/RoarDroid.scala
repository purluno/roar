package purluno.roar

import android.content.Context
import android.util.Log
import purluno.roar.twitter.Twitter.SetOAuthAccessToken
import purluno.roar.twitter.TwitterUtil
import twitter4j.auth.AccessToken

/**
 * 현재는 트위터 로그인 정보(접근 토큰)를 저장 공간에 저장하거나 읽어오는 기능이 있는 유틸리티 객체.
 */
object RoarDroid {
  /**
   * 저장했던 접근 토큰(access token)을 읽어와 로그인 상태를 지속하게 한다.
   */
  def load(context: Context): Unit = {
    if (TwitterUtil.isAuthEnabled) {
      return
    }
    val pref = context.getSharedPreferences("roar", 0)
    val token = pref.getString("token", "")
    val tokenSecret = pref.getString("tokenSecret", "")
    Log.d("RoarDroid", s"Loading: token = $token, secret = $tokenSecret")
    if (!token.isEmpty) {
      val accessToken = new AccessToken(token, tokenSecret)
      App.twitter ! SetOAuthAccessToken(accessToken)
    }
  }

  /**
   * 접근 토큰(access token)을 저장 공간에 보관한다.
   *
   * @param context
   * @param token 접근 토큰(access token)
   */
  def save(context: Context, token: AccessToken): Unit = {
    context.getSharedPreferences("roar", 0)
      .edit()
      .putString("token", token.getToken)
      .putString("tokenSecret", token.getTokenSecret)
      .commit()
  }
}
