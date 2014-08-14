package purluno.roar.util

import scala.collection.mutable

object MutableLruMap {
  def empty[A, B](maxSize: Int): MutableLruMap[A, B] = new MutableLruMap(maxSize)
}

class MutableLruMap[A, B](maxSize: Int) extends mutable.AbstractMap[A, B] {
  private val map = new mutable.LinkedHashMap[A, B]

  override def +=(kv: (A, B)): this.type = {
    if (size + 1 > maxSize) {
      val keys = map.keysIterator.take(size - maxSize + 1)
      map --= keys
    }
    map += kv
    this
  }

  override def -=(key: A): this.type = {
    map -= key
    this
  }

  override def get(key: A): Option[B] = {
    map.get(key) match {
      case option@Some(value) =>
        map -= key
        map += key -> value
        option
      case None =>
        None
    }
  }

  override def iterator: Iterator[(A, B)] = map.iterator

  override def empty: mutable.Map[A, B] = new MutableLruMap(maxSize)

  override def foreach[U](f: ((A, B)) => U): Unit = map.foreach(f)

  override def size: Int = map.size
}
