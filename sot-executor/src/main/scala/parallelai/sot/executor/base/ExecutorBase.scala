package parallelai.sot.executor.base

import scala.collection.mutable.{Map => MutableMap}
import scala.util.Try
import scala.language.implicitConversions
import scala.language.existentials


/**
  * Created by crajah on 02/06/2017.
  */
class ExecutorBase {

}

trait Configurable {
  private val config: MutableMap[String, String] = MutableMap()

  def setConfig(k: String, v: String): Unit = config += (k -> v)

  def setConfig(m: Map[String, String]): Unit = config ++= m

  def setConfig(m: MutableMap[String, String]): Unit = config ++= m

  def getConfig: Map[String, String] = config.toMap
}
