package parallelai.sot.executor.builder

import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import parallelai.sot.executor.model.SOTMacroConfig.{BigQueryTapDefinition, PubSubTapDefinition}
import parallelai.sot.executor.scio.PaiScioContext._
import shapeless.{HList, LabelledGeneric}
trait OutputWriter[T, C, A] {
  def write[Out <: A : Manifest](sCollection: SCollection[Out], tap: T, config: C): Unit
}

object OutputWriter {

  def apply[T, C, A](implicit outputWriter: OutputWriter[T, C, A]) = outputWriter

  implicit def bigQueryWriter = new OutputWriter[BigQueryTapDefinition, GcpOptions, HasAnnotation] {
    def write[Out <: HasAnnotation : Manifest]
    (sCollection: SCollection[Out], tap: BigQueryTapDefinition, config: GcpOptions): Unit = {
      sCollection.saveAsTypedBigQuery(s"${tap.dataset}.${tap.table}")
    }
  }

  implicit def pubSubWriter = new OutputWriter[PubSubTapDefinition, GcpOptions, HasAvroAnnotation] {
    def write[Out <: HasAvroAnnotation : Manifest]
    (sCollection: SCollection[Out], tap: PubSubTapDefinition, config: GcpOptions): Unit = {
      sCollection.saveAsPubsub(tap.topic)
    }
  }

}

trait Writer[TAP, CONFIG, ANNO, TOUT, OUTGEN <: HList] {
  def write(sc: SCollection[Row[OUTGEN]], tap: TAP, config: CONFIG)(implicit m: Manifest[TOUT]): Unit
}

object Writer {
  def apply[TAP, CONFIG, ANNO, TOUT, OUTGEN <: HList](implicit reader: Writer[TAP, CONFIG, ANNO, TOUT, OUTGEN]) = reader

  implicit def pubSubAvroWrtier[TOUT <: HasAvroAnnotation, OUTGEN <: HList](implicit lableleGeneric: LabelledGeneric.Aux[TOUT, OUTGEN]): Writer[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, TOUT, OUTGEN] = new Writer[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, TOUT, OUTGEN] {
    def write(sCollection: SCollection[Row[OUTGEN]], tap: PubSubTapDefinition, config: GcpOptions)(implicit m: Manifest[TOUT]): Unit = {
      sCollection.map(x => x.to[TOUT]).saveAsTypedPubSub(config.getProject, tap.topic)
    }
  }

  implicit def bigqueryWriter[TOUT <: HasAnnotation, OUTGEN <: HList](implicit lableleGeneric: LabelledGeneric.Aux[TOUT, OUTGEN]): Writer[BigQueryTapDefinition, GcpOptions, HasAnnotation, TOUT, OUTGEN] = new Writer[BigQueryTapDefinition, GcpOptions, HasAnnotation, TOUT, OUTGEN] {
    def write(sCollection: SCollection[Row[OUTGEN]], tap: BigQueryTapDefinition, config: GcpOptions)(implicit m: Manifest[TOUT]): Unit = {
      sCollection.map(x => x.to[TOUT]).saveAsTypedBigQuery(s"${tap.dataset}.${tap.table}")
    }
  }
}
