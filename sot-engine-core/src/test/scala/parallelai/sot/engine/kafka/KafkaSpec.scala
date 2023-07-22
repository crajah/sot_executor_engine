package parallelai.sot.engine.kafka

import com.spotify.scio.kafka._

import java.net.InetSocketAddress
import java.nio.charset.Charset

import com.spotify.scio.{ContextAndArgs, ScioContext}
import com.spotify.scio.testing.PipelineSpec
import org.joda.time.Duration

/**
  * Can be run by
  * <pre>
  * $ sbt "testOnly *KafkaSpec"
  * </pre>
  */
object KafkaJobSpec {
  val options = KafkaOptions("DUMMY-BOOTSTRAP", "SOME-TOPIC", None, None, None)
  val input: Seq[Array[Byte]] = Seq("a", "b", "c").map(_.getBytes(Charset.forName("UTF8")))
}

object KafkaJob {

  def main(cmdlineArgs: Array[String]): Unit = {

    import KafkaJobSpec._
    val (sc, _) = ContextAndArgs(cmdlineArgs)
    sc
      .readFromKafka(options)
      .map(v => new String(v, Charset.forName("UTF8")).toUpperCase().getBytes(Charset.forName("UTF8")))
      .writeToKafka(options)
    sc.close()
  }
}

class KafkaSpec extends PipelineSpec {

  def testKafka(expected: String*): Unit = {

    import KafkaJobSpec._

    JobTest[KafkaJob.type]
      .input(KafkaTestIO(options), input)
      .output(KafkaTestIO[Array[Byte]](options)) {
        _ should containInAnyOrder(expected.map(_.getBytes(Charset.forName("UTF8"))))
      }
      .run()
  }

  it should "pass with correct expected output" in {
    testKafka("A", "B", "C")
  }

  it should "fail for incorrect expected output" in {
    an [AssertionError] should be thrownBy testKafka("A", "B")
    an [AssertionError] should be thrownBy testKafka("a", "B", "C")
    an [AssertionError] should be thrownBy testKafka("A", "B", "C", "D")
  }
}
