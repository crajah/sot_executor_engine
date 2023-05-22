package parallelai.sot.engine.runner

import com.google.api.services.bigquery.model.{TableRow, TableSchema}
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.values.SCollection
import com.trueaccord.scalapb.GeneratedMessage
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.{CreateDisposition, WriteDisposition}
import parallelai.sot.engine.config.gcp.SOTUtils
import parallelai.sot.engine.generic.row.{DeepRec, Row}
import parallelai.sot.engine.io.{SchemalessTapDef, TapDef}
import parallelai.sot.engine.io.bigquery.{BigQuerySchemaProvider, HListSchemaProvider, ToTableRow}
import parallelai.sot.executor.model.SOTMacroConfig.{BigQueryTapDefinition, DatastoreTapDefinition, PubSubTapDefinition, TapDefinition}
import parallelai.sot.engine.runner.scio.PaiScioContext._
import shapeless.{HList, LabelledGeneric, Poly2}
import parallelai.sot.engine.io.bigquery._
import parallelai.sot.engine.io.datastore.{DatastoreType, ToEntity}
import parallelai.sot.engine.io.utils.annotations._
import shapeless.ops.hlist.IsHCons
import com.google.datastore.v1.client.DatastoreHelper.makeKey

trait Writer[TAP, UTIL, ANNO, Out] extends Serializable {
  def write[TOUT <: HList](sc: SCollection[Row.Aux[TOUT]], tap: TAP, utils: UTIL)(implicit
                                                                                  gen: LabelledGeneric.Aux[Out, TOUT]): Unit
}

trait DatastoreWriter[TAP, UTIL, ANNO, Out] extends Serializable {
  def write[TOUT <: HList](sc: SCollection[Row.Aux[TOUT]], tap: TAP, utils: UTIL)(implicit
                                                                                  gen: LabelledGeneric.Aux[Out, TOUT],
                                                                                  e: ToEntity[TOUT],
                                                                                  h: IsHCons[TOUT]): Unit
}

object Writer {
  def apply[TAP, UTIL, ANNO, Out](implicit reader: Writer[TAP, UTIL, ANNO, Out]) = reader

  implicit def pubSubWriterAvro[T0 <: HasAvroAnnotation : Manifest]: Writer[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0] =
    new Writer[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0] {
    def write[OutR <: HList](sCollection: SCollection[Row.Aux[OutR]], tap: PubSubTapDefinition, utils: SOTUtils)(implicit
                                                                                                                 gen: LabelledGeneric.Aux[T0, OutR]): Unit = {
      utils.setPubsubTopic(s"projects/${utils.getProject}/topics/${tap.topic}")
      utils.setupPubsubTopic()
      sCollection.map(x => gen.from(x.hl)).saveAsTypedPubSubAvro(utils.getProject, tap.topic)
    }
  }

  implicit def pubSubWriterProto[T0 <: GeneratedMessage with com.trueaccord.scalapb.Message[T0] : Manifest](implicit
                                                                                                            messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0]): Writer[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0] =
    new Writer[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0] {
    def write[OutR <: HList](sCollection: SCollection[Row.Aux[OutR]], tap: PubSubTapDefinition, utils: SOTUtils)(implicit
                                                                                                                 gen: LabelledGeneric.Aux[T0, OutR]): Unit = {
      utils.setPubsubTopic(s"projects/${utils.getProject}/topics/${tap.topic}")
      utils.setupPubsubTopic()
      sCollection.map(x => gen.from(x.hl)).saveAsTypedPubSubProto(utils.getProject, tap.topic)
    }
  }

  implicit def bigqueryWriter[T0 <: HasAnnotation : Manifest]: Writer[BigQueryTapDefinition, SOTUtils, HasAnnotation, T0] =
    new Writer[BigQueryTapDefinition, SOTUtils, HasAnnotation, T0] {
    def write[OutR <: HList](sCollection: SCollection[Row.Aux[OutR]], tap: BigQueryTapDefinition, utils: SOTUtils)(implicit
                                                                                                                   gen: LabelledGeneric.Aux[T0, OutR]): Unit = {
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

      sCollection.map(x => gen.from(x.hl)).saveAsTypedBigQuery(s"${tap.dataset}.${tap.table}", writeDisposition, createDisposition)
    }
  }
}

