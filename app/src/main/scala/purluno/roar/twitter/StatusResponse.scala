package purluno.roar.twitter

import twitter4j.{RateLimitStatus, ResponseList, Status}

import scala.collection.JavaConverters._

object StatusResponse {
  def apply(responseList: ResponseList[Status]): StatusResponse =
    StatusResponse(responseList.asScala, Option(responseList.getRateLimitStatus))
}

case class StatusResponse(statuses: Seq[Status], rateLimitStatus: Option[RateLimitStatus])
