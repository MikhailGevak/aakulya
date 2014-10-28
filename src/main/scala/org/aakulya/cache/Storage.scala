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
  private var nextElementKey: Option[String] = _
  private var prevElementKey: Option[String] = _
  
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
  
  def rate = created/_accessCount
  
  override def toString = s"(instance=$instance, accessCount=$accessCount)"
}

trait Storage[V] {
    final def getObject(key: String): Option[Element[V]] = {
	  val element = _getObject(key)
	  
	  element match{
	    case Some(element) => 
	     putObject(key, element.doAccess)
	    case None => 
	      None
	  }
	}
	
	final def putObject(key: String, obj: Element[V]): Option[Element[V]] = { 
	   if (size == 0){
	     setFirstElementKey(key)
	   }
	  _putObject(key, obj)
	}
	
	final def removeObject(key: String) = {
	  _removeObject(key)
	}
	
	protected def _getObject(key: String): Option[Element[V]] 
	protected def _putObject(key: String, obj: Element[V]):Option[Element[V]]
	protected def _removeObject(key: String)
	
	def clear
	def size: Int
	
	def setFirstElementKey(elementKey: String)
	def firstElementKey: Option[String]
	
	def firstElement: Option[Element[V]] = {
		firstElementKey match{
			case Some(key) => _getObject(key)
			case None => None
		}
	}
}