package parallelai.sot.engine.io.bigquery

trait BaseMappableTypeBQ[M] extends Serializable {
  def base: M
  def get(m: M, key: String): Option[M]
  def getAll(m: M, key: String): Seq[M]
  def put(key: String, value: M, tail: M): M
  def put(key: String, value: Option[M], tail: M): M
  def put(key: String, values: Seq[M], tail: M): M
}

trait MappableTypeBQ[M, V] extends Serializable {
  def get(m: M, key: String): Option[V]
  def getAll(m: M, key: String): Seq[V]
  def put(key: String, value: V, tail: M): M
  def put(key: String, value: Option[V], tail: M): M
  def put(key: String, values: Seq[V], tail: M): M
}

trait CanNest[M] extends Serializable