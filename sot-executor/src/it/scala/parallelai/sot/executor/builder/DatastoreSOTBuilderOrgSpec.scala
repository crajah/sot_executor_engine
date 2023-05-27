/*
package parallelai.sot.executor.builder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import shapeless._
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.SerializableCoder
import org.apache.beam.sdk.transforms.DoFn.ProcessElement
import org.apache.beam.sdk.transforms.{Create, ParDo}
import org.apache.beam.sdk.values.PCollection
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.{NoCredentials, ServiceOptions}
import com.spotify.scio.ScioContext
import com.spotify.scio.extra.transforms.DoFnWithResource.ResourceType
import com.spotify.scio.extra.transforms.{DoFnWithResource, ScalaAsyncDoFn}
import com.spotify.scio.values.SCollection
import parallelai.sot.containers.ContainersForAllExamples
import parallelai.sot.containers.gcloud.DatastoreContainer
import parallelai.sot.engine._
import parallelai.sot.engine.generic.row.Row
import parallelai.sot.engine.io.datastore.Datastore
import scala.concurrent.duration._
import com.spotify.scio._

import scala.concurrent.Await

/**
  * Requires a local instance of Google Datastore e.g.
  * <pre>
  *   $ gcloud beta emulators datastore start
  * </pre>
  * The above command will highlight which port Datastore will be running on.
  * Said port can also be given by:
  * <pre>
  *   $ gcloud beta emulators datastore env-init
  * </pre>
  * and this also sets some environment variables one, DATASTORE_EMULATOR_HOST, which is required by this test - where all are:
  * <pre>
  *   export DATASTORE_DATASET=bi-crm-poc
  *   export DATASTORE_EMULATOR_HOST=localhost:8081
  *   export DATASTORE_EMULATOR_HOST_PATH=localhost:8081/datastore
  *   export DATASTORE_HOST=http://localhost:8081
  *   export DATASTORE_PROJECT_ID=bi-crm-poc
  * </pre>
  * So, you could run (first command) the Datastore emulator in detached mode and then run (second command) the env-int to then run this test.
  * <p/>
  * Or (much better) use docker:
  * <pre>
  *   $ cd ./docker/gcloud/datastore
  *   $ docker build -t gcloud-datastore .
  *   $ docker run -p 8081:8081 --name my-gcloud-datastore gcloud-datastore
  * </pre>
  * By using [[https://github.com/testcontainers/testcontainers-scala]] we can mix in a Docker Container rule which requires a "container" (or containers) to configured for a test,
  * i.e. we can configure and start up containers programmatically as use in this specification.
  */
class DatastoreSOTBuilderOrgSpec extends WordSpec with MustMatchers with ScalaFutures with ContainersForAllExamples {
  val datastorePort: Int = DatastoreContainer.exposeFreePort()

  override val container = DatastoreContainer(datastorePort)

  def datastore = {
    val datastoreOptions: DatastoreOptions = DatastoreOptions.newBuilder()
      .setProjectId(project)
      .setHost(s"http://0.0.0.0:8081")
      .setCredentials(NoCredentials.getInstance)
      .setRetrySettings(ServiceOptions.getNoRetrySettings)
      .build()

    implicit val blahGen = LabelledGeneric[Blah]

    val datastore = new Datastore(datastoreOptions)
    datastore.put("blah", Blah("yip"))
    datastore
  }

  /*

  implicit def genericTransformation: Transformer[Message, MessageExtended, Nothing] = new Transformer[Message, MessageExtended, Nothing] {
    type Out = (Option[Nothing], SCollection[MessageExtended])

    def transform(rowIn: SCollection[Message]): Out = {
      val converter = Row.to[MessageExtended]
      val in = rowIn.map(r => Row(r))
      val trans = in.map(m => m.append('count, 1))
      (None, trans.map(r => converter.from(r.hList)))
    }
  }

   */



  type Out = SCollection[Message]

  def transform(rowIn: SCollection[Message]): Out = {
    //val trans = in.map(m => m.append('blah, datastore.get("id"))) // TODO where datastore query will give a Message (that was converted from the Entity found)

    /*def proc = new ScalaAsyncDoFn[Message, Message, Datastore] {
      def processElement(m: Message): Future[Message] = Future {
        //println(getResource.get("id"))
        println(s"===> processElement")
        m
      }

      def createResource(): Datastore = datastore

      def getResourceType: ResourceType = ResourceType.PER_CLASS
    }

    rowIn.applyTransform(ParDo.of(proc))*/

    val converter = Row.to[Message]
    val in = rowIn.map(r => Row(r))

    val trans = in map { m =>
      (m, datastore)
    } map { case (m, datastore) =>
      println(s"===> m = $m")
      // implicit val blahGen = LabelledGeneric[Blah]
      //val b: Option[Blah] = datastore.get("blah")
      // m.append('blah, b.get)
      m
    }

    // TODO -> assert trans

    //implicit val messageGen = LabelledGeneric[MessageExtended]

    trans.map(r => converter.from(r.hList))
  }

  "" should {
    "" in {
      /*val p: Pipeline = Pipeline.create()

      //val pp: PCollection[Message] = p.apply(Create.of(Message("user", "teamName", score = 1, eventTime = 1, "eventTimeStr")).withCoder(SerializableCoder.of(Message.getClass.asInstanceOf[Class[Message]])))

      val sc = ScioContext()


      //val blah = transform(sc.apply().parallelize(Seq(Message("user", "teamName", score = 1, eventTime = 1, "eventTimeStr"))))

      val blah = sc.parallelize(Seq(Message("user", "teamName", score = 1, eventTime = 1, "eventTimeStr"))).map { m =>
        println(s"===> Message = $m")
        m
      }


      println(blah.materialize.waitForResult(20 seconds))*/


      val (sc1, args) = ContextAndArgs(Array(""))

      val f1 = sc1.parallelize(1 to 10)
        .sum
        .materialize  // save data to a temporary location for use later

      val f2 = sc1.parallelize(1 to 100)
        .sum
        .map(_.toString)
        .saveAsTextFile(args("output"))  // save data for use later
      sc1.close()

      // wait for future completion in case job is non-blocking
      val t1 = f1.waitForResult()
      val t2 = f2.waitForResult()

      // fetch tap values directly
      println(t1.value.mkString(", "))
      println(t2.value.mkString(", "))
    }
  }
}

case class Message(user: String, teamName: String, score: Int, eventTime: Long, eventTimeStr: String)

case class MessageExtended(user: String, teamName: String, score: Int, eventTime: Long, eventTimeStr: String, blah: Blah)

case class Blah(whatever: String)*/
