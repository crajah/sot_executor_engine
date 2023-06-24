package com.spotify.scio

import com.spotify.scio.testing.TestIO
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.io.kafka.KafkaIO
import org.apache.kafka.common.serialization._
import com.google.common.collect.ImmutableMap
import org.apache.kafka.clients.consumer.ConsumerConfig

package object kafka {

  case class KafkaOptions(bootstrap: String, topic: String, group: String, offset: String = "latest")

  case class KafkaTestIO[T](opt: KafkaOptions) extends TestIO[T](s"${opt.bootstrap}:${opt.topic}")

  implicit class KafkaScioContext(val self: ScioContext) extends AnyVal {

    def readFromKafka(opt: KafkaOptions): SCollection[Array[Byte]] = readFromKafkaBounded(opt, None)

    /**
      * This is mainly intended to be used for testing, for most cases `fromKafka` should be called
      */
    def readFromKafkaBounded(opt: KafkaOptions, boundCount: Option[Long] = None): SCollection[Array[Byte]] =
      self.requireNotClosed {
        if (self.isTest) {
          self.getTestInput[Array[Byte]](KafkaTestIO(opt))
        } else {
          val bdes: Deserializer[Array[Byte]] = new ByteArrayDeserializer
          val read = KafkaIO.read[String, Array[Byte]]
            .withBootstrapServers(opt.bootstrap)
            .withTopic(opt.topic)
              //            .withReadCommitted() // Looks like this is not supported by currently used version of Beam
            .withKeyDeserializer(classOf[StringDeserializer])
            .withValueDeserializer(bdes.getClass)
            .updateConsumerProperties(ImmutableMap.of(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, opt.offset))
            .updateConsumerProperties(ImmutableMap.of(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.box(true)))
            .updateConsumerProperties(ImmutableMap.of(ConsumerConfig.GROUP_ID_CONFIG, opt.group))

          val finalRead = boundCount match {
            case Some(c) if c >= 0 => read.withMaxNumRecords(c)
            case _ => read
          }
          self
            .wrap(self.applyInternal(finalRead)).setName(s"${opt.bootstrap}:${opt.topic}:${opt.group}")
            .map(kv => kv.getKV.getValue)
            .asInstanceOf[SCollection[Array[Byte]]]
        }
      }
  }

  implicit class KafkaSCollection(val self: SCollection[Array[Byte]]) {
    def writeToKafka(opt: KafkaOptions): Unit = {
      if (self.context.isTest) {
        self.context.testOut[Array[Byte]](KafkaTestIO[Array[Byte]](opt))(self)
      }
      else {
        val bdes: Serializer[Array[Byte]] = new ByteArraySerializer
        val write = KafkaIO.write[String, Array[Byte]]
          .withBootstrapServers(opt.bootstrap)
          .withTopic(opt.topic)
          .withKeySerializer(classOf[StringSerializer])
          .withValueSerializer(bdes.getClass)
          .values()
        self.applyInternal(write)
      }
    }
  }
}

