package purluno.roar.twitter

import akka.actor.{Actor, ActorRef}
import purluno.roar.twitter.Twitter.GetTimeline

class TimelineCachers extends Actor {
  var map: Map[TimelineType, ActorRef] = Map.empty

  def getCacher(timelineType: TimelineType): ActorRef = {
    if (!map.isDefinedAt(timelineType)) {
      val cacher = context.actorOf(TimelineCacher.props(), timelineType.toString)
      map += timelineType -> cacher
    }
    map(timelineType)
  }

  def receive = {
    case msg@GetTimeline(timelineType, _, _) =>
      getCacher(timelineType).tell(msg, sender())

    case msg@TimelineCacher.Store(GetTimeline(timelineType, _, _), _) =>
      getCacher(timelineType) ! msg

    case msg: TimelineCacher.LoadFailure =>
      context.parent forward msg
  }
}
