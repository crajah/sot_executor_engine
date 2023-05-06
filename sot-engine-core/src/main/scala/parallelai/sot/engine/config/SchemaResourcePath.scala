package parallelai.sot.engine.config

class SchemaResourcePath(val value: String) extends AnyVal

object SchemaResourcePath {
  def apply(): SchemaResourcePath = SystemConfig[SchemaResourcePath]("json.file.name")
}