package parallelai.sot.engine.io.datastore

class Kind(val value: String) extends AnyVal with Serializable

object Kind {
  def apply(value: String) = new Kind(value)
}