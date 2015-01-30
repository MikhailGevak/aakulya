package aakulya
package cache

import scala.collection._

trait CacheStorage{
  def clear
  def remove(key: (String, String))
  def put[A](key: (String, String), value: A)
  def get[A](key: (String, String)): A
}