package parallelai.sot.engine.io

import parallelai.sot.executor.model.SOTMacroConfig.TapDefinition

case class TapDef[T <: TapDefinition, UTIL, ANNO, C](rowTapDefinition: TapDefinition) {
  def tapDefinition: T = rowTapDefinition.asInstanceOf[T]
}

case class SchemalessTapDef[T <: TapDefinition, UTIL, ANNO](rowTapDefinition: TapDefinition) {
  def tapDefinition: T = rowTapDefinition.asInstanceOf[T]
}