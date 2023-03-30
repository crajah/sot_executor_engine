package parallelai.sot.executor.builder

import com.spotify.scio.values.SCollection


trait Transformer[TIN, TOUT] {
  def transform(sCollection: SCollection[TIN]): SCollection[TOUT]
}

object Transformer {
  def apply[TIN, TOUT]()(implicit transformer: Transformer[TIN, TOUT]): Transformer[TIN, TOUT] = transformer
}
