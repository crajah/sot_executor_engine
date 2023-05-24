package parallelai.sot.engine.generic.helper

import java.util.TimeZone

import org.joda.time.{DateTimeZone, Instant => JodaInstant}
import org.joda.time.format.DateTimeFormat

import scala.util.Random

object Helper {
  def fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    .withZone(DateTimeZone.forTimeZone(TimeZone.getTimeZone("UTC")))

  val random : Random.type = scala.util.Random

  object Instant {
    def now() = JodaInstant.now()
  }
}