package parallelai.sot.engine.runner

import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.values.SCollection
import com.trueaccord.scalapb.GeneratedMessage
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.{CreateDisposition, WriteDisposition}
import parallelai.sot.engine.config.gcp.SOTUtils
import parallelai.sot.engine.generic.row.{DeepRec, Row}
import parallelai.sot.engine.io.{SchemalessTapDef, TapDef}
import parallelai.sot.executor.model.SOTMacroConfig._
import com.spotify.scio.sot.PaiScioContext._
import shapeless.{HList, LabelledGeneric, Poly2}
import parallelai.sot.engine.io.utils.annotations._
import com.google.datastore.v1.client.DatastoreHelper.makeKey
import shapeless.ops.hlist.IsHCons
import parallelai.sot.engine.io.datastore._
import parallelai.sot.engine.io.bigquery._

trait Writer[TAP, UTIL, ANNO, Out, TOUT <: HList] extends Serializable {
  def write(sc: SCollection[Row.Aux[TOUT]], tap: TAP, utils: UTIL): Unit
}

object Writer {
  def apply[TAP, UTIL, ANNO, Out, TOUT <: HList](implicit reader: Writer[TAP, UTIL, ANNO, Out, TOUT]) = reader

  implicit def pubSubWriterAvro[T0 <: HasAvroAnnotation : Manifest, OutR <: HList](implicit
                                                                                   gen: LabelledGeneric.Aux[T0, OutR]):
  Writer[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0, OutR] =
    new Writer[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0, OutR] {
      def write(sCollection: SCollection[Row.Aux[OutR]], tap: PubSubTapDefinition, utils: SOTUtils): Unit = {
        sCollection.map(x => gen.from(x.hList)).saveAsTypedPubSubAvro(tap, utils)
      }
    }

  implicit def pubSubWriterProto[T0 <: GeneratedMessage with com.trueaccord.scalapb.Message[T0] : Manifest, OutR <: HList](implicit
                                                                                                                           gen: LabelledGeneric.Aux[T0, OutR],
                                                                                                                           messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0]):
  Writer[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0, OutR] =
    new Writer[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0, OutR] {
      def write(sCollection: SCollection[Row.Aux[OutR]], tap: PubSubTapDefinition, utils: SOTUtils): Unit = {
        sCollection.map(x => gen.from(x.hList)).saveAsTypedPubSubProto(tap, utils)
      }
    }

  implicit def bigqueryWriter[T0 <: HasAnnotation : Manifest, OutR <: HList](implicit
                                                                             gen: LabelledGeneric.Aux[T0, OutR]):
  Writer[BigQueryTapDefinition, SOTUtils, HasAnnotation, T0, OutR] =
    new Writer[BigQueryTapDefinition, SOTUtils, HasAnnotation, T0, OutR] {
      def write(sCollection: SCollection[Row.Aux[OutR]], tap: BigQueryTapDefinition, utils: SOTUtils): Unit = {
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

        sCollection.map(x => gen.from(x.hList)).saveAsTypedBigQuery(s"${tap.dataset}.${tap.table}", writeDisposition, createDisposition)
      }
    }

  implicit def datastoreWriter[T0 <: HasDatastoreAnnotation : Manifest, OutR <: HList](implicit
                                                                                       gen: LabelledGeneric.Aux[T0, OutR],
                                                                                       e: ToEntity[OutR],
                                                                                       h: IsHCons[OutR]):
  Writer[DatastoreTapDefinition, SOTUtils, HasDatastoreAnnotation, T0, OutR] =
    new Writer[DatastoreTapDefinition, SOTUtils, HasDatastoreAnnotation, T0, OutR] {
      def write(sCollection: SCollection[Row.Aux[OutR]], tap: DatastoreTapDefinition, utils: SOTUtils): Unit = {
        sCollection.map(x => (h.head(x.hList), gen.from(x.hList))).saveAsDatastoreWithSchema(utils.getProject, tap.kind, tap.dedupCommits)
      }
    }
}

