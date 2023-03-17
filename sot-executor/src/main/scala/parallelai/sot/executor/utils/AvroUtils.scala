package parallelai.sot.executor.utils

import java.io.ByteArrayOutputStream

import com.google.common.base.Throwables
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io._
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}
import parallelai.sot.executor.example.Injector

import scala.util.{Failure, Success}

object AvroUtils {

  def decodeAvro(message: Array[Byte], schemaStr: String): GenericRecord = {
    val schema: Schema = new Parser().parse(schemaStr)
    // Deserialize and create generic record
    val reader: DatumReader[GenericRecord] = new SpecificDatumReader[GenericRecord](schema)
    val decoder: Decoder = DecoderFactory.get().binaryDecoder(message, null)
    val record: GenericRecord = reader.read(null, decoder)
    record
  }

  def encodeAvro(genericRecord: GenericRecord, schemaStr: String) : Array[Byte] = {
    val schema: Schema = new Parser().parse(schemaStr)
    val writer = new SpecificDatumWriter[GenericRecord](schema)
    val out = new ByteArrayOutputStream()
    val encoder: BinaryEncoder = EncoderFactory.get().binaryEncoder(out, null)
    writer.write(genericRecord, encoder)
    encoder.flush()
    out.close()
    val serializedBytes: Array[Byte] = out.toByteArray()
    serializedBytes
  }

}
