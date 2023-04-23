package parallelai.sot.executor.builder

import com.spotify.scio.ScioContext
import parallelai.sot.executor.common.SOTUtils
import parallelai.sot.executor.model.SOTMacroConfig.TapDefinition
import shapeless.Poly1


trait Runner[TAPIN, UTIL, AIN, IN <: AIN, AOUT, OUT <: AOUT, TAPOUT] {
  def exec(sc: ScioContext, tapIn: TapDefinition, tapOut: TapDefinition, sOTUtils: UTIL): Unit
}

object Runner {
  def apply[TAPIN, UTIL, AIN, IN <: AIN, AOUT, OUT <: AOUT, TAPOUT](implicit runner: Runner[TAPIN, UTIL, AIN, IN, AOUT, OUT, TAPOUT]):
  Runner[TAPIN, UTIL, AIN, IN, AOUT, OUT, TAPOUT] = runner

  implicit def genericRunner[TAPIN, UTIL, SIN, AIN, IN <: AIN : Manifest, AOUT, OUT <: AOUT : Manifest, TAPOUT, SCHEMA](
                                                                                           implicit reader: Reader[TAPIN, UTIL, AIN, IN],
                                                                                           transformer: Transformer[IN, OUT, SCHEMA],
                                                                                           writer: Writer[TAPOUT, UTIL, AOUT, OUT, SCHEMA]): Runner[TAPIN, UTIL, AIN, IN, AOUT, OUT, TAPOUT] = new Runner[TAPIN, UTIL, AIN, IN, AOUT, OUT, TAPOUT] {
    def exec(sc: ScioContext, tapIn: TapDefinition, tapOut: TapDefinition, sotUtils: UTIL): Unit = {
      val typedTapIn = tapIn.asInstanceOf[TAPIN] // oups
      val typedTapOut = tapOut.asInstanceOf[TAPOUT] // oups
      val read = reader.read(sc, typedTapIn , sotUtils)
      val (schema, value) = transformer.transform(read)
      writer.write(value, typedTapOut, sotUtils, schema)
    }
  }
}