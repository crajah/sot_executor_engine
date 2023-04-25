package parallelai.sot.engine.generic.helper

import java.util.TimeZone

import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

object Helper {
  def fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    .withZone(DateTimeZone.forTimeZone(TimeZone.getTimeZone("UTC")))

}