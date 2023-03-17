package parallelai.sot.macros

import java.io.File

import com.typesafe.config.ConfigFactory
import parallelai.sot.executor.model.SOTMacroJsonConfig

trait EngineConfig {
  val source = getClass.getResource("/application.conf").getPath
  val fileName = ConfigFactory.parseFile(new File(source)).getString("json.file.name")
  val config = SOTMacroJsonConfig(fileName)

}
