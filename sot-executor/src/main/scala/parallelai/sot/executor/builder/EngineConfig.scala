package parallelai.sot.executor.builder

import java.io.File

import com.typesafe.config.ConfigFactory
import parallelai.sot.executor.model.SOTMacroJsonConfig

@deprecated(message = "Seems that this is no longer used, besides it has hardcoding", since = "9th November 2017")
trait EngineConfig {
  val source = getClass.getResource("/application.conf").getPath
  val fileName = ConfigFactory.parseFile(new File(source)).getString("json.file.name")
  val jobConfig = SOTMacroJsonConfig(fileName)
}