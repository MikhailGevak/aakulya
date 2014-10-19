package org.aakulya.cache

import scala.collection._
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import scala.concurrent.Future
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.ActorLogging
import scala.concurrent.ExecutionContext
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class CacheSystem(val name: String) {
  private var initialized = false
  private val cacheWorkersMap = mutable.Map.empty[String, ActorRef]
  val actorSystem = ActorSystem("Cache" + name)

  initialize()
  ///////////////////////////////////////////////////////////////////////////////////
  def getCacheWorker(name: String) = cacheWorkersMap.get(name)

  def putCacheWorker[V](name: String,
    cacheFunction: WorkerParams => V,
    cacheSettings: CacheSettings = CacheSettings())(implicit storageCreator: () => Storage[V]) = {
   
    val actor = actorSystem.actorOf(Props(new CacheWorker[V](name, cacheFunction, cacheSettings)), name = name)
    cacheWorkersMap.put(name, actor)

    actor
  }

  def sendMessage(cacheName: String, params: WorkerParams)(implicit sender: ActorRef): Boolean = {
    getCacheWorker(cacheName) match {
      case Some(actor) =>
        actor ! GetValueRequest(params)
        true
      case None =>
        false
    }
  }

  def shutdown() = {
    actorSystem.shutdown
    cacheWorkersMap.clear
    initialized = false
  }

  def initialize() = {
    initialized = true
  }
}