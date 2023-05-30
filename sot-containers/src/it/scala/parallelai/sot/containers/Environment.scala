package parallelai.sot.containers

import java.net.ServerSocket
import scala.collection.mutable.ListBuffer
import grizzled.slf4j.Logging

trait Environment {
  /**
    *
    * @return Int A free port (though use it quickly in case another process steals it)
    */
  def freePort: Int = Environment.freePort
}

object Environment extends Logging {
  val ports: ListBuffer[Int] = ListBuffer.empty[Int]

  def freePort: Int = {
    val s = new ServerSocket(0)
    val port = s.getLocalPort

    if (ports.contains(port)) {
      freePort
    } else {
      ports += port
      info(s"Currently port $port is free - giving all lookup ports as: [${ports.mkString(", ")}]")
      port
    }
  }
}