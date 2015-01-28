package aakulya
package cache

import scala.concurrent.duration._

class CacheSettings(
  val duration: FiniteDuration,
  val autoUpdate: Boolean)

object CacheSettings {
  def apply(duration: FiniteDuration = 5 minutes, autoUpdate: Boolean = false): CacheSettings = {
    new CacheSettings(duration, autoUpdate)
  }
}