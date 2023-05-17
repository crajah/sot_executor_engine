package parallelai.sot.engine.io.utils

import java.{io => jio}

import parallelai.sot.engine.io.datastore.{DatastoreType, HasDatastoreAnnotation}

//TODO: Move to separate test project
object SerializableUtils {

  private def serializeToByteArray(value: Serializable): Array[Byte] = {
    val buffer = new jio.ByteArrayOutputStream()
    val oos = new jio.ObjectOutputStream(buffer)
    oos.writeObject(value)
    buffer.toByteArray
  }

  private def deserializeFromByteArray(encodedValue: Array[Byte]): AnyRef = {
    val ois = new jio.ObjectInputStream(new jio.ByteArrayInputStream(encodedValue))
    ois.readObject()
  }

  def ensureSerializable[T <: Serializable](value: T): T =
    deserializeFromByteArray(serializeToByteArray(value)).asInstanceOf[T]

}

object Test extends App {

  case class OutSchema(teamscores: String, score1: Int, score2: Int) extends HasDatastoreAnnotation
  def dataStoreT: DatastoreType[OutSchema] = DatastoreType[OutSchema]
  SerializableUtils.ensureSerializable(dataStoreT)

}