object DatastoreWriter {

  implicit def datastoreWriter[T0 <: HasDatastoreAnnotation : Manifest]: DatastoreWriter[DatastoreTapDefinition, SOTUtils, HasDatastoreAnnotation, T0] =
    new DatastoreWriter[DatastoreTapDefinition, SOTUtils, HasDatastoreAnnotation, T0] {
    def write[OutR <: HList](sCollection: SCollection[Row.Aux[OutR]], tap: DatastoreTapDefinition, utils: SOTUtils)(implicit
                                                                                                                    gen: LabelledGeneric.Aux[T0, OutR],
                                                                                                                    e: ToEntity[OutR],
                                                                                                                    h: IsHCons[OutR]): Unit = {
      sCollection.map(x => (h.head(x.hl), gen.from(x.hl))).saveAsTypedDatastore(utils.getProject, tap.kind)
    }
  }

}

trait SchemalessWriter[TAP, UTIL, ANNO, OutR <: HList] extends Serializable {
  def write(sc: SCollection[Row.Aux[OutR]], tap: TAP, utils: UTIL): Unit
}

trait DatastoreSchemalessWriter[TAP, UTIL, ANNO, OutR <: HList] extends Serializable {
  def write(sc: SCollection[Row.Aux[OutR]], tap: TAP, utils: UTIL)(implicit h: IsHCons[OutR], toL: ToEntity[OutR]): Unit
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

      sColl.map(m => {
        val tr = new TableRow()
        tr.putAll(toL(m.hl))
        tr
      }).saveAsBigQuery(s"${tap.dataset}.${tap.table}", schema, writeDisposition, createDisposition, null)
    }
  }
}

object DatastoreSchemalessWriter {

  implicit def datastoreSchemalessWriter[OutR <: HList](implicit
                                                        h: IsHCons[OutR],
                                                        toL: ToEntity[OutR]
                                                       ): SchemalessWriter[DatastoreTapDefinition, SOTUtils, Schemaless, OutR] =
    new SchemalessWriter[DatastoreTapDefinition, SOTUtils, Schemaless, OutR] {
    def write(sColl: SCollection[Row.Aux[OutR]], tap: DatastoreTapDefinition, utils: SOTUtils): Unit = {
      val project = utils.getProject
      sColl.map { rec =>
        val entity = DatastoreType.toEntityBuilder(rec.hl)
        val key = h.head(rec.hl)
        val keyEntity = key match {
          case name: String => makeKey(tap.kind, name.asInstanceOf[AnyRef])
          case id: Int => makeKey(tap.kind, id.asInstanceOf[AnyRef])
        }
        entity.setKey(keyEntity)
        entity.build()
      }.saveAsDatastore(project)
    }
  }

}

object writer2 extends Poly2 {

  implicit def writer0[TAP <: TapDefinition, UTIL, ANNO, Out, OutR <: HList, OutT <: HList]
  (implicit
   gen: LabelledGeneric.Aux[Out, OutR],
   h: IsHCons[OutR],
   ev: OutT =:= OutR,
   writer: Writer[TAP, UTIL, ANNO, Out]
  ) = at[(SCollection[Row.Aux[OutT]], UTIL), TapDef[TAP, UTIL, ANNO, Out]] {
    case ((sColl, utils), tap) =>
      writer.write(sColl.map(r => Row[OutR](ev(r.hl))), tap.tapDefinition, utils)
      (sColl, utils)
  }

  implicit def datastoreWriter[TAP <: DatastoreTapDefinition, UTIL, ANNO <: HasDatastoreAnnotation, Out, OutR <: HList, OutT <: HList]
  (implicit
   gen2: LabelledGeneric.Aux[Out, OutR],
   e: ToEntity[OutR],
   h: IsHCons[OutR],
   ev: OutT =:= OutR,
   writer: DatastoreWriter[TAP, UTIL, ANNO, Out]
  ) = at[(SCollection[Row.Aux[OutT]], UTIL), TapDef[TAP, UTIL, ANNO, Out]] {
    case ((sColl, utils), tap) =>
      writer.write(sColl.map(r => Row[OutR](ev(r.hl))), tap.tapDefinition, utils)
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