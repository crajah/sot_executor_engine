package parallelai.sot.engine

package object system {
  type Key = String
  type Value = String

  def withSystemProperties[R](keyValues: (Key, Value)*)(block: => R): R = {
    keyValues.foreach { case (key, value) => System.setProperty(key, value) }
    val result = block
    keyValues.foreach { case (key, _) => System.clearProperty(key) }
    result
  }
}