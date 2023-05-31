package parallelai.sot.containers

import org.scalatest.{MustMatchers, WordSpec}
import org.testcontainers.containers.ContainerFetchException

class ContainerITSpec extends WordSpec with MustMatchers {
  "Container" should {
    "require a valid image" in {
      new Container("davidainslie/gcloud-datastore:latest") mustBe a [Container]

      a [ContainerFetchException] must be thrownBy new Container("blah-blah-and-more-blah/gcloud-datastore:latest")
    }

    "default an image to the 'latest' tag" in {
      new Container("davidainslie/gcloud-datastore") mustBe a [Container]
    }

    "require a correctly tagged image" in {
      a [ContainerFetchException] must be thrownBy new Container("davidainslie/gcloud-datastore:whoops")
    }

    "utilise environment variable" in {
      val container = new Container("davidainslie/gcloud-datastore:latest", environment = Map("KEY" -> "VALUE"))

      container.env mustBe Seq("KEY=VALUE")
    }
  }
}