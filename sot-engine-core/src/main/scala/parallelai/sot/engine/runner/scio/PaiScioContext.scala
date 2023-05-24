package parallelai.sot.engine.runner.scio

import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.values.SCollection
import com.trueaccord.scalapb.GeneratedMessage
import parallelai.sot.engine.serialization.avro.AvroUtils
import com.google.datastore.v1.client.DatastoreHelper.makeKey
import parallelai.sot.engine.io.utils.annotations.{HasDatastoreAnnotation, HasJSONAnnotation}
import shapeless.{HList, LabelledGeneric}
import io.circe.generic.auto._
import io.circe.parser._
import org.slf4j.LoggerFactory

import parallelai.sot.engine.io.datastore._

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

    def typedPubSubJSON[In <: HasJSONAnnotation : Manifest](project: String, topic: String)(implicit ev: io.circe.Decoder[In]): SCollection[In] = {
      sc.pubsubTopic[String](s"projects/${project}/topics/${topic}", timestampAttribute = "timestamp_ms")
        .map { f =>
          decode[In](f) match {
            case Right(in) => in
            case Left(p) => throw p.fillInStackTrace()
          }
        }
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

  implicit class KVPaiScioSCollection[Out <: HasDatastoreAnnotation : Manifest, Key](c: SCollection[(Key, Out)]) {
    def saveAsTypedDatastore[L <: HList](project: String, kind: String)(implicit gen: LabelledGeneric.Aux[Out, L],
                                                                        toL: ToEntity[L]): Unit = {
      val dataStoreT: DatastoreType[Out] = DatastoreType[Out]

      c.map { case (key, rec) =>
        val entity = dataStoreT.toEntityBuilder(rec)
        val keyEntity = key match {
          case name: String => makeKey(kind, name.asInstanceOf[AnyRef])
          case id: Int => makeKey(kind, id.asInstanceOf[AnyRef])
        }
        entity.setKey(keyEntity)
        entity.build()
      }.saveAsDatastore(project)
    }
  }

}
