package parallelai.sot.containers

import java.net.{ServerSocket, SocketException}
import scala.util.{Failure, Success, Try}
import grizzled.slf4j.Logging

trait Environment extends Logging {
  /**
    * @param fromPort Int Lowest available free port required
    * @param toPort Int Highest available free port required
    * @return Int The acquired free port (though use it quickly in case another process steals it)
    */
  def freePort(fromPort: Int = 8000, toPort: Int = 9000): Int = {
    // TODO I suspect that this is really nonsense!
    def exposeFreePort(port: Int): Int = Try(new ServerSocket(fromPort)) match {
      case Success(ss) =>
        ss.close()
        info(s"Currently port $port is free")
        port

      case Failure(t) =>
        if (fromPort < toPort) exposeFreePort(fromPort + 1)
        else throw new SocketException(s"Could not acquire a free port between $fromPort and $toPort")
    }

    exposeFreePort(fromPort)
  }
}

object Environment extends Environment