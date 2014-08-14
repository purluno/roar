package purluno.roar

import android.graphics.Color
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.widget._
import purluno.roar.App._
import purluno.roar.twitter.TwitterUtil
import twitter4j.TwitterException

import scala.util.Success

/**
 * `MainActivity`를 총괄하는 클래스.
 */
class MainActivitySupport(protected val activity: MainActivity) extends ActivitySupport
with SwipeRefreshLayout.OnRefreshListener
with AbsListView.OnScrollListener {

  activity.setContentView(R.layout.activity_main)

  /**
   * 위로 스크롤했을 때 목록을 새로고침해주는 기능을 가진 레이아웃.
   */
  private val swipeRefreshLayout: SwipeRefreshLayout =
    activity.findViewById(R.id.layout_swipe_refresh).asInstanceOf[SwipeRefreshLayout]

  /**
   * 트윗 목록을 스크롤하여 보여주는 뷰.
   */
  private val listView: ListView = activity.findViewById(R.id.list).asInstanceOf[ListView]

  /**
   * 트윗 목록을 관리하고 `listView`에 각 항목에 대한 뷰를 제공하는 객체.
   */
  private val adapter: TwitterStatusAdapter = new TwitterStatusAdapter(activity)

  /**
   * `listView` 초기화 메소드.
   */
  def initListView(): Unit = {
    listView.setAdapter(adapter)
    listView.setOnScrollListener(this)
  }

  /**
   * Swipe refresh layout을 초기화한다.
   */
  def initSwipeRefreshLayout(): Unit = {
    swipeRefreshLayout.setOnRefreshListener(this)
    swipeRefreshLayout.setColorSchemeColors(
      Color.rgb(0xcc, 0x00, 0x00),
      Color.rgb(0xff, 0x88, 0x00),
      Color.rgb(0x66, 0x99, 0x00),
      Color.rgb(0x00, 0x99, 0xcc)
    )
  }

  /**
   * Activity 초기화 메소드.
   *
   * @param savedInstanceState 이전 상태
   */
  def onCreate(savedInstanceState: Bundle): Unit = {
    RoarDroid.load(activity)
    if (!TwitterUtil.isAuthEnabled) {
      startActivity(classOf[LoginActivity])
      activity.finish()
    } else {
      initSwipeRefreshLayout()
      adapter.refresh(false)
      initListView()
    }
  }

  /**
   * Swipe refresh layout의 갱신 동작 이벤트 핸들러.
   */
  def onRefresh(): Unit = {
    adapter.refresh(true) match {
      case None =>
        swipeRefreshLayout.setRefreshing(false)
      case Some(future) =>
        future andThen {
          case _ =>
            runOnUiThread {
              swipeRefreshLayout.setRefreshing(false)
            }
        } andThen {
          case Success(_) =>
            runOnUiThread {
              toast("갱신했습니다.", Toast.LENGTH_SHORT).show()
            }
        } onFailure (twitterFailureToast)
    }
  }

  /**
   * `listView`를 거의 맨 아래까지 스크롤하면 그 뒤의 트윗을 더 읽어온다.
   *
   * @param view 리스트뷰 객체
   * @param firstVisibleItem 현재 화면에 제일 위에 보이는 항목의 인덱스
   * @param visibleItemCount 현재 화면에 보이는 항목의 개수
   * @param totalItemCount 리스트뷰(어댑터)가 가진 항목의 총 개수
   */
  override def onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int): Unit = {
    if (firstVisibleItem + visibleItemCount < totalItemCount)
      return
    for (future <- adapter.loadMore())
      future onFailure twitterFailureToast
  }

  override def onScrollStateChanged(view: AbsListView, scrollState: Int): Unit = ()

  /**
   * 트위터 관련 동작 실패에 대한 UI 처리 partial function
   */
  val twitterFailureToast: PartialFunction[Throwable, Unit] = {
    case e: TwitterException =>
      runOnUiThread {
        if (e.getStatusCode == 429) {
          val min = (e.getRetryAfter / 60.0).ceil.toInt
          toast(s"갱신 한도 초과. ${min}분 후에 다시 시도하세요.", Toast.LENGTH_LONG).show()
        } else {
          toast("트위터 요청 실패, 불러오는 데 실패했습니다.", Toast.LENGTH_LONG).show()
        }
      }
    case t: Throwable =>
      runOnUiThread {
        toast("알 수 없는 이유로 불러오는 데 실패했습니다.", Toast.LENGTH_LONG).show()
      }
  }

}
