package aakulya
package cache

import scala.collection._

private[cache] class CacheStorage[Key, Value] extends mutable.HashMap[Key, Value]