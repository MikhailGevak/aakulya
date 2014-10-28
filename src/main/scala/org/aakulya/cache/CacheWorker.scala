package org.aakulya.cache

import akka.actor.ActorLogging
import akka.actor.Actor
import scala.concurrent.ExecutionContext

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.ThreadPoolExecutor

trait WorkerParams {
  def cacheKey: String
}
case class GetValueRequest(params: WorkerParams)
case class GetValueResponse(params: WorkerParams, value: Any)

private[cache] case class GetCacheRequest()
private[cache] case class GetCacheResponse(cache: Any)

private[cache] case class RemoveExpired(key: String)
private[cache] case class AutoUpdate(params: WorkerParams)

class CacheWorker[V](val name: String, cacheFunction: WorkerParams => V, cacheSettings: CacheSettings = CacheSettings())(implicit storageCreator: () => Storage[V]) extends Actor with ActorLogging {
  private val cache = new Cache[V](name, storageCreator, cacheSettings)

  def receive = {
    case message @ GetValueRequest(params) =>
      println(s"CacheWorker receive message: $message -> $self")
      sender ! GetValueResponse(params, getValue(params))
    case message @ GetCacheRequest() =>
      println(s"CacheWorker receive message: $message -> $self")
      sender ! GetCacheResponse(cache)
    case message @ RemoveExpired(key) =>
      println(s"CacheWorker receive message: $message -> $self")
      cache.remove(key)
    case message @ AutoUpdate(params) =>
      println(s"CacheWorker receive message: $message -> $self")
      updateValue(params)
  }

  override def unhandled(message: Any) = {
    println(s"CacheWorker receive unhandled message: $message -> $self")
    super.unhandled(message)
  }

  def updateValue(params: WorkerParams): V = {
    val key = params.cacheKey
    val value = doWork(params)
    println(s"Put value to cache ($key -> $value)")
    cache.put(key, value)
    value
  }

  def getValue(params: WorkerParams): V = {
    val key = params.cacheKey
    val value = cache.get(key) match {
      case Some(value) =>
        println(s"Get value from cache ($key -> $value)")
        value
      case None =>
        val value = updateValue(params)

        if (cache.isAutoUpdated) {
          addAutoUpdatedTask(params) //schedule Auto Update task 
        } else {
          addRemoveExpiredTask(key) //schedule Remove Expired task
        }
        value
    }
    
     println("Storage: " + cache.storage)
     value
  }

  def addAutoUpdatedTask(params: WorkerParams) {
    import AutoUpdateContext._
    context.system.scheduler.scheduleOnce(cache.duration, self, AutoUpdate(params))
  }
  def addRemoveExpiredTask(key: String) {
    import RemoveExpiredContext._
    context.system.scheduler.scheduleOnce(cache.duration, self, RemoveExpired(key))
  }

  def doWork(params: WorkerParams): V = {
    cacheFunction(params)
  }
}

object AutoUpdateContext {
  val FUTURE_POOL_SIZE = 5
  val FUTURE_QUEUE_SIZE = 20000

  private lazy val ucfdThreadPoolExecutor =
    new ThreadPoolExecutor(FUTURE_POOL_SIZE, FUTURE_POOL_SIZE,
      1, TimeUnit.MINUTES,
      new ArrayBlockingQueue(FUTURE_QUEUE_SIZE, true))

  implicit lazy val ucfdExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(ucfdThreadPoolExecutor)
}

private object RemoveExpiredContext {
  val FUTURE_POOL_SIZE = 5
  val FUTURE_QUEUE_SIZE = 20000

  private lazy val expThreadPoolExecutor =
    new ThreadPoolExecutor(FUTURE_POOL_SIZE, FUTURE_POOL_SIZE,
      1, TimeUnit.MINUTES,
      new ArrayBlockingQueue(FUTURE_QUEUE_SIZE, true))

  implicit lazy val expExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(expThreadPoolExecutor)
}