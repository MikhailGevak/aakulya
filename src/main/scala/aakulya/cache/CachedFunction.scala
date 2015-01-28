package aakulya
package cache

import scala.concurrent.duration._

abstract class CachedFunction[In, Out](implicit cacheSystem: CacheSystem) {

  def function: In => Out
  def keyFunction: In => String
  def cacheSettings: CacheSettings

  private[this] def fromCache(params: In): Either[String, Out] = {
    val key = keyFunction(params)
    println(s"Trying to get from cache by key: $key...")
    val value: Option[Out] = cacheSystem.get(keyFunction(params))

    value match {
      case Some(value) =>
        println(s"Value: $value")
        Right(value)
      case None =>
        println("Value: None")
        Left(key)
    }
    
  }

  final def calculate(params: In): Out = {
    fromCache(params) match {
      case Right(value) => value
      case Left(key) =>
        val value = function(params)
        cacheSystem.put(key, value, cacheSettings)
        value
    }
  }

  final def >>(params: In) = calculate(params)
}

private[this] class SimpleCachedFunction[In, Out](val function: In => Out, val cacheSettings: CacheSettings)(implicit cacheSystem: CacheSystem) extends CachedFunction[In, Out] {
  def keyFunction: In => String = { params => function.getClass.getName + "@" + params.toString }
}

object CachedFunction {
  def apply[In, Out](
    function: In => Out,
    cacheSettings: CacheSettings = CacheSettings())(implicit cacheSystem: CacheSystem): CachedFunction[In, Out] = {
    new SimpleCachedFunction[In, Out](function, cacheSettings)
  }
}

class CacheBuilder(val cacheSettings: CacheSettings){
   def &[In, Out](function: In => Out)(implicit cacheSystem: CacheSystem): CachedFunction[In, Out] = CachedFunction(function, cacheSettings)
}

object CacheBuilder{
  def apply(cacheSettings: CacheSettings)(implicit cacheSystem: CacheSystem): CacheBuilder = {
    new CacheBuilder(cacheSettings)
  } 
  
  def apply(duration: FiniteDuration = 5 minutes, autoUpdate: Boolean = false)(implicit cacheSystem: CacheSystem): CacheBuilder = {
    apply(CacheSettings(duration, autoUpdate))
  }
}
