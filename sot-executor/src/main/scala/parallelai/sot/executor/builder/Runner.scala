package parallelai.sot.executor.builder

import com.spotify.scio.ScioContext
import parallelai.sot.executor.model.SOTMacroConfig.TapDefinition
import shapeless.Poly1


trait Runner[TAPIN, CONF, AIN, IN <: AIN, AOUT, OUT <: AOUT, TAPOUT] {
  def exec(sc: ScioContext, tapIn: TapDefinition, tapOut: TapDefinition, config:CONF): Unit
}

object Runner {
  def apply[TAPIN, CONF, AIN, IN <: AIN, AOUT, OUT <: AOUT, TAPOUT](implicit runner: Runner[TAPIN, CONF, AIN, IN, AOUT, OUT, TAPOUT]):
  Runner[TAPIN, CONF, AIN, IN, AOUT, OUT, TAPOUT] = runner

  implicit def genericRunner[TAPIN, CONF, SIN, AIN, IN <: AIN : Manifest, AOUT, OUT <: AOUT : Manifest, TAPOUT, SCHEMA](
                                                                                           implicit reader: Reader[TAPIN, CONF, AIN, IN],
                                                                                           transformer: Transformer[IN, OUT, SCHEMA],
                                                                                           writer: Writer[TAPOUT, CONF, AOUT, OUT, SCHEMA]): Runner[TAPIN, CONF, AIN, IN, AOUT, OUT, TAPOUT] = new Runner[TAPIN, CONF, AIN, IN, AOUT, OUT, TAPOUT] {
    def exec(sc: ScioContext, tapIn: TapDefinition, tapOut: TapDefinition, config:CONF): Unit = {
      val typedTapIn = tapIn.asInstanceOf[TAPIN] // oups
      val typedTapOut = tapOut.asInstanceOf[TAPOUT] // oups
      val read = reader.read(sc, typedTapIn , config)
      val (schema, value) = transformer.transform(read)
      writer.write(value, typedTapOut, config, schema)
    }
  }
}