package parallelai.sot.executor.builder

import com.spotify.scio.ScioContext
import parallelai.sot.executor.model.SOTMacroConfig.TapDefinition
import shapeless.Poly1


object Runner1 extends Poly1 with Serializable {
  implicit def runner[TAPIN, CONF, AIN, IN <: AIN, AOUT, OUT <: AOUT, TAPOUT](implicit runner: Runner[TAPIN, CONF, AIN, IN, AOUT, OUT, TAPOUT]) = at[RunnerConfig[TAPIN, CONF, AIN, IN, AOUT, OUT, TAPOUT]] {
    case config =>
      val inSchema: SchemaType.Aux[AIN, IN] = config.inSchema
      val outSchema : SchemaType.Aux[AOUT, OUT] = config.outSchema
      runner.exec(inSchema, outSchema) _
  }
}

trait Runner[TAPIN, CONF, AIN, IN <: AIN, AOUT, OUT <: AOUT, TAPOUT] {
  def exec(in: SchemaType.Aux[AIN, IN], out: SchemaType.Aux[AOUT, OUT])(sc: ScioContext, tapIn: TapDefinition, tapOut: TapDefinition, config:CONF): Unit
}

object Runner {
  def apply[TAPIN, CONF, AIN, IN <: AIN, AOUT, OUT <: AOUT, TAPOUT](implicit runner: Runner[TAPIN, CONF, AIN, IN, AOUT, OUT, TAPOUT]):
  Runner[TAPIN, CONF, AIN, IN, AOUT, OUT, TAPOUT] = runner

  implicit def genericRunner[TAPIN, CONF, SIN, AIN, IN <: AIN, AOUT, OUT <: AOUT, TAPOUT](
                                                                                           implicit reader: Reader[TAPIN, CONF, AIN, IN],
                                                                                           transformer: Transformer[AIN, IN, AOUT, OUT],
                                                                                           writer: Writer[TAPOUT, CONF, AOUT, OUT]): Runner[TAPIN, CONF, AIN, IN, AOUT, OUT, TAPOUT] = new Runner[TAPIN, CONF, AIN, IN, AOUT, OUT, TAPOUT] {
    def exec(in: SchemaType.Aux[AIN, IN], out: SchemaType.Aux[AOUT, OUT])(sc: ScioContext, tapIn: TapDefinition, tapOut: TapDefinition, config:CONF): Unit = {
      val typedTapIn = tapIn.asInstanceOf[TAPIN] // oups
      val typedTapOut = tapOut.asInstanceOf[TAPOUT] // oups
      val read = reader.read(sc, typedTapIn , config)(in.m)
      val value = transformer.transform(read.res)
      writer.write(value.res, typedTapOut, config)(out.m)
    }
  }
}

case class RunnerConfig[TAPIN, CONF, AIN, IN <: AIN, AOUT, OUT <: AOUT, TAPOUT](inSchema: SchemaType.Aux[AIN, IN], outSchema: SchemaType.Aux[AOUT, OUT])

trait SchemaType {
  type A
  type T <: A
  val m: Manifest[T]
}

object SchemaType {
  type Aux[A0, T0 <: A0] = SchemaType {type A = A0; type T = T0}

  def apply[A0, T0 <: A0]()(implicit ma: Manifest[T0], ev: T0 <:< A0): SchemaType.Aux[A0, T0] = {
    new SchemaType() {
      type T = T0
      type A = A0
      val m = ma
    }
  }
}