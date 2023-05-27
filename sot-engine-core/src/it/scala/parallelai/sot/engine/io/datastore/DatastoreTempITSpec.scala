package parallelai.sot.engine.io.datastore

import shapeless._
import org.scalatest.{MustMatchers, WordSpec}
import parallelai.sot.engine.{Project, projectId}

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
class DatastoreTempITSpec extends WordSpec with MustMatchers {
  case class Foo(score1x: String, score2x: Float, teamscores: String)

  implicit val fooGen = LabelledGeneric[Foo]

  "" should {
    "" in {
      val datastore = Datastore(Project(projectId), Kind("testkind1"))

      println(datastore.get("AliceBlueCaneToad"))
    }
  }
}