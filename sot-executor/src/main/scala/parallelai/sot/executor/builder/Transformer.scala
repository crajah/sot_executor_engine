package parallelai.sot.executor.builder

import com.spotify.scio.values.SCollection


trait Transformer[ANNO, TIN, ANNO_OUT, TOUT] {
  def transform(sCollection: SCollection[TIN]): Result.Aux[ANNO_OUT, TOUT]
}

object Transformer {
  def apply[ANNO, TIN, ANNO_OUT, TOUT]()(implicit transformer: Transformer[ANNO, TIN, ANNO_OUT, TOUT]): Transformer[ANNO, TIN, ANNO_OUT, TOUT] = transformer
}
