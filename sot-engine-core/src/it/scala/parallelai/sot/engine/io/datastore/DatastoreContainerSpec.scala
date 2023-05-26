package parallelai.sot.engine.io.datastore

import java.net.URI
import java.util.UUID
import org.scalatest.Suite
import com.google.cloud.datastore.{KeyQuery, Query}
import com.google.cloud.{NoCredentials, ServiceOptions}
import parallelai.sot.containers.ContainersSpec
import parallelai.sot.containers.io.datastore.DatastoreContainer
import parallelai.sot.engine.Project

trait DatastoreContainerSpec {
  this: Suite with ContainersSpec =>

  lazy val project: Project = Project(UUID.randomUUID().toString)

  lazy val kind: Kind = Kind(UUID.randomUUID().toString)

  lazy val datastoreContainer: DatastoreContainer = new DatastoreContainer

  lazy val datastore = Datastore(project, kind,
                                 host = Some(new URI(s"http://${datastoreContainer.containerIpAddress}:${datastoreContainer.datastorePort}")),
                                 credentials = Some(NoCredentials.getInstance),
                                 retry = Some(ServiceOptions.getNoRetrySettings))

  override def teardown(): Unit = {
    val queryAllKeys: KeyQuery = Query.newKeyQueryBuilder().setKind(kind.value).build()
    val result = datastore.run(queryAllKeys)

    while (result.hasNext) {
      datastore.delete(result.next())
    }
  }
}