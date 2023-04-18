package parallelai.sot.executor.builder

import com.google.api.services.bigquery.model.{TableFieldSchema, TableRow, TableSchema}
import com.google.protobuf.ByteString
import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.values.SCollection
import com.trueaccord.scalapb.GeneratedMessage
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import parallelai.sot.executor.bigquery.{BigQueryType, ToTableRow}
import parallelai.sot.executor.model.SOTMacroConfig.{BigQueryTapDefinition, PubSubTapDefinition}
import parallelai.sot.executor.scio.PaiScioContext._
import shapeless.{::, HList, HNil}

trait Writer[TAP, CONFIG, ANNO, TOUT, SCHEMA] {
  def write(sc: SCollection[TOUT], tap: TAP, config: CONFIG, schema: Option[SCHEMA])(implicit m: Manifest[TOUT]): Unit
}

object Writer {
  def apply[TAP, CONFIG, ANNO, TOUT, SCHEMA](implicit reader: Writer[TAP, CONFIG, ANNO, TOUT, SCHEMA]) = reader

  implicit def pubSubWriterAvro[T0 <: HasAvroAnnotation]: Writer[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0, Nothing] = new Writer[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0, Nothing] {
    def write(sCollection: SCollection[T0], tap: PubSubTapDefinition, config: GcpOptions, schema: Option[Nothing])(implicit m: Manifest[T0]): Unit = {
      sCollection.saveAsTypedPubSubAvro(config.getProject, tap.topic)
    }
  }

  implicit def pubSubWriterProto[T0 <: GeneratedMessage with com.trueaccord.scalapb.Message[T0]](implicit
                                                                                                 messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0]): Writer[PubSubTapDefinition, GcpOptions, GeneratedMessage, T0, Nothing] = new Writer[PubSubTapDefinition, GcpOptions, GeneratedMessage, T0, Nothing] {
    def write(sCollection: SCollection[T0], tap: PubSubTapDefinition, config: GcpOptions, schema: Option[Nothing])(implicit m: Manifest[T0]): Unit = {
      sCollection.saveAsTypedPubSubProto(config.getProject, tap.topic)
    }
  }

  implicit def bigqueryWriter[T0 <: HasAnnotation]: Writer[BigQueryTapDefinition, GcpOptions, HasAnnotation, T0, Nothing] = new Writer[BigQueryTapDefinition, GcpOptions, HasAnnotation, T0, Nothing] {
    def write(sCollection: SCollection[T0], tap: BigQueryTapDefinition, config: GcpOptions, schema: Option[Nothing])(implicit m: Manifest[T0]): Unit = {
      sCollection.saveAsTypedBigQuery(s"${tap.dataset}.${tap.table}")
    }
  }

  implicit def bigQuerySchemalessWriter = new Writer[BigQueryTapDefinition, GcpOptions, com.google.api.client.json.GenericJson, TableRow, TableSchema] {
    override def write(sc: SCollection[TableRow], tap: BigQueryTapDefinition, config: GcpOptions, schema: Option[TableSchema])(implicit m: Manifest[TableRow]): Unit = {
      sc.saveAsBigQuery(s"${tap.dataset}.${tap.table}", schema.get, null, null, null)
    }
  }

}
