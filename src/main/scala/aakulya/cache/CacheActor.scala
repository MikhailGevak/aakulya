package aakulya.cache

import akka.actor.Actor
import scala.concurrent.duration._

object CacheActor {
  case class GetValue(key: Any)
  case class PutValue(key: Any, value: Any, settings: CacheSettings)
  case class CacheValue(value: Option[Any])
  case class ClearCache()
}

private[cache] class CacheActor extends Actor {
  import CacheActor._
  val cacheStorage = new CacheStorage[Any, Any]
  
  def receive = {
    case GetValue(key) =>
      sender ! CacheValue(cacheStorage.get(key))
    case PutValue(key, value, settings) =>
      cacheStorage.put(key, value)
      
  }
}