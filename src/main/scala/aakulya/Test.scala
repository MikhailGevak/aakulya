package aakulya

import akka.actor.ActorSystem
import aakulya.cache.CacheSystem
import aakulya.cache.CachedFunction

object Test {
   def main(args: Array[String]): Unit = {
    implicit val cacheSystem = CacheSystem("cache")
     
    val cached = CachedFunction{
      params: (Int, Int) =>
         params._1 + "@" + params._2
      }
    println(cached >> (1, 1))
    println(cached >> (1, 2))
    println(cached >> (2, 2))

    println(cached >> (1, 1))
    println(cached >> (1, 2))
    println(cached >> (2, 2))  }
}