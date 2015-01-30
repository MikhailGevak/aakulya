package aakulya
package cache

import akka.actor.{Actor, Props}
import scala.concurrent.duration._

object CacheActor {
  //input messages
  case class ClearCache()
  case class RemoveValue(key: String)
  case class PutValue(key: String, function: Unit => Any, returnValue: Boolean = true)
  case class GetValue(key: String)
  
  def props[Value](cacheStorage: CacheStorage, functionId: String): Props = Props(classOf[CacheActor], cacheStorage, functionId)
}

private[cache] class CacheActor(cacheStorage: CacheStorage, functionId: String) extends Actor {
  import CacheActor._

  def receive = {
    case ClearCache() =>
      cacheStorage.clear
    case RemoveValue(key: String) =>
      cacheStorage.remove((functionId, key))
    case PutValue(key, function, returnValue) =>
      val value = function.apply()
      
      cacheStorage.put((functionId, key), value)
      if (returnValue) sender ! Option(value)
    case GetValue(key) =>
      sender ! cacheStorage.get((functionId, key))
    case other => println(s"Unhandled message: $other")
  }
}