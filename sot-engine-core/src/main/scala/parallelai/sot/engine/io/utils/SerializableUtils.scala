package parallelai.sot.engine.io.utils

import java.{io => jio}

import java.util.function.{Function => JFunction, Predicate => JPredicate, BiFunction => JBiFunction, BiPredicate => JBiPredicate}

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