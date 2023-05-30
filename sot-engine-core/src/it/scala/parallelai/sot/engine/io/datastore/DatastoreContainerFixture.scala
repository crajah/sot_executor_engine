package parallelai.sot.engine.io.datastore

import java.net.URI
import org.scalatest.Suite
import org.testcontainers.containers.wait.Wait
import com.google.cloud.datastore.{KeyQuery, Query}
import com.google.cloud.{NoCredentials, ServiceOptions}
import parallelai.sot.containers.{Container, ContainersFixture}
import parallelai.sot.engine.ProjectFixture

trait DatastoreContainerFixture {
  this: Suite with ContainersFixture with ProjectFixture =>

  lazy val kind: Kind = Kind("kind-test")

  lazy val datastoreContainer = new DatastoreContainer(8081)

  lazy val datastore = Datastore(project, kind,
                                 host = Some(new URI(datastoreContainer.ip)),
                                 credentials = Some(NoCredentials.getInstance),
                                 retry = Some(ServiceOptions.getNoRetrySettings))

  override def setup(): Unit = reset()

  override def teardown(): Unit = reset()

  private def reset(): Unit = {
    val queryAllKeys: KeyQuery = Query.newKeyQueryBuilder().setKind(kind.value).build()
    val result = datastore.run(queryAllKeys)

    while (result.hasNext) {
      datastore.delete(result.next())
    }
  }

  class DatastoreContainer(exposedPort: Int) extends Container(
    imageName = "google/cloud-sdk",
    exposedPorts = Seq(exposedPort),
    waitStrategy = Option(Wait.forHttp("/")),
    commands = Seq(s"gcloud beta emulators datastore start --project=${project.id} --host-port=0.0.0.0:$exposedPort")) {

    lazy val host: String = containerIpAddress

    lazy val port: Int = container.getMappedPort(exposedPort)

    lazy val ip: String = s"http://$host:$port"
  }
}