trait SchemalessWriter[TAP, UTIL, ANNO, OutR <: HList] extends Serializable {
  def write(sc: SCollection[Row.Aux[OutR]], tap: TAP, utils: UTIL): Unit
}

object SchemalessWriter {
  implicit def bigQuerySchemalessWriter[OutR <: HList](implicit
                                                       hListSchemaProvider: HListSchemaProvider[OutR],
                                                       toL: ToTableRow[OutR]
                                                      ): SchemalessWriter[BigQueryTapDefinition, SOTUtils, Schemaless, OutR] =
    new SchemalessWriter[BigQueryTapDefinition, SOTUtils, Schemaless, OutR] {

      def write(sColl: SCollection[Row.Aux[OutR]], tap: BigQueryTapDefinition, utils: SOTUtils): Unit = {
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
        val schema = BigQuerySchemaProvider[OutR].getSchema

        sColl.map(m => m.hList.toTableRow(toL)).saveAsBigQuery(s"${tap.dataset}.${tap.table}", schema, writeDisposition, createDisposition, null)
      }
    }

  implicit def datastoreSchemalessWriter[OutR <: HList](implicit h: IsHCons[OutR], toL: ToEntity[OutR]):
  SchemalessWriter[DatastoreTapDefinition, SOTUtils, Schemaless, OutR] =
    new SchemalessWriter[DatastoreTapDefinition, SOTUtils, Schemaless, OutR] {
      def write(sColl: SCollection[Row.Aux[OutR]], tap: DatastoreTapDefinition, utils: SOTUtils): Unit = {

        val project = utils.getProject
        sColl.map { rec =>
          val entity = rec.hList.toEntityBuilder
          val key = h.head(rec.hList)
          val keyEntity = key match {
            case name: String => makeKey(tap.kind, name.asInstanceOf[AnyRef])
            case id: Int => makeKey(tap.kind, id.asInstanceOf[AnyRef])
          }
          entity.setKey(keyEntity)
          entity.build()
        }.saveAsDatastore(project)
      }
    }

  implicit def kafkaWriter[OutR <: HList](implicit ev: io.circe.Encoder[OutR]):
  SchemalessWriter[KafkaTapDefinition, SOTUtils, Schemaless, OutR] =
    new SchemalessWriter[KafkaTapDefinition, SOTUtils, Schemaless, OutR] {
      def write(sCollection: SCollection[Row.Aux[OutR]], tap: KafkaTapDefinition, utils: SOTUtils): Unit = {
        sCollection.saveAsKafka(tap, utils)
      }
    }

}

object writer2 extends Poly2 {

  implicit def writer0[TAP <: TapDefinition, UTIL, ANNO, Out, OutR <: HList, OutT <: HList]
  (implicit
   gen: LabelledGeneric.Aux[Out, OutR],
   h: IsHCons[OutR],
   ev: OutT =:= OutR,
   writer: Writer[TAP, UTIL, ANNO, Out, OutR]
  ) = at[(SCollection[Row.Aux[OutT]], UTIL), TapDef[TAP, UTIL, ANNO, Out]] {
    case ((sColl, utils), tap) =>
      writer.write(sColl.map(r => Row[OutR](ev(r.hList))), tap.tapDefinition, utils)
      (sColl, utils)
  }

  implicit def schemalessWriter[TAP <: TapDefinition, UTIL, ANNO, OutT <: HList]
  (implicit
   writer: SchemalessWriter[TAP, UTIL, ANNO, OutT]
  ) = at[(SCollection[Row.Aux[OutT]], UTIL), SchemalessTapDef[TAP, UTIL, ANNO]] {
    case ((sColl, utils), tap) =>
      writer.write(sColl, tap.tapDefinition, utils)
      (sColl, utils)
  }
}