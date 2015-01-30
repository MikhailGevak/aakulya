package aakulya.cache

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.ActorSystem
import org.scalatest.{Matchers, BeforeAndAfterAll, BeforeAndAfter, FunSuiteLike}
import aakulya.storage.MapStorage

class MultiCacheActorTest extends TestKit(ActorSystem("cache")) with FunSuiteLike with ImplicitSender
  with Matchers with BeforeAndAfterAll with BeforeAndAfter{
 
  implicit val cacheSystem = system
  
  def testFunction1(s: String) = s"testFunction1($s)"
  def testFunction2(s: String) = s"testFunction2($s)"
  
  val cacheStorage = new MapStorage()
  
  val cachedFunction1: CachedFunction[String, String] = CacheBuilder(cacheStorage = cacheStorage) & {testFunction1(_)}
  val cachedFunction2: CachedFunction[String, String] = CacheBuilder(cacheStorage = cacheStorage) & {testFunction2(_)}
  
  test("Get 2 values from one storage for two functions using the same params (key)"){
    val value1 = cachedFunction1 >> "1"
    val value2 = cachedFunction2 >> "1"
    
    value1 should not be value2
    
  }
}