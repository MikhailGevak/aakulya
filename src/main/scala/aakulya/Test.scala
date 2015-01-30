package aakulya

import akka.actor.ActorSystem
import aakulya.cache.CachedFunction
import java.util.Calendar
import scala.concurrent.duration._
import aakulya.cache.CacheBuilder

object Test {
  def currentTime = Calendar.getInstance.getTimeInMillis
  def testFunction(key: String) = currentTime
  implicit val cacheSystem = ActorSystem("cache")
  val cachedFunction = CacheBuilder(4 seconds, true) & { key: String => testFunction(key) }

  def main(args: Array[String]): Unit = {
    val value_1 = cachedFunction >> "1"
    Thread.sleep(2000)
    val value_2 = cachedFunction.getByParams("1")
    Thread.sleep(3000)
    val value_3 = cachedFunction.getByParams("1")
  }
}