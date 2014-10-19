package org.aakulya.cache

import scala.concurrent.duration._

object Element {
  def apply[V](instance: V, duration: FiniteDuration = (5 minutes)) = {
    new Element[V](instance, System.currentTimeMillis(), duration)
  }
}

class Element[V](val instance: V, val created: Long, val duration: FiniteDuration){
  private var _accessed = created
  private var _accessCount = 0
  
  def accessed = _accessed
  def accessCount = _accessCount
  
  private[cache] def doAccess: Element[V] = {
    _accessed = System.currentTimeMillis
    _accessCount = _accessCount + 1
    this
  }
  
  def isExpired = {
    created + duration.toMillis < System.currentTimeMillis()
  }
}

trait Storage[V] {
	final def getObject(key: String): Option[Element[V]] = {
	  val element = _getObject(key)
	  
	  element match{
	    case Some(element) => 
	     _putObject(key, element.doAccess)
	    case None => 
	      None
	  }
	}
	
	final def putObject(key: String, obj: Element[V]): Option[Element[V]] = { 
	  _putObject(key, obj)
	}
	
	protected def _getObject(key: String): Option[Element[V]] 
	protected def _putObject(key: String, obj: Element[V]):Option[Element[V]]
	
	def removeObject(key: String)
	def clear
	def size: Int
}