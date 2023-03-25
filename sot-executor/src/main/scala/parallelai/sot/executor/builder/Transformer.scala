package parallelai.sot.executor.builder

import com.spotify.scio.values.SCollection
import shapeless.HList


trait Transformer[ANNO, TIN <: ANNO, ANNO_OUT, TOUT <: ANNO_OUT] {
  def transform(sCollection: SCollection[TIN]): SCollection[TOUT]
}

object Transformer {
  type Aux[IN <: HList, OUTGEN0 <: HList] = Transformer[IN]{type OUTGEN= OUTGEN0}

  def apply[IN <: HList, OUT <: HList](implicit transformer: Transformer[IN]): Transformer[IN] = transformer

  implicit def identity[IN <: HList]: Transformer.Aux[IN, IN] = new Transformer[IN] {
    type OUTGEN = IN
    def transform(sCollection: SCollection[Row[IN]]): SCollection[Row[OUTGEN]] = {
      sCollection
    }
  }
}
