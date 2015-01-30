package aakulya
package cache

import scala.concurrent.duration._

class CacheSettings(
  val duration: FiniteDuration,
  val isAutoUpdate: Boolean)

object CacheSettings {
  val DEFAULT_DURATION = 5 minutes
  val DEFAULT_UPDATE = false
  
  def apply(duration: FiniteDuration = DEFAULT_DURATION, autoUpdate: Boolean = DEFAULT_UPDATE): CacheSettings = {
    new CacheSettings(duration, autoUpdate)
  }
}