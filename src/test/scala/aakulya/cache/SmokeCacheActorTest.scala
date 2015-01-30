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
import org.scalatest.BeforeAndAfter

class SmokeCacheActorTest extends TestKit(ActorSystem("cache")) with FunSuiteLike with ImplicitSender
  with Matchers with BeforeAndAfterAll with BeforeAndAfter {
  implicit val cacheSystem = system

  var cachedFunction: CachedFunction[String, Long] = _
  def currentTime = Calendar.getInstance.getTimeInMillis
  def testFunction(key: String) = currentTime

  test("get value which is not in the cache") {
    cachedFunction = CachedFunction { key: String => testFunction(key) }
    val current = currentTime
    (cachedFunction >> "1").get should be >= current
  }

  test("get value from two times. Second value have to be cached") {
    cachedFunction = CachedFunction { key: String => testFunction(key) }
    val value_1 = cachedFunction >> "1"
    Thread.sleep(2000)
    val value_2 = cachedFunction >> "1"

    value_1 shouldEqual value_2
  }

  test("Value should be remove from cache after 5 seconds (cache duration=4 seconds)") {
    cachedFunction = CacheBuilder(4 seconds) & { key: String => testFunction(key) }

    val value_1 = cachedFunction >> "1"
    Thread.sleep(2000)
    val value_2 = cachedFunction.getByParams("1")
    Thread.sleep(3000)
    val value_3 = cachedFunction.getByParams("1")

    value_1 shouldEqual value_2
    value_3 shouldBe 'empty
  }

  test("Value should be updated in cache after 5 seconds (cache duration=4 seconds)") {
    cachedFunction = CacheBuilder(4 seconds, true) & { key: String => testFunction(key) }

    val value_1 = cachedFunction >> "1"
    Thread.sleep(2000)
    val value_2 =  cachedFunction.getByParams("1")
    Thread.sleep(3000)
    val value_3 = cachedFunction.getByParams("1")

    value_1 shouldEqual value_2
    value_3.get should be > value_2.get
  }
  
   test("Value should be updated in cache after 5 seconds 2 times(cache duration=4 seconds)") {
    cachedFunction = CacheBuilder(4 seconds, true) & { key: String => testFunction(key) }

    val value_1 = cachedFunction >> "1"
    Thread.sleep(2000)
    val value_2 =  cachedFunction.getByParams("1")
    Thread.sleep(3000)
    val value_3 = cachedFunction.getByParams("1")
    Thread.sleep(4000)
    val value_4 = cachedFunction.getByParams("1")

    value_1 shouldEqual value_2
    value_3.get should be > value_2.get
    value_4.get should be > value_3.get
  }

  override def afterAll {
    TestKit.shutdownActorSystem(cacheSystem)
  }

  after {
    cachedFunction.clear
  }
}