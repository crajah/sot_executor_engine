package parallelai.sot.executor.builder

import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import parallelai.sot.executor.model.SOTMacroConfig.{BigQueryTapDefinition, PubSubTapDefinition}

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