package com.spotify.scio.sot

import java.nio.charset.Charset

import com.google.datastore.v1.Entity
import com.google.datastore.v1.client.DatastoreHelper.makeKey
import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.kafka._
import com.spotify.scio.values.SCollection
import com.trueaccord.scalapb.GeneratedMessage
import io.circe.parser._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.shapes._
import parallelai.sot.engine.io.datastore.{DatastoreType, ToEntity}
import org.apache.beam.sdk.io.gcp.datastore.DatastoreIOSOT
import parallelai.sot.engine.config.gcp.SOTUtils
import parallelai.sot.engine.generic.row.Row
import parallelai.sot.engine.io.datastore._
import parallelai.sot.engine.io.utils.annotations.{HasDatastoreAnnotation, HasJSONAnnotation, Schemaless}
import parallelai.sot.engine.serialization.avro.AvroUtils
import parallelai.sot.executor.model.DedupeStrategy
import parallelai.sot.executor.model.SOTMacroConfig.{GoogleStoreTapDefinition, KafkaTapDefinition, PubSubTapDefinition}
import shapeless.{HList, LabelledGeneric}

object PaiScioContext extends Serializable {

  //replace io.circe.Decoder[List[T]] with io.circe.Decoder[Option[List[T]]]
  //adding the type to the definition results in java.lang.StackOverflowError
  implicit def listDecoder[T](implicit decoder: Decoder[T]) = io.circe.Decoder[Option[List[T]]].map(_.toList.flatten)

  def getSubscription(tap: PubSubTapDefinition, utils: SOTUtils) =
    s"projects/${utils.getProject}/subscriptions/${utils.getJobName}-${tap.topic}-${tap.id}-managed"

  implicit class PaiScioContext(sc: ScioContext) {
    def typedPubSubAvro[In <: HasAvroAnnotation : Manifest](tap: PubSubTapDefinition, utils: SOTUtils): SCollection[In] = {
      val avroT = AvroType[In]
      val schema = avroT.schema
      val fromGenericRecord = avroT.fromGenericRecord
      val schemaString = schema.toString

      if (tap.managedSubscription.isDefined && tap.managedSubscription.get) {
        utils.setPubsubTopic(s"projects/${utils.getProject}/topics/${tap.topic}")
        val subscriptionName = getSubscription(tap, utils)
        utils.setPubsubSubscription(subscriptionName)
        utils.setupPubsubSubscription()
        sc.pubsubSubscription[Array[Byte]](utils.getPubsubSubscription, timestampAttribute = tap.timestampAttribute.orNull, idAttribute = tap.idAttribute.orNull)
          .map(f => fromGenericRecord(AvroUtils.decodeAvro(f, schemaString)))
      } else {
        sc.pubsubTopic[Array[Byte]](s"projects/${utils.getProject}/topics/${tap.topic}", timestampAttribute = tap.timestampAttribute.orNull, idAttribute = tap.idAttribute.orNull)
          .map(f => fromGenericRecord(AvroUtils.decodeAvro(f, schemaString)))
      }
    }

    def typedPubSubProto[In <: GeneratedMessage with com.trueaccord.scalapb.Message[In] : Manifest](tap: PubSubTapDefinition, utils: SOTUtils)(implicit messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[In]): SCollection[In] = {

      if (tap.managedSubscription.isDefined && tap.managedSubscription.get) {
        utils.setPubsubTopic(s"projects/${utils.getProject}/topics/${tap.topic}")
        val subscriptionName = getSubscription(tap, utils)
        utils.setPubsubSubscription(subscriptionName)
        utils.setupPubsubSubscription()
        sc.pubsubSubscription[Array[Byte]](utils.getPubsubSubscription, timestampAttribute = tap.timestampAttribute.orNull, idAttribute = tap.idAttribute.orNull)
          .map(f => messageCompanion.parseFrom(f))
      } else {
        sc.pubsubTopic[Array[Byte]](s"projects/${utils.getProject}/topics/${tap.topic}", timestampAttribute = tap.timestampAttribute.orNull, idAttribute = tap.idAttribute.orNull)
          .map(f => messageCompanion.parseFrom(f))
      }
    }

    def typedPubSubJSON[In <: HasJSONAnnotation : Manifest](tap: PubSubTapDefinition, utils: SOTUtils)(implicit ev: io.circe.Decoder[In]): SCollection[In] = {

      if (tap.managedSubscription.isDefined && tap.managedSubscription.get) {
        utils.setPubsubTopic(s"projects/${utils.getProject}/topics/${tap.topic}")
        val subscriptionName = getSubscription(tap, utils)
        utils.setPubsubSubscription(subscriptionName)
        utils.setupPubsubSubscription()
        sc.pubsubSubscription[String](s"projects/${utils.getProject}/topics/${tap.topic}", timestampAttribute = tap.timestampAttribute.orNull, idAttribute = tap.idAttribute.orNull)
          .map { f =>
            decode[In](f) match {
              case Right(in) => in
              case Left(p) => throw p.fillInStackTrace()
            }
          }
      } else {
        sc.pubsubTopic[String](s"projects/${utils.getProject}/topics/${tap.topic}", timestampAttribute = tap.timestampAttribute.orNull, idAttribute = tap.idAttribute.orNull)
          .map { f =>
            decode[In](f) match {
              case Right(in) => in
              case Left(p) => throw p.fillInStackTrace()
            }
          }
      }
    }

    def typedKafkaProto[In <: GeneratedMessage with com.trueaccord.scalapb.Message[In] : Manifest](tap: KafkaTapDefinition, utils: SOTUtils)(implicit messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[In]): SCollection[In] = {

      val opt = KafkaOptions(tap.bootstrap, tap.topic, tap.group, tap.defaultOffset, tap.autoCommit)
      sc.readFromKafka(opt).map(f => messageCompanion.parseFrom(f))
    }

    def typedKafkaJSON[In <: HasJSONAnnotation : Manifest](tap: KafkaTapDefinition, utils: SOTUtils)(implicit ev: io.circe.Decoder[In]): SCollection[In] = {

      val opt = KafkaOptions(tap.bootstrap, tap.topic, tap.group, tap.defaultOffset, tap.autoCommit)

      sc.readFromKafka(opt).map { f =>
        val charset = Charset.forName("UTF-8")
        val f1 = new String(f, charset)
        decode[In](f1) match {
          case Right(in) => in
          case Left(p) => throw p.fillInStackTrace()
        }
      }
    }

    def typedGoogleStorageJSON[In <: HasJSONAnnotation : Manifest](path: String)(implicit ev: io.circe.Decoder[In]): SCollection[In] = {
      sc.textFile(path).map { f =>
        decode[In](f) match {
          case Right(in) => in
          case Left(p) => throw p.fillInStackTrace()
        }
      }
    }
  }

