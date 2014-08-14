package purluno.roar.twitter

import purluno.roar.twitter.Twitter.{GetTimeline, GetTimelineResult}
import twitter4j.Status

/**
 *
 */
object TimelineSlice {
  def apply(request: GetTimeline, result: GetTimelineResult): TimelineSlice = {
    val paging = request.paging
    TimelineSlice(IndexedSeq.empty ++ result.timeline, paging.maxId, paging.sinceId)
  }

  implicit val ord: Ordering[TimelineSlice] =
    Ordering.by((slice: TimelineSlice) => slice.topId).reverse
}

case class TimelineSlice(timeline: IndexedSeq[Status], maxId: Option[Long] = None,
                         sinceId: Option[Long] = None) {
  val topId: Long = maxId getOrElse timeline.head.getId

  val bottomId: Long = timeline.last.getId

  def ++(that: TimelineSlice): TimelineSlice = {
    val (top, bottom) =
      if (topId >= that.topId) (this, that)
      else (that, this)
    if (top.sinceOrBottomId > bottom.topId + 1) {
      throw new Exception(s"Cannot merge two slices: this($this) " +
        s"and that($that)")
    } else {
      val slicedBottomTimeline = bottom.timeline.dropWhile(_.getId >= top.bottomId)
      TimelineSlice(top.timeline ++ slicedBottomTimeline, top.maxId, bottom.sinceId)
    }
  }

  def isContinuous(that: TimelineSlice): Boolean = !isNotContinuous(that)

  def isNotContinuous(that: TimelineSlice): Boolean = {
    (this.topId > that.topId && this.sinceOrBottomId > that.topId + 1) ||
      (that.topId > this.topId && that.sinceOrBottomId > this.topId + 1)
  }

  def getTimeline(paging: Paging): IndexedSeq[Status] = {
    val p = paging.default
    val filtered = p match {
      case Paging(_, _, None, None) =>
        timeline
      case Paging(_, _, None, Some(maxId)) =>
        timeline.dropWhile(_.getId > maxId)
      case Paging(_, _, Some(sinceId), None) =>
        timeline.takeWhile(_.getId > sinceId)
      case Paging(_, _, Some(sinceId), Some(maxId)) =>
        timeline.dropWhile(_.getId > maxId).takeWhile(_.getId > sinceId)
    }
    p match {
      case Paging(Some(page), Some(count), _, _) =>
        filtered.drop((page - 1) * count).take(count)
    }
  }

  override def toString: String = {
    s"Slice($toRangeString/$size)"
  }

  def toRangeString: String = {
    sinceId match {
      case Some(sinceId) =>
        s"[$topId, $sinceId)"
      case None =>
        s"[$topId, $bottomId]"
    }
  }

  def sinceOrBottomId: Long = sinceId getOrElse bottomId

  def size: Int = timeline.size
}
