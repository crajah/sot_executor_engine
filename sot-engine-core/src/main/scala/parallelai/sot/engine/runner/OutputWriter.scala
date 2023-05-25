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
import parallelai.sot.executor.model.SOTMacroConfig.{BigQueryTapDefinition, PubSubTapDefinition, TapDefinition}
import parallelai.sot.engine.runner.scio.PaiScioContext._
import shapeless.{HList, LabelledGeneric, Poly2}
import parallelai.sot.engine.io.bigquery._

trait Writer[TAP, UTIL, ANNO, Out]  extends Serializable {
  def write[TOUT <: HList](sc: SCollection[Row.Aux[TOUT]], tap: TAP, utils: UTIL)(implicit
                                                                                  gen: LabelledGeneric.Aux[Out, TOUT]): Unit
}

object Writer {
  def apply[TAP, UTIL, ANNO, Out](implicit reader: Writer[TAP, UTIL, ANNO, Out]) = reader

  implicit def pubSubWriterAvro[T0 <: HasAvroAnnotation : Manifest]: Writer[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0] = new Writer[PubSubTapDefinition, SOTUtils, HasAvroAnnotation, T0] {
    def write[OutR <: HList](sCollection: SCollection[Row.Aux[OutR]], tap: PubSubTapDefinition, utils: SOTUtils)(implicit
                                                                                                                 gen: LabelledGeneric.Aux[T0, OutR]): Unit = {
      utils.setPubsubTopic(s"projects/${utils.getProject}/topics/${tap.topic}")
      utils.setupPubsubTopic()
      sCollection.map(x => gen.from(x.hl)).saveAsTypedPubSubAvro(utils.getProject, tap.topic)
    }
  }

    implicit def pubSubWriterProto[T0 <: GeneratedMessage with com.trueaccord.scalapb.Message[T0] : Manifest](implicit
                                                                                                   messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[T0]): Writer[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0] = new Writer[PubSubTapDefinition, SOTUtils, GeneratedMessage, T0] {
      def write[OutR <: HList](sCollection: SCollection[Row.Aux[OutR]], tap: PubSubTapDefinition, utils: SOTUtils)(implicit
                                                                                         gen: LabelledGeneric.Aux[T0, OutR]): Unit = {
        utils.setPubsubTopic(s"projects/${utils.getProject}/topics/${tap.topic}")
        utils.setupPubsubTopic()
        sCollection.map(x => gen.from(x.hl)).saveAsTypedPubSubProto(utils.getProject, tap.topic)
      }
    }

    implicit def bigqueryWriter[T0 <: HasAnnotation : Manifest]: Writer[BigQueryTapDefinition, SOTUtils, HasAnnotation, T0] = new Writer[BigQueryTapDefinition, SOTUtils, HasAnnotation, T0] {
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

trait SchemalessWriter[TAP, UTIL, ANNO, OutR <: HList]  extends Serializable {
  def write(sc: SCollection[Row.Aux[OutR]], tap: TAP, utils: UTIL): Unit
}

object SchemalessWriter {
  implicit def bigQuerySchemalessWriter[OutR <: HList](implicit
                                                       hListSchemaProvider: HListSchemaProvider[OutR],
                                                       toL: ToTableRow[OutR]
                                                      ): SchemalessWriter[BigQueryTapDefinition, SOTUtils, com.google.api.client.json.GenericJson, OutR] = new SchemalessWriter[BigQueryTapDefinition, SOTUtils, com.google.api.client.json.GenericJson, OutR] {

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

object writer2 extends Poly2 {
  implicit def writer[TAP <: TapDefinition, UTIL, ANNO, Out, OutR <: HList, OutT <: HList]
  (implicit
   gen2: LabelledGeneric.Aux[Out, OutR],
   ev: OutT =:= OutR,
   writer: Writer[TAP, UTIL, ANNO, Out]
  )
  = at[(SCollection[Row.Aux[OutT]], UTIL), TapDef[TAP, UTIL, ANNO, Out]] {
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