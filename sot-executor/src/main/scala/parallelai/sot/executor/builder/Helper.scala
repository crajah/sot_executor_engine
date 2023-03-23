package parallelai.sot.executor.builder

import java.util.TimeZone

import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

object Helper {
  def fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    .withZone(DateTimeZone.forTimeZone(TimeZone.getTimeZone("PST")))

}