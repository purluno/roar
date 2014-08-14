package purluno.roar

import java.util.concurrent.TimeUnit

import akka.pattern._
import akka.util.Timeout
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.{WebView, WebViewClient}
import android.widget.Toast
import purluno.roar.App.executionContext
import purluno.roar.twitter.Twitter.{GetOAuthAccessToken, GetOAuthAccessTokenResult, GetOAuthRequestToken, GetOAuthRequestTokenResult}
import purluno.roar.twitter.TwitterUtil
import twitter4j.auth.RequestToken

import scala.util.{Failure, Success}

/**
 * `LoginActivity`의 지원 객체.
 */
class LoginActivitySupport(protected val activity: LoginActivity) extends ActivitySupport {

  activity.setContentView(R.layout.activity_login)

  /**
   * 트위터 OAuth 로그인 과정에 필요한 웹뷰.
   *
   * 트위터는 웹으로 로그인(애플리케이션 허용) 기능을 제공하기 때문에 웹뷰를 활용해야 한다.
   */
  private val webView: WebView = activity.findViewById(R.id.web_view).asInstanceOf[WebView]

  /**
   * 접근 토큰(access token)을 얻기 위한 중간 단계인 요청 토큰(request token)을 임시로 보관하는
   * 공간.
   */
  private var requestToken: Option[RequestToken] = None

  implicit val timeout = Timeout(10, TimeUnit.SECONDS)

  /**
   * 로그인이 되어있지 않으면 웹뷰에 콜백 핸들러를 걸고 로그인 과정을 시작한다.
   *
   * `LoginActivity.onCreate()`에서 명시적으로 호출되어야 한다.
   */
  def onCreate(savedInstanceState: Bundle): Unit = {
    RoarDroid.load(activity)
    if (TwitterUtil.isAuthEnabled) {
      startActivity(classOf[MainActivity])
      activity.finish()
    } else {
      webView.setWebViewClient(OAuthCallbackHandler)
      if (savedInstanceState != null) {
        runOnUiThread {
          webView.restoreState(savedInstanceState)
        }
      } else {
        startSignIn()
      }
    }
  }

  /**
   * 화면 회전 등에 대비한 웹뷰 상태 저장 메소드
   */
  def onSaveInstanceState(outState: Bundle) {
    webView.saveState(outState)
  }

  /**
   * 로그인을 시작하는 메소드.
   *
   * 요청 토큰을 먼저 얻고, 접근 토큰을 얻기 위해 웹뷰에 트위터 로그인 페이지를 연결한다.
   */
  def startSignIn(): Unit = {
    val dialog = progressDialog("요청 토큰을 얻는 중입니다.")
    dialog.show()
    App.twitter ? GetOAuthRequestToken("roar://purluno.wendies.org/") andThen {
      case _ =>
        dialog.dismiss()

    } onComplete {
      case Success(GetOAuthRequestTokenResult(token)) =>
        this.requestToken = Some(token)
        val url = token.getAuthenticationURL
        runOnUiThread {
          webView.loadUrl(url)
        }

      case Failure(t) =>
        postDelayed(2000) {
          activity.finish()
        }

      case _ =>
        Log.e("LoginActivitySupport", "It should be impossible to reach this code.")
        throw new Exception("It should be impossible to reach this code.")
    }
  }

  /**
   * access token을 받아와 트위터 로그인을 완성한다.
   *
   * 웹뷰 트위터 로그인 페이지에서 보내온 verifier 정보를 이용한다.
   * 내부 객체 `OAuthCallbackHandler`에 의해 호출된다.
   *
   * @param verifier OAuth verifier 값
   */
  private def continueSignIn(verifier: String): Unit = {
    val dialog = progressDialog("접근 토큰을 얻는 중입니다.")
    dialog.show()
    App.twitter ? GetOAuthAccessToken(requestToken.get, verifier) andThen {
      case _ =>
        dialog.dismiss()

    } onComplete {
      case Success(GetOAuthAccessTokenResult(token)) =>
        RoarDroid.save(activity, token)
        runOnUiThread {
          toast("로그인에 성공했습니다.", Toast.LENGTH_SHORT).show()
          startActivity(classOf[MainActivity])
          activity.finish()
        }

      case Failure(t) =>
        runOnUiThread {
          toast("로그인에 실패했습니다. (접근 토큰 획득 실패)", Toast.LENGTH_LONG).show()
          postDelayed(2000) {
            activity.finish()
          }
        }

      case _ =>
        Log.e("LoginActivitySupport", "It should be impossible to reach this code.")
        throw new Exception("It should be impossible to reach this code.")
    }
  }

  /**
   * 웹뷰에서 OAuth 콜백 URL을 감지하여 OAuth verifier 값을 추출하고
   * `signInContinue()`를 호출해주는 핸들러 객체.
   *
   * `WebView.setWebViewClient`를 통해 지정되어야 한다.
   */
  private object OAuthCallbackHandler extends WebViewClient {
    override def shouldOverrideUrlLoading(view: WebView, url: String): Boolean = {
      val uri = Uri.parse(url)
      if (uri.getScheme == "roar") {
        continueSignIn(uri.getQueryParameter("oauth_verifier"))
        true
      } else {
        false
      }
    }
  }

}
