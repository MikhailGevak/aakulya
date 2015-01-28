package aakulya
package cache

import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Success, Failure }
import scala.concurrent.Await

class CacheSystem(val actorSystem: ActorSystem) {
  import CacheActor._

  val cacheHandler = actorSystem.actorOf(Props[CacheActor])

 private[cache] def get[A](key: String): Option[A] = {
    implicit val timeout: Timeout = 1 second
    val future = cacheHandler ? GetValue(key)

    val value = Await.result(future, timeout.duration).asInstanceOf[CacheValue]

    value.value match {
      case Some(value) => Some(value.asInstanceOf[A])
      case None => None
    }
  }

 private[cache] def clear =  cacheHandler ! ClearCache
  
  private[cache] def put[A](key: String, value: A, settings: CacheSettings) {
    cacheHandler ! PutValue(key, value, settings)
  }
}

object CacheSystem {
  def apply(id: String):CacheSystem = {
    new CacheSystem(ActorSystem(id))
  }
}
