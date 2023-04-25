package parallelai.sot.engine.runner

import com.google.api.services.bigquery.model.{TableRow, TableSchema}
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.values.SCollection
import com.trueaccord.scalapb.GeneratedMessage
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.{CreateDisposition, WriteDisposition}
import parallelai.sot.engine.config.gcp.SOTUtils
import parallelai.sot.executor.model.SOTMacroConfig.{BigQueryTapDefinition, PubSubTapDefinition}
import parallelai.sot.engine.runner.scio.PaiScioContext._

trait Writer[TAP, UTIL, ANNO, TOUT, SCHEMA] {
  def write(sc: SCollection[TOUT], tap: TAP, utils: UTIL, schema: Option[SCHEMA])(implicit m: Manifest[TOUT]): Unit
}

object Writer {
  def apply[TAP, UTIL, ANNO, TOUT, SCHEMA](implicit reader: Writer[TAP, UTIL, ANNO, TOUT, SCHEMA]) = reader

  implicit def pubSubWriterAvro[T0 <: HasAvroAnnotation]: Writer[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0, Nothing] = new Writer[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0, Nothing] {
    def write(sCollection: SCollection[T0], tap: PubSubTapDefinition, utils: SOTUtils, schema: Option[Nothing])(implicit m: Manifest[T0]): Unit = {
      utils.setPubsubTopic(s"projects/${utils.getProject}/topics/${tap.topic}")
      utils.setupPubsubTopic()
      sCollection.saveAsTypedPubSubAvro(utils.getProject, tap.topic)
    }
  }

  implicit def pubSubWriterProto[T0 <: GeneratedMessage with com.trueaccord.scalapb.Message[T0]](implicit
                                                                                                 messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0]): Writer[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0, Nothing] = new Writer[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0, Nothing] {
    def write(sCollection: SCollection[T0], tap: PubSubTapDefinition, utils: SOTUtils, schema: Option[Nothing])(implicit m: Manifest[T0]): Unit = {
      utils.setPubsubTopic(s"projects/${utils.getProject}/topics/${tap.topic}")
      utils.setupPubsubTopic()
      sCollection.saveAsTypedPubSubProto(utils.getProject, tap.topic)
    }
  }

  implicit def bigqueryWriter[T0 <: HasAnnotation]: Writer[BigQueryTapDefinition, SOTUtils, HasAnnotation, T0, Nothing] = new Writer[BigQueryTapDefinition, SOTUtils, HasAnnotation, T0, Nothing] {
    def write(sCollection: SCollection[T0], tap: BigQueryTapDefinition, utils: SOTUtils, schema: Option[Nothing])(implicit m: Manifest[T0]): Unit = {
      val createDisposition = tap.createDisposition match {
        case Some(cd) if cd == "CREATE_IF_NEEDED" => CreateDisposition.CREATE_IF_NEEDED
        case Some(cd) if cd == "CREATE_NEVER" => CreateDisposition.CREATE_NEVER
        case None => CreateDisposition.CREATE_IF_NEEDED
      }
      val writeDisposition = tap.writeDisposition match {
        case Some(wd) if wd == "WRITE_TRUNCATE" => WriteDisposition.WRITE_TRUNCATE
        case Some(wd) if wd == "WRITE_EMPTY" => WriteDisposition.WRITE_EMPTY
        case Some(wd) if wd == "WRITE_APPEND" => WriteDisposition.WRITE_APPEND
        case None => WriteDisposition.WRITE_EMPTY
      }

      sCollection.saveAsTypedBigQuery(s"${tap.dataset}.${tap.table}", writeDisposition, createDisposition)
    }
  }

  implicit def bigQuerySchemalessWriter: Writer[BigQueryTapDefinition, SOTUtils, com.google.api.client.json.GenericJson, TableRow, TableSchema] = new Writer[BigQueryTapDefinition, SOTUtils, com.google.api.client.json.GenericJson, TableRow, TableSchema] {
    override def write(sc: SCollection[TableRow], tap: BigQueryTapDefinition, utils: SOTUtils, schema: Option[TableSchema])(implicit m: Manifest[TableRow]): Unit = {
      val createDisposition = tap.createDisposition match {
        case Some(cd) if cd == "CREATE_IF_NEEDED" => CreateDisposition.CREATE_IF_NEEDED
        case Some(cd) if cd == "CREATE_NEVER" => CreateDisposition.CREATE_NEVER
        case None => CreateDisposition.CREATE_IF_NEEDED
      }
      val writeDisposition = tap.writeDisposition match {
        case Some(wd) if wd == "WRITE_TRUNCATE" => WriteDisposition.WRITE_TRUNCATE
        case Some(wd) if wd == "WRITE_EMPTY" => WriteDisposition.WRITE_EMPTY
        case Some(wd) if wd == "WRITE_APPEND" => WriteDisposition.WRITE_APPEND
        case None => WriteDisposition.WRITE_EMPTY
      }

      sc.saveAsBigQuery(s"${tap.dataset}.${tap.table}", schema.get, writeDisposition, createDisposition, null)
    }
  }

}
