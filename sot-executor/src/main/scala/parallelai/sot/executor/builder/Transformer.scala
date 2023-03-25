package parallelai.sot.executor.builder

import com.spotify.scio.values.SCollection


trait Transformer[ANNO, TIN <: ANNO, ANNO_OUT, TOUT <: ANNO_OUT] {
  def transform(sCollection: SCollection[TIN]): SCollection[TOUT]
}

object Transformer {
  def apply[ANNO, TIN <: ANNO, ANNO_OUT, TOUT <: ANNO_OUT]()(implicit transformer: Transformer[ANNO, TIN, ANNO_OUT, TOUT]): Transformer[ANNO, TIN, ANNO_OUT, TOUT] = transformer
}
