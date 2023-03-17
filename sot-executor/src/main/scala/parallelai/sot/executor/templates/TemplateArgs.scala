package parallelai.sot.executor.templates

sealed abstract class TemplateArgs

case class PubSubArgs(topic: String) extends TemplateArgs
case class BigQueryArgs(dataset: String, table: String) extends TemplateArgs
case class BigTableArgs(instanceId: String, tableId: String, familyName: List[String], numNodes: Int) extends TemplateArgs
case class DatastoreArgs(kind: String) extends TemplateArgs
