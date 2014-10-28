package org.aakulya.cache

import org.scalatest.Matchers
import akka.testkit.ImplicitSender
import org.scalatest.FunSuiteLike
import akka.testkit.TestKit
import akka.actor.ActorSystem
import scala.concurrent.duration._
import akka.util.Timeout
import scala.language.postfixOps
import scala.concurrent.Await
import org.aakulya.storage.StorageByMap

object TestFunction {
  case class TimeParams() extends WorkerParams {
    def cacheKey = "TestParameters"
  }

  def currentTime(params: WorkerParams): Long = {
    params match {
      case TimeParams() => System.currentTimeMillis()
    }
  }

  case class EchoParams(message: String) extends WorkerParams {
    def cacheKey = message
  }

  def echo(params: WorkerParams): String = {
    params match {
      case EchoParams(message) => message
    }
  }
}
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class CacheSystemTest extends TestKit(ActorSystem()) with FunSuiteLike with ImplicitSender with Matchers {
  import TestFunction._
  implicit val timeout = Timeout(60 seconds)
  val TEST_NAME = "testCache"

  test("value should be stored in cache system") {
    val cachedSystem = new CacheSystem("testCacheSystem")

    cachedSystem.putCacheWorker(TEST_NAME, TestFunction.currentTime(_))(() => new StorageByMap[Long])

    cachedSystem.sendMessage(TEST_NAME, TimeParams())

    val putValue = expectMsgPF() {
      case GetValueResponse(TimeParams(), x) => x
    }

    Thread.sleep(1000)

    cachedSystem.sendMessage(TEST_NAME, TimeParams())

    val getValue = expectMsgPF() {
      case GetValueResponse(TimeParams(), x) => x
    }

    cachedSystem.shutdown

    putValue should be(getValue)
  }

  test("value should have expired time") {
    val cachedSystem = new CacheSystem("testCacheSystem")

    cachedSystem.putCacheWorker(TEST_NAME, TestFunction.currentTime(_), CacheSettings().setDuration(2 seconds))(() => new StorageByMap[Long])

    cachedSystem.sendMessage(TEST_NAME, TimeParams())

    val value1 = expectMsgPF() {
      case GetValueResponse(TimeParams(), x: Long) => x
    }

    Thread.sleep(1000)

    cachedSystem.sendMessage(TEST_NAME, TimeParams())

    val value2 = expectMsgPF() {
      case GetValueResponse(TimeParams(), x: Long) => x
    }

    Thread.sleep(2000)
    cachedSystem.sendMessage(TEST_NAME, TimeParams())

    val value3 = expectMsgPF() {
      case GetValueResponse(TimeParams(), x: Long) => x
    }

    cachedSystem.shutdown

    println(value1)
    println(value2)
    println(value3)

    value1 should be(value2)
    value3 should be > value2
  }

  test("cache size should be less then max") {
    val cachedSystem = new CacheSystem("testCacheSystem")

    cachedSystem.putCacheWorker(TEST_NAME, TestFunction.echo(_), CacheSettings().setMaxCapacity(10))(() => new StorageByMap[String])

    var i = 0
    for (i <- 1 to 100) {
      cachedSystem.sendMessage(TEST_NAME, EchoParams(i.toString))
      expectMsgPF() { case x => println(x) }
    }

    Thread.sleep(2000)

    cachedSystem.getCacheWorker(TEST_NAME).get ! GetCacheRequest()

    val cache = expectMsgPF() {
      case GetCacheResponse(x: Cache[_]) => x
    }

    cache.size should be(10)
  }

  test("expired values should be removed from cache") {
    val cachedSystem = new CacheSystem("testCacheSystem")

    cachedSystem.putCacheWorker(TEST_NAME, TestFunction.currentTime(_), CacheSettings().setDuration(2 seconds))(() => new StorageByMap[Long])

    cachedSystem.sendMessage(TEST_NAME, TimeParams())
    expectMsgPF() { case x => println(x) }

    cachedSystem.getCacheWorker(TEST_NAME).get ! GetCacheRequest()
    val cacheSize1 = expectMsgPF() {
      case GetCacheResponse(x: Cache[_]) => x
    }.size

    Thread.sleep(2100)

    cachedSystem.getCacheWorker(TEST_NAME).get ! GetCacheRequest()
    val cacheSize2 = expectMsgPF() {
      case GetCacheResponse(x: Cache[_]) => x
    }.size

    println(cacheSize1)
    println(cacheSize2)

    cacheSize1 should be(1)
    cacheSize2 should be(0)
  }

  test("value should be auto updated") {
    val cachedSystem = new CacheSystem("testCacheSystem")

    cachedSystem.putCacheWorker(TEST_NAME, TestFunction.currentTime(_), CacheSettings().setDuration(2 seconds).setAutoUpdated(true))(() => new StorageByMap[Long])

    cachedSystem.sendMessage(TEST_NAME, TimeParams())

    val value1 = expectMsgPF() {
      case GetValueResponse(TimeParams(), x: Long) => x
    }

    Thread.sleep(1000)

    cachedSystem.getCacheWorker(TEST_NAME).get ! GetCacheRequest()

    val value2 = expectMsgPF() {
      case GetCacheResponse(x: Cache[_]) => x
    }.get(TimeParams().cacheKey).get.asInstanceOf[Long]

    Thread.sleep(2000)
    cachedSystem.getCacheWorker(TEST_NAME).get ! GetCacheRequest()

    val value3 = expectMsgPF() {
      case GetCacheResponse(x: Cache[_]) => x
    }.get(TimeParams().cacheKey).get.asInstanceOf[Long]

    cachedSystem.shutdown

    println(value1)
    println(value2)
    println(value3)

    value1 should be(value2)
    value3 should be > value2
  }

  test("element in cache should know how many time it has been touched") {
    val cachedSystem = new CacheSystem("testCacheSystem")

    cachedSystem.putCacheWorker(TEST_NAME, TestFunction.currentTime(_), CacheSettings().setDuration(2 seconds).setAutoUpdated(true))(() => new StorageByMap[Long])

    cachedSystem.sendMessage(TEST_NAME, TimeParams())

    val value1 = expectMsgPF() {
      case GetValueResponse(TimeParams(), x: Long) => x
    }

    cachedSystem.sendMessage(TEST_NAME, TimeParams())

    val value2 = expectMsgPF() {
      case GetValueResponse(TimeParams(), x: Long) => x
    }

    cachedSystem.getCacheWorker(TEST_NAME).get ! GetCacheRequest()

    val value3 = expectMsgPF() {
      case GetCacheResponse(x: Cache[_]) => x
    }.storage.getObject(TimeParams().cacheKey).get.accessCount

    Thread.sleep(2000)

    println(value1)
    println(value2)
    println(value3)
    
    value3 should be(2)
  }

}