package parallelai.sot.executor.builder

import com.spotify.scio.ScioContext
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import parallelai.sot.executor.model.SOTMacroConfig.{BigQueryTapDefinition, PubSubTapDefinition}
import parallelai.sot.executor.scio.PaiScioContext._
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

trait Writer[TAP, CONFIG, ANNO, TOUT] {
  def write(sc: SCollection[TOUT], tap: TAP, config: CONFIG)(implicit m: Manifest[TOUT]): Unit
}

object Writer {
  def apply[TAP, CONFIG, ANNO, TOUT](implicit reader: Writer[TAP, CONFIG, ANNO, TOUT]) = reader

  implicit def pubSubAvroWrtier[T0 <: HasAvroAnnotation]: Writer[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] = new Writer[PubSubTapDefinition, GcpOptions, HasAvroAnnotation, T0] {
    def write(sCollection: SCollection[T0], tap: PubSubTapDefinition, config: GcpOptions)(implicit m: Manifest[T0]): Unit = {
      sCollection.saveAsTypedPubSub(config.getProject, tap.topic)
    }
  }

  implicit def bigqueryWriter[T0 <: HasAnnotation]: Writer[BigQueryTapDefinition, GcpOptions, HasAnnotation, T0] = new Writer[BigQueryTapDefinition, GcpOptions, HasAnnotation, T0] {
    def write(sCollection: SCollection[T0], tap: BigQueryTapDefinition, config: GcpOptions)(implicit m: Manifest[T0]): Unit = {
      sCollection.saveAsTypedBigQuery(s"${tap.dataset}.${tap.table}")
    }
  }
}
