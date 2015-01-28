package aakulya.cache

import akka.testkit.TestActorRef
import akka.testkit.{ TestActors, DefaultTimeout, ImplicitSender, TestKit }
import akka.actor.{ ActorSystem, ActorRef }
import org.scalatest.FunSuiteLike
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.{ Future, ExecutionContext }
import org.scalatest.Matchers._
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.actor.Props
import java.util.Calendar

class CacheActorTest extends TestKit(ActorSystem()) with FunSuiteLike with ImplicitSender
  with Matchers with BeforeAndAfterAll {
  implicit val cacheSystem = CacheSystem("cache")

  def currentTime = Calendar.getInstance.getTimeInMillis
  def testFunction(key: String) = currentTime

  test("get value which is not in the cache") {
    val cachedFunction = CachedFunction { key: String => testFunction(key) }
    val current = currentTime
    (cachedFunction >> "1") should be >= current
  }

  test("get value from two times. Second value have to be cached") {
    val cachedFunction = CachedFunction { key: String => testFunction(key) }
    val value_1 = cachedFunction >> "2"
    Thread.sleep(2000)
    val value_2 = cachedFunction >> "2"

    value_1 shouldEqual value_2
  }

  test("Value should be remove from cache after 5 seconds (cache duration=4 seconds)") {
    val cachedFunction = CacheBuilder(4 seconds) & { key: String => testFunction(key) }

    val value_1 = cachedFunction >> "3"
    Thread.sleep(2000)
    val value_2 = cachedFunction >> "3"
    Thread.sleep(3000)
    val value_3 = cachedFunction >> "3"

    value_1 shouldEqual value_2
    value_3 should be > value_2
  }

  override def afterAll {
    TestKit.shutdownActorSystem(cacheSystem.actorSystem)
  }
}