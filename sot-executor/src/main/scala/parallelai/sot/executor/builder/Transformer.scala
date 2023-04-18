package parallelai.sot.executor.builder

import com.spotify.scio.values.SCollection


trait Transformer[TIN, TOUT, SCHEMA] {
  def transform(sCollection: SCollection[TIN]): (Option[SCHEMA], SCollection[TOUT])
}

object Transformer {
  def apply[TIN, TOUT, SCHEMA]()(implicit transformer: Transformer[TIN, TOUT, SCHEMA]): Transformer[TIN, TOUT, SCHEMA] = transformer
}
