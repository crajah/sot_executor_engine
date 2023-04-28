package parallelai.sot.engine.config

import scala.reflect.ClassTag
import grizzled.slf4j.Logging
import pureconfig._
import com.typesafe.config.{Config, ConfigFactory}

trait SystemConfig {
  def config[C: ClassTag](implicit reader: Derivation[ConfigReader[C]]): C =
    SystemConfig[C]

  def config[C: ClassTag](namespace: String)(implicit reader: Derivation[ConfigReader[C]]): C =
    SystemConfig[C](namespace)
}

object SystemConfig extends Logging {
  def apply[C: ClassTag](implicit reader: Derivation[ConfigReader[C]]): C =
    loadConfigOrThrow[C](typesafeConfig)

  def apply[C: ClassTag](namespace: String)(implicit reader: Derivation[ConfigReader[C]]): C =
    loadConfigOrThrow[C](typesafeConfig, namespace)

  private def typesafeConfig: Config = {
    val typesafeConfig = ConfigFactory.load(getClass.getClassLoader).resolve()
    debug(s"Read config:\n${typesafeConfig.root().render()}")
    typesafeConfig
  }
}