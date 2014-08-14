package purluno.roar.twitter

object Paging {
  def apply(paging: twitter4j.Paging): Paging = {
    val page = if (paging.getPage == -1) None else Some(paging.getPage)
    val count = if (paging.getCount == -1) None else Some(paging.getCount)
    val sinceId = if (paging.getSinceId == -1) None else Some(paging.getSinceId)
    val maxId = if (paging.getMaxId == -1) None else Some(paging.getMaxId)
    Paging(page, count, sinceId, maxId)
  }
}

case class Paging(page: Option[Int] = None, count: Option[Int] = None,
                  sinceId: Option[Long] = None, maxId: Option[Long] = None) {

  def asTwitter4j: twitter4j.Paging = {
    (page, sinceId) match {
      case (None, Some(s)) =>
        val paging = new twitter4j.Paging(s)
        for (c <- count) paging.setCount(c)
        for (id <- maxId) paging.setMaxId(id)
        paging
      case (Some(p), _) =>
        val paging = new twitter4j.Paging(p)
        for (c <- count) paging.setCount(c)
        for (id <- sinceId) paging.setSinceId(id)
        for (id <- maxId) paging.setMaxId(id)
        paging
      case (_, _) =>
        val paging = new twitter4j.Paging(1)
        for (c <- count) paging.setCount(c)
        for (id <- sinceId) paging.setSinceId(id)
        for (id <- maxId) paging.setMaxId(id)
        paging
    }
  }

  def default: Paging = {
    Paging(Some(page getOrElse 1), Some(count getOrElse 20), sinceId, maxId)
  }

  def isEmpty: Boolean = page.isEmpty && count.isEmpty && sinceId.isEmpty && maxId.isEmpty

}
