package parallelai.sot.engine

import parallelai.sot.engine.config.SystemConfig

class Project(val id: String) extends AnyVal with Serializable

object Project {
  def apply(id: String): Project = new Project(id)

  def apply(): Project = SystemConfig[Project]("project")
}