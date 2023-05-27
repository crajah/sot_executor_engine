package parallelai.sot.engine.io.datastore

import parallelai.sot.engine.config.SystemConfig

class Kind(val value: String) extends AnyVal with Serializable

object Kind {
  def apply(value: String): Kind = new Kind(value)

  def apply(): Kind = SystemConfig[Kind]("datastore.kind")
}