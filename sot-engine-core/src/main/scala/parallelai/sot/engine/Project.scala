package parallelai.sot.engine

class Project(val id: String) extends AnyVal with Serializable

object Project {
  def apply(id: String) = new Project(id)
}