package parallelai.sot.executor.scio

import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.values.SCollection
import parallelai.sot.executor.utils.AvroUtils

object PaiScioContext {

  implicit class PaiScioContext(sc: ScioContext) {
    def typedPubSub[In <: HasAvroAnnotation : Manifest](project: String, topic: String): SCollection[In] = {
      val avroT = AvroType[In]
      avroT.fromGenericRecord
      val schema = avroT.schema
      val fromGenericRecord = avroT.fromGenericRecord
      val schemaString = schema.toString

      sc.pubsubTopic[Array[Byte]](s"projects/${project}/topics/${topic}", timestampAttribute = "timestamp_ms")
        .map(f => fromGenericRecord(AvroUtils.decodeAvro(f, schemaString)))
    }
  }

  implicit class PaiScioSCollection[Out <: HasAvroAnnotation : Manifest](c: SCollection[Out]) {
    def saveAsTypedPubSub(project: String, topic: String): Unit = {
      val avroT = AvroType[Out]
      val schemaOut = avroT.schema
      val toGenericRecordOut = avroT.toGenericRecord
      val schemaStringOut = schemaOut.toString
      c.map(r => AvroUtils.encodeAvro(toGenericRecordOut(r), schemaStringOut)).saveAsPubsub(s"projects/${project}/topics/${topic}")
    }
  }

}