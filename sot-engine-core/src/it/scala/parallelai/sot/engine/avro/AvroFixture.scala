package parallelai.sot.engine.avro

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import org.apache.avro.Schema
import org.scalatest.Suite
import com.sksamuel.avro4s._

trait AvroFixture {
  this: Suite =>

  def serialize[T: SchemaFor : ToRecord](t: T): Array[Byte] = {
    implicit val schema: Schema = AvroSchema[T]

    val baos = new ByteArrayOutputStream
    val out = AvroOutputStream.binary[T](baos)

    out.write(t)
    out.close

    baos.toByteArray
  }

  def deserialize[T: SchemaFor : FromRecord](bytes: Array[Byte]): Iterator[T] = {
    val in = new ByteArrayInputStream(bytes)
    val input = AvroInputStream.binary[T](in)

    input.iterator
  }
}