  implicit class PaiScioSCollectionAvro[Out <: HasAvroAnnotation : Manifest](c: SCollection[Out]) {
    def saveAsTypedPubSubAvro(tap: PubSubTapDefinition, utils: SOTUtils): Unit = {
      val avroT = AvroType[Out]
      val schemaOut = avroT.schema
      val toGenericRecordOut = avroT.toGenericRecord
      val schemaStringOut = schemaOut.toString

      //setup topic if it does not exist
      utils.setPubsubTopic(s"projects/${utils.getProject}/topics/${tap.topic}")
      utils.setupPubsubTopic()

      c.map(r => AvroUtils.encodeAvro(toGenericRecordOut(r), schemaStringOut)).saveAsPubsub(s"projects/${utils.getProject}/topics/${tap.topic}",
        timestampAttribute = tap.timestampAttribute.orNull, idAttribute = tap.idAttribute.orNull)
    }

    def saveAsTypedKafkaAvro(tap: KafkaTapDefinition, utils: SOTUtils): Unit = {
      val avroT = AvroType[Out]
      val schemaOut = avroT.schema
      val toGenericRecordOut = avroT.toGenericRecord
      val schemaStringOut = schemaOut.toString

      val opt = KafkaOptions(tap.bootstrap, tap.topic, tap.group, tap.defaultOffset, tap.autoCommit)

      c.map(r => AvroUtils.encodeAvro(toGenericRecordOut(r), schemaStringOut)).writeToKafka(opt)
    }

  }

  implicit class PaiScioSCollectionProto[T0 <: GeneratedMessage with com.trueaccord.scalapb.Message[T0]](c: SCollection[T0]) {
    def saveAsTypedPubSubProto(tap: PubSubTapDefinition, utils: SOTUtils)(implicit messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0]): Unit = {

      //setup topic if it does not exist
      utils.setPubsubTopic(s"projects/${utils.getProject}/topics/${tap.topic}")
      utils.setupPubsubTopic()

      c.map(r => messageCompanion.toByteArray(r)).saveAsPubsub(s"projects/${utils.getProject}/topics/${tap.topic}",
        timestampAttribute = tap.timestampAttribute.orNull, idAttribute = tap.idAttribute.orNull)
    }

    def saveAsTypedKafkaProto(tap: KafkaTapDefinition, utils: SOTUtils)(implicit messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0]): Unit = {

      val opt = KafkaOptions(tap.bootstrap, tap.topic, tap.group, tap.defaultOffset, tap.autoCommit)
      c.map(r => messageCompanion.toByteArray(r)).writeToKafka(opt)
    }
  }

  implicit class KVHListPaiScioSCollection(c: SCollection[Entity]) {

    def saveAsDatastoreSchemaless(project: String, kind: String, dedupeStrategy: DedupeStrategy, allowPartialUpdates: Boolean): Unit = {
      c.applyInternal(DatastoreIOSOT.v1.write.withProjectId(project).withDedupeStrategy(dedupeStrategy)
        .allowPartialUpdates(allowPartialUpdates))
    }

  }

  implicit class KVPaiScioSCollection[Out <: HasDatastoreAnnotation : Manifest, Key](c: SCollection[(Key, Out)]) {

    def saveAsDatastoreWithSchema[L <: HList](project: String, kind: String, dedupeStrategy: DedupeStrategy, allowPartialUpdates: Boolean)(implicit gen: LabelledGeneric.Aux[Out, L],
                                                                                                    toL: ToEntity[L]): Unit = {
      val dataStoreT: DatastoreType[Out] = DatastoreType[Out]

      c.map { case (key, rec) =>
        val entity = dataStoreT.toEntityBuilder(rec)
        entity.setKey(makeKey(kind, key.asInstanceOf[AnyRef]))
        entity.build()
      }.applyInternal(DatastoreIOSOT.v1.write.withProjectId(project)
        .withDedupeStrategy(dedupeStrategy)
        .allowPartialUpdates(allowPartialUpdates))
    }
  }

  implicit class PaiScioSCollectionJSON[L <: HList](c: SCollection[Row.Aux[L]]) {
    def saveAsKafka(tap: KafkaTapDefinition, utils: SOTUtils)(implicit en: io.circe.Encoder[L]): Unit = {
      val opt = KafkaOptions(tap.bootstrap, tap.topic, tap.group, tap.defaultOffset, tap.autoCommit)
      c.map(r => { val j = r.hList.asJson; j.toString().getBytes(Charset.forName("UTF-8"))}).writeToKafka(opt)
    }
  }
}