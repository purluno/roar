package purluno.roar

import akka.pattern._
import akka.util.Timeout
import android.app.Activity
import android.graphics.Bitmap
import android.view.{View, ViewGroup}
import android.widget.{BaseAdapter, ImageView, TextView}
import purluno.roar.App._
import purluno.roar.twitter.Twitter.{GetTimeline, GetTimelineResult}
import purluno.roar.twitter.{Paging, TimelineType}
import purluno.roar.util.DateFormats
import twitter4j.Status

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * 트윗 목록을 관리하고 `ListView`에 항목 뷰를 제공하는 어댑터 클래스.
 *
 * @param activity 어댑터를 사용할 `Activity`
 */
class TwitterStatusAdapter(activity: Activity) extends BaseAdapter with UiSupport {

  /**
   * Actor로부터 받을 응답의 최대 대기 시간.
   */
  private implicit val timeout = Timeout(30 seconds)

  /**
   * 반복되는 프로필 이미지 재활용을 위한 캐시
   */
  private var profileImageCache: Map[Long, Bitmap] = Map.empty

  /**
   * 트윗 목록을 보관하는 콜렉션
   */
  private var seq: IndexedSeq[Status] = IndexedSeq.empty

  /**
   * 트윗 목록 받아오는 작업이 중복되지 않도록 확인하는 상태 변수
   */
  var working: Boolean = false

  /**
   * 어댑터의 데이터셋을 갱신한다. (트윗 목록 갱신)
   *
   * @param f 데이터셋 갱신 함수
   */
  def edit(f: IndexedSeq[Status] => IndexedSeq[Status]): TwitterStatusAdapter = {
    seq = f(seq)
    runOnUiThread(activity) {
      notifyDataSetChanged()
    }
    this
  }

  /**
   * 현재 어댑터가 보유한 트윗의 개수.
   *
   * @return 어댑터가 보유한 트윗의 개수
   */
  override def getCount: Int = seq.size

  /**
   * 특정 인덱스의 트윗(status; 상태라고도 부른다.)을 가져온다.
   *
   * @param position 트윗의 인덱스
   * @return 트윗 객체
   */
  override def getItem(position: Int): Status = seq(position)

  /**
   * 특정 인덱스의 트윗의 ID 값을 가져온다.
   *
   * @param position 트윗의 인덱스
   * @return 트윗의 ID
   */
  override def getItemId(position: Int): Long = seq(position).getId

  /**
   * `ListView`에 의해 호출되어 트윗 하나에 해당하는 `View` 객체를 제공한다.
   *
   * @param position 보여줄 트윗의 인덱스
   * @param convertView 기존에 활용한 뷰 객체
   * @param parent `ListView` 객체일 것이다.
   * @return 트윗 하나에 해당하는 `View` 객체
   */
  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val rootView =
      if (convertView != null) convertView
      else getLayoutInflater(activity).inflate(R.layout.adapter_twitter, parent, false)

    val profileView = rootView.findViewById(R.id.image_profile).asInstanceOf[ImageView]
    val nameView = rootView.findViewById(R.id.text_name).asInstanceOf[TextView]
    val screenNameView = rootView.findViewById(R.id.text_screen_name).asInstanceOf[TextView]
    val elapsedTimeView = rootView.findViewById(R.id.text_elapsed_time).asInstanceOf[TextView]
    val textView = rootView.findViewById(R.id.text_text).asInstanceOf[TextView]

    val status = getItem(position)
    val user = status.getUser

    imageDownloader ? ImageDownloader.Get(user.getProfileImageURL) onSuccess {
      case ImageDownloader.Result(bitmap) =>
        post(profileView) {
          profileView.setImageBitmap(bitmap)
          profileView.refreshDrawableState()
        }
    }

    nameView.setText(status.getUser.getName)
    screenNameView.setText("@" + status.getUser.getScreenName)
    elapsedTimeView.setText(DateFormats.elapsedString(status.getCreatedAt))
    textView.setText(status.getText)
    rootView
  }

  /**
   * 트윗 목록을 갱신하는 작업을 비동기로 진행하며 이에 대한 Future 객체를 리턴한다.
   *
   * 이미 어댑터가 다른 작업을 하는 중이면 아무것도 하지 않고 None을 리턴한다.
   *
   * @return 작업에 대한 Future 객체, 또는 None
   */
  def refresh(noCache: Boolean = true): Option[Future[TwitterStatusAdapter]] = {
    if (!workExclusively()) {
      return None
    }

    val f = App.twitter ? GetTimeline(TimelineType.Home, noCache = noCache) collect {
      case GetTimelineResult(timelineType, timeline, _, _) =>
        edit { _ =>
          IndexedSeq.empty ++ timeline
        }
    } andThen {
      case _ =>
        working = false
    }
    Some(f)
  }

  /**
   * 트윗 목록을 추가로 더 읽어오는 작업을 비동기로 진행하며 이에 대한 Future 객체를 리턴한다.
   *
   * 이미 어댑터가 다른 작업을 하는 중이면 아무것도 하지 않고 None을 리턴한다.
   *
   * @return 작업에 대한 Future 객체, 또는 None
   */
  def loadMore(): Option[Future[TwitterStatusAdapter]] = {
    if (!workExclusively()) {
      return None
    } else if (seq.isEmpty) {
      return refresh()
    }

    val last = seq.last.getId
    val f = App.twitter ? GetTimeline(TimelineType.Home, Paging(maxId = Some(last - 1)))
    f collect {
      case GetTimelineResult(timelineType, timeline, _, _) =>
        edit { seq =>
          seq ++ timeline
        }
    } andThen {
      case _ =>
        synchronized {
          working = false
        }
    } match {
      case f: Future[TwitterStatusAdapter] =>
        Some(f)
    }
  }

  /**
   * 어댑터가 다른 작업을 하고 있지 않으면 `true`를 리턴한다.
   *
   * 이 메소드에서 `true`를 받으면 반드시 작업을 마치고 `working` 상태 변수를 false로 고쳐야 한다.
   * 이 메소드는 한번에 1개 스레드만 접근할 수 있다.
   *
   * @return 단독 작업이 가능하면 true, 아니면 false를 리턴한다.
   */
  def workExclusively(): Boolean = synchronized {
    if (working)
      return false
    working = true
    true
  }
}
