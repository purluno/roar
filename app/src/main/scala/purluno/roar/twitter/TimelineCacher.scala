package purluno.roar.twitter

import akka.actor.{Actor, Props}
import purluno.roar.twitter.Twitter.{GetTimeline, GetTimelineResult}

import scala.collection.SortedSet

object TimelineCacher {

  trait Request

  trait Result

  /**
   * `TimelineCacher`의 응답 메시지로서 캐시에 원하는 데이터가 없는 경우 부모 액터(`Twitter`)로 보낸다.
   *
   * @param request 원래 요청
   */
  case class LoadFailure(request: GetTimeline) extends Result

  /**
   * `TimelineCacher`로의 요청 메시지로서 응답 받은 timeline을 캐시로 저장하게 한다.
   *
   * 효율적인 캐싱을 위해 타임라인 요청 정보도 받아서 의도했던 데이터 범위를 파악한다.
   *
   * @param request 타임라인 요청 메시지
   * @param result 타임라인 응답 메시지
   */
  case class Store(request: GetTimeline, result: GetTimelineResult) extends Request

  def props(slices: SortedSet[TimelineSlice] = SortedSet.empty): Props =
    Props(new TimelineCacher(slices))
}

/**
 * 타임라인 캐시 액터.
 *
 * 다음 메시지를 받아 처리한다.
 *
 * - GetTimeline 계열
 * - G
 *
 * @param slices
 */
class TimelineCacher(var slices: SortedSet[TimelineSlice] = SortedSet.empty) extends Actor {

  import purluno.roar.twitter.TimelineCacher._

  def receive = {
    case msg@GetTimeline(timelineType, paging, _) =>
      val sliceOption = paging.maxId match {
        case None =>
          slices.headOption
        case Some(maxId) =>
          slices.find(maxId >= _.bottomId)
      }
      val result = sliceOption match {
        case None =>
          IndexedSeq.empty
        case Some(slice) =>
          slice.getTimeline(paging)
      }
      if (result.isEmpty) {
        context.parent forward LoadFailure(msg)
      } else {
        sender() ! GetTimelineResult(timelineType, result, None, true)
      }

    case Store(request, result) =>
      val slice = TimelineSlice(request, result)
      val conts = slices.dropWhile(_.isNotContinuous(slice)).takeWhile(_.isContinuous(slice))
      val merged = conts.fold(slice) {
        _ ++ _
      }
      slices = slices -- conts + merged
  }
}
