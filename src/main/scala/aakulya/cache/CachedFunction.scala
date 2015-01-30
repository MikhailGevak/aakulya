package aakulya
package cache

import scala.concurrent.duration._
import akka.actor.{ Props, ActorSystem }
import akka.pattern.ask
import akka.util.Timeout
import CacheActor._
import scala.util.{ Success, Failure }
import scala.concurrent.Await
import akka.actor.Cancellable
import scala.collection._
import aakulya.storage.MapStorage
import java.util.concurrent.{ ArrayBlockingQueue, ThreadPoolExecutor, TimeUnit }
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

abstract class CachedFunction[In, Out](cacheStorage: CacheStorage)(implicit cacheSystem: ActorSystem) {
  protected def function: In => Out
  protected def keyFunction: In => String
  protected def cacheSettings: CacheSettings

  lazy val functionId = function.getClass.getName
  
  private[this] var updaters: mutable.Map[String, Cancellable] = mutable.Map()
  private[this] val cacheHandler = cacheSystem.actorOf(CacheActor.props[Out](cacheStorage, functionId))

  private[this] def fromCache(params: In): Either[String, Out] = {
    val key = keyFunction(params)

    val value: Option[Out] = get(key)

    value match {
      case Some(value) =>
        Right(value)
      case None =>
        Left(key)
    }
  }

  final def calculate(params: In): Option[Out] = {
    fromCache(params) match {
      case Right(value) => Some(value)
      case Left(key) =>
        calculateAndPut(key, params)
    }
  }

  final def >>(params: In) = calculate(params)
  ////Updaters//////////////////////////////////////////////////////////////////////////
  private[this] def scheduleUpdater(duration: FiniteDuration, key: String, params: In) {
    import AutoUpdateContext._
    
    val updater = cacheSystem.scheduler.schedule(duration, duration, cacheHandler, PutValue(key, createDoFunction(params), false))
    updaters.put(key, updater)
  }

  private[this] def scheduleRemover(duration: FiniteDuration, key: String) {
    import RemoveExpiredContext._

    cacheSystem.scheduler.scheduleOnce(duration, cacheHandler, RemoveValue(key))
  }

  ////Working with Cache Actor//////////////////////////////////////////////////////////
  private[this] def get(key: String): Option[Out] = {
    implicit val timeout: Timeout = 1 second
    val future = cacheHandler ? GetValue(key)

    val value = Await.result(future, timeout.duration)

    value match {
      case None => None
      case Some(value) => Some(value.asInstanceOf[Out])
    }
  }

  def getByParams(params: In): Option[Out] = {
    get(keyFunction(params))
  }

  def clear = cacheHandler ! ClearCache()

  def calculateAndPut(key: String, params: In): Option[Out] = {
    implicit val timeout: Timeout = 1 second

    val future = cacheHandler ? PutValue(key, createDoFunction(params))
    val value = Await.result(future, timeout.duration)

    cacheSettings.isAutoUpdate match {
      case true if (!updaters.contains(key)) =>

        scheduleUpdater(cacheSettings.duration, key, params)
      case false =>

        scheduleRemover(cacheSettings.duration, key)
    }

    value match {
      case None => None
      case Some(value) => Some(value.asInstanceOf[Out])
    }
  }

  def calculateAndPut(params: In): Option[Out] = {
    calculateAndPut(keyFunction(params), params)
  }

  def remove(key: String) = {
    updaters.remove(key)
    cacheHandler ! RemoveValue(key)
  }

  private[this] def createDoFunction(params: In): (Unit => Out) = {
    def doFunction(unit: Unit): Out = function(params)
    doFunction
  }
}

private[this] class SimpleCachedFunction[In, Out](val function: In => Out, val cacheSettings: CacheSettings, val cacheStorage: CacheStorage)(implicit cacheSystem: ActorSystem) extends CachedFunction[In, Out](cacheStorage) {
  def keyFunction: In => String = { params => params.toString }
}

object CachedFunction {
  def apply[In, Out](
    function: In => Out,
    cacheSettings: CacheSettings = CacheSettings(),
    cacheStorage: CacheStorage = MapStorage())(implicit cacheSystem: ActorSystem): CachedFunction[In, Out] = {
    new SimpleCachedFunction[In, Out](function, cacheSettings, cacheStorage)
  }
}

class CacheBuilder(val cacheSettings: CacheSettings, val cacheStorage: CacheStorage) {
  def &[In, Out](function: In => Out)(implicit cacheSystem: ActorSystem): CachedFunction[In, Out] = CachedFunction(function, cacheSettings, cacheStorage)
}

object CacheBuilder {
  def apply(cacheSettings: CacheSettings, cacheStorage: CacheStorage)(implicit cacheSystem: ActorSystem): CacheBuilder = {
    new CacheBuilder(cacheSettings, cacheStorage)
  }

  def apply(duration: FiniteDuration = 5 minutes, autoUpdate: Boolean = false, cacheStorage: CacheStorage = MapStorage())(implicit cacheSystem: ActorSystem): CacheBuilder = {
    apply(CacheSettings(duration, autoUpdate), cacheStorage)
  }
}

private object AutoUpdateContext {
  /*val FUTURE_POOL_SIZE = 5
  val FUTURE_QUEUE_SIZE = 20000

  private lazy val ucfdThreadPoolExecutor =
    new ThreadPoolExecutor(FUTURE_POOL_SIZE, FUTURE_POOL_SIZE,
      1, TimeUnit.MINUTES,
      new ArrayBlockingQueue(FUTURE_QUEUE_SIZE, true))
*/
  implicit lazy val ucfdExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool)
}

private object RemoveExpiredContext {
  /*val FUTURE_POOL_SIZE = 5
  val FUTURE_QUEUE_SIZE = 20000

  private lazy val expThreadPoolExecutor =
    new ThreadPoolExecutor(FUTURE_POOL_SIZE, FUTURE_POOL_SIZE,
      1, TimeUnit.MINUTES,
      new ArrayBlockingQueue(FUTURE_QUEUE_SIZE, true))*/

  implicit lazy val expExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool)
}