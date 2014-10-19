package org.aakulya.cache

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.postfixOps
object CacheSettings {
  val DEFAULT_INITIAL_CAPACITY = 128
  val DEFAULT_MAX_CAPACITY = 1024
  val DEFAULT_DURATION = 5 minutes

  def apply() = new CacheSettings
}

class CacheSettings {
  private var _duration: FiniteDuration = 5 minutes
  private var _initialCapacity: Int = 128
  private var _maxCapacity: Int = 1024
  private var _autoUpdated: Boolean = false

  def duration() = {
    _duration
  }

  def initialCapacity() = {
    _initialCapacity
  }

  def maxCapacity() = {
    _maxCapacity
  }

  def isAutoUpdated() = {
    _autoUpdated
  }

  def setDuration(newDuration: FiniteDuration) = {
    _duration = newDuration
    this
  }

  def setInitialCapacity(newInitialCapacity: Int) = {
    _initialCapacity = newInitialCapacity
    this
  }

  def setMaxCapacity(newMaxCapacity: Int) = {
    _maxCapacity = newMaxCapacity
    this
  }

  def setAutoUpdated(newAutoUpdated: Boolean) = {
    _autoUpdated = newAutoUpdated
    this
  }
}

class Cache[V](val cacheName: String, storageCreator: () => Storage[V], val initialCapacity: Int, val maxCapacity: Int, val duration: FiniteDuration, val isAutoUpdated: Boolean) {
  import org.aakulya.cache.CacheSettings;
  import org.aakulya.cache.Element._

  val storage = storageCreator()

  def this(cacheName: String, storageCreator: () => Storage[V], settings: CacheSettings) = this(cacheName,
    storageCreator,
    math.min(settings.initialCapacity, settings.maxCapacity),
    settings.maxCapacity,
    settings.duration, settings.isAutoUpdated)

  def get(key: String): Option[V] = {
    storage.getObject(key) match {
      case None =>
        None
      case Some(element) if isExpired(element) =>
        None
      case Some(element) =>
        Some(element.instance)
    }
  }

  def put(key: String, value: V): Unit = {
    storage.putObject(key, Element(value))
  }

  def remove(key: String): Unit = {
    storage.removeObject(key)
  }

  def size = storage.size

  def clear = storage.clear

  private def isExpired(element: Element[V]) = {
    element.isExpired
  }
}
