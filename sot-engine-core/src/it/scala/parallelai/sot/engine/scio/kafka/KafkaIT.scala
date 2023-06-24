package parallelai.sot.engine.scio.kafka

import com.spotify.scio.kafka._
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar

import com.spotify.scio._
import com.spotify.scio.testing._
import org.apache.beam.sdk.options.{PipelineOptions, PipelineOptionsFactory}
import org.junit.Test
import org.scalatest.PrivateMethodTester
import parallelai.sot.containers.{Container, ForAllContainersFixture}

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import org.slf4j.LoggerFactory

/**
  * Requires a local instance of Kafka available on 0.0.0.0:9092
  *
  * Best way to do this is by using a docker container running both Kafka and Zookeeper (Kafka needs Zookeeper to run).
  *
  * There is a docker image in docker public repo <b>all4it<b> running Kafka v 0.10 that fits the purpose.
  *
  * Other similar docker images or non-docker Kafka instances should work too as long as they are accessible via
  * 0.0.0.0:9092 and are compatible with the client built for v 0.10 of Kafka
  * <pre>
  *   docker run -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=0.0.0.0 --env ADVERTISED_PORT=9092 all4it/local-kafka:v2
  * </pre>
  *
  * An alternative to running the Docker container manually is to mix in a Container fixture which will configure and
  * start up the container programmatically and that's what we use here.
  *
  * This will run only the current test
  * <pre>
  * $ sbt "it:testOnly *KafkaIT"
  * </pre>
  */
object KafkaIT {

  val kafkaOptionsLatest = KafkaOptions("0.0.0.0:9092", "my-topic", "my-group", "latest")
  val kafkaOptionsEarliest = kafkaOptionsLatest.copy(offset = "earliest")
  val charset = Charset.forName("UTF-8")
  val timeFormat = new SimpleDateFormat("hh:mm:ss")
  val runID = timeFormat.format(Calendar.getInstance.getTime())
  val testStaticData = Seq("Some message from Kafka Integration Test", "Second message", "3rd test message")
  val testDynamicData = testStaticData.map(v => s"$runID: " + v)
}

class KafkaIT extends PipelineSpec with ForAllContainersFixture with KafkaContainerFixture {

  System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG")
  val logger = LoggerFactory.getLogger(classOf[KafkaIT])
  logger.info("Starting KafkaIT integration test")

  import KafkaIT._

  def createContext(appName: String = "MyApp"): ScioContext = {
//    val opts = PipelineOptionsFactory.fromArgs(s"--appName=$appName").as(classOf[PipelineOptions])
//    new ScioContext(opts, List[String]())
    ScioContext()
  }

  def write(): Unit = {
    logger.debug("--> write")
    val sc = createContext()
    sc.parallelize(testDynamicData.map(_.getBytes(charset)).toArray).writeToKafka(kafkaOptionsLatest)
    sc.close()
    logger.debug("<-- write")
  }

  @Test
  def read(fromStart: Boolean = false, count: Int = 3) = {
    logger.debug("--> read")
    val sc = createContext()
    val data = count match {
      case c if c > 0 => testDynamicData
      case _ => Seq.empty[String]
    }
    // TODO: make this work with .take instead of forcing bounded reads
    sc.readFromKafkaBounded(if (fromStart) kafkaOptionsEarliest else kafkaOptionsLatest, Some(count)).map(v => new String(v, charset)) should containInAnyOrder(data)
    sc.close()
    logger.debug("<-- read")
  }

  "KafkaIO" should "write" in {
    write
  }

  it should "read zero records from beginning" in {
    read(fromStart = true, count = 0)
  }

  it should "read what's written" in {

    // This is needed to assure that read starts before teh write is done
    val w = Future {
      // Pause for at least 20 sec, increase if test is getting blocked
      Thread.sleep(20000)
      write
    }
    read()
  }
}
