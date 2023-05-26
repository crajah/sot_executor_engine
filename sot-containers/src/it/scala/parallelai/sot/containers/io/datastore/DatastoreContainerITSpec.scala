package parallelai.sot.containers.io.datastore

import org.scalatest.{MustMatchers, WordSpec}
import org.testcontainers.containers.ContainerFetchException

class DatastoreContainerITSpec extends WordSpec with MustMatchers {
  "Datestore Container" should {
    "require a valid image" in {
      new DatastoreContainer("davidainslie/gcloud-datastore:latest") mustBe a [DatastoreContainer]

      a [ContainerFetchException] must be thrownBy new DatastoreContainer("blah-blah-and-more-blah/gcloud-datastore:latest")
    }

    "default an image to the 'latest' tag" in {
      new DatastoreContainer("davidainslie/gcloud-datastore") mustBe a [DatastoreContainer]
    }

    "require a correctly tagged image" in {
      a [ContainerFetchException] must be thrownBy new DatastoreContainer("davidainslie/gcloud-datastore:whoops")
    }

    "bind ports" in {
      val container = new DatastoreContainer("davidainslie/gcloud-datastore:latest", datastorePort = 8001)

      container.portBindings mustBe Seq("8001:8081")
    }

    "utilise environment variable" in {
      val container = new DatastoreContainer("davidainslie/gcloud-datastore:latest", environment = Map("KEY" -> "VALUE"))

      container.env mustBe Seq("KEY=VALUE")
    }
  }
}