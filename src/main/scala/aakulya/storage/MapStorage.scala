package aakulya
package storage

import aakulya.cache.CacheStorage
import scala.collection._

class MapStorage extends CacheStorage{
  private[this] val mapStorage = mutable.Map[(String, String), Any]()
  
  def clear = mapStorage.clear
  
  def remove(key: (String, String)){
    mapStorage.remove(key)
  }
  
  def put[A](key: (String, String), value: A){
	 mapStorage.put(key, value)
  }
  
  def get[A](key: (String, String)): A = {
    mapStorage.get(key).asInstanceOf[A]
  }
}

object MapStorage{
  def apply() = new MapStorage()
}