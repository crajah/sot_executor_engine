package parallelai.sot.executor.scio

import com.google.protobuf.Message
import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.values.SCollection
import com.trueaccord.scalapb.GeneratedMessage
import me.lyh.protobuf.generic.Schema
import parallelai.sot.executor.protobuf.PBReader
import parallelai.sot.executor.utils.AvroUtils

object PaiScioContext extends Serializable {

  implicit class PaiScioContext(sc: ScioContext) {
    def typedPubSubAvro[In <: HasAvroAnnotation : Manifest](project: String, topic: String): SCollection[In] = {
      val avroT = AvroType[In]
      val schema = avroT.schema
      val fromGenericRecord = avroT.fromGenericRecord
      val schemaString = schema.toString

      sc.pubsubTopic[Array[Byte]](s"projects/${project}/topics/${topic}", timestampAttribute = "timestamp_ms")
        .map(f => fromGenericRecord(AvroUtils.decodeAvro(f, schemaString)))
    }

    def typedPubSubProto[In <: GeneratedMessage with com.trueaccord.scalapb.Message[In] : Manifest](project: String, topic: String)(implicit messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[In]): SCollection[In] = {

      sc.pubsubTopic[Array[Byte]](s"projects/${project}/topics/${topic}", timestampAttribute = "timestamp_ms")
        .map(f => messageCompanion.parseFrom(f))
    }
  }

  implicit class PaiScioSCollectionAvro[Out <: HasAvroAnnotation : Manifest](c: SCollection[Out]) {
    def saveAsTypedPubSubAvro(project: String, topic: String): Unit = {
      val avroT = AvroType[Out]
      val schemaOut = avroT.schema
      val toGenericRecordOut = avroT.toGenericRecord
      val schemaStringOut = schemaOut.toString
      c.map(r => AvroUtils.encodeAvro(toGenericRecordOut(r), schemaStringOut)).saveAsPubsub(s"projects/${project}/topics/${topic}")
    }
  }

  implicit class PaiScioSCollectionProto[T0 <: GeneratedMessage with com.trueaccord.scalapb.Message[T0]](c: SCollection[T0]) {
      def saveAsTypedPubSubProto(project: String, topic: String)(implicit messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0]): Unit = {
        c.map(r => messageCompanion.toByteArray(r)).saveAsPubsub(s"projects/${project}/topics/${topic}")
      }
  }

}
