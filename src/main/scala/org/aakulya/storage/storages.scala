package org.aakulya.storage

import org.aakulya.cache.Element
import org.aakulya.cache.Storage

class StorageByMap[V] extends Storage[V]{
    val map: collection.mutable.Map[String, Element[V]] = collection.mutable.Map.empty[String, Element[V]]
    
    def _getObject(key: String): Option[Element[V]] = {
      map.get(key)
    }
    
	def _putObject(key: String, obj: Element[V]) = {
		map.put(key, obj)
	}
	
	def removeObject(key: String) = {
	  map.remove(key)
	}
	
	def clear = map.clear
	
	def getAll: Iterable[Element[V]] = map.values
	
    def size: Int = map.size
}