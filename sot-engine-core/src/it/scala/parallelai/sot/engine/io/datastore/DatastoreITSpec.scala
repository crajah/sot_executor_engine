package parallelai.sot.engine.io.datastore

import shapeless._
import org.scalatest.{MustMatchers, WordSpec}
import com.google.cloud.datastore.Entity
import parallelai.sot.containers.{Container, ForAllContainersFixture}
import parallelai.sot.engine.ProjectFixture

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
class DatastoreITSpec extends WordSpec with MustMatchers with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture {
  case class Foo(one: String, two: String)

  case class Foo1(one: Boolean, two: Int)

  implicit val fooGen: LabelledGeneric[Foo] = LabelledGeneric[Foo]

  implicit val foo1Gen: LabelledGeneric[Foo1] = LabelledGeneric[Foo1]

  override val container: Container = datastoreContainer

  "Datastore" should {
    "persist data by ID of type Long" in {
      val foo = Foo("oneValue", "twoValue")
      datastore.put(42, foo)

      datastore.get[Foo](42) mustBe Some(foo)
    }

    "persist data by ID of type String" in {
      val foo = Foo("oneValue", "twoValue")
      datastore.put("myFoo", foo)

      datastore.get[Foo]("myFoo") mustBe Some(foo)
    }

    "not find a value given a non existing key" in {
      datastore.get[Foo]("blah") mustBe None
    }

    // TODO - This seems dodgy
    "find and generate domain data even when there is extra data" in {
      val key = datastore.keyFactory.newKey(42)

      val entity: Entity =
        Entity.newBuilder(key)
          .set("one", "oneAgain")
          .set("two", "twoAgain")
          .set("three", "extra!")
          .build()

      datastore.put(entity)

      datastore.get[Foo](42) mustBe Some(Foo("oneAgain", "twoAgain"))
    }

    "find but not generate domain data when there is not enough data" in {
      val key = datastore.keyFactory.newKey(42)

      val entity: Entity =
        Entity.newBuilder(key)
          .set("one", "oneAgain")
          .build()

      datastore.put(entity)

      datastore.get[Foo](42) mustBe None
    }

    // TODO - dodgy
    "find but incorrectly generate domain data when data types do not match" in {
      val key = datastore.keyFactory.newKey(42)

      val entity: Entity =
        Entity.newBuilder(key)
          .set("one", 1, 2, 3)
          .set("two", "The '1' for field 'one' is the problem")
          .build()

      datastore.put(entity)

      datastore.get[Foo](42) mustBe Some(Foo("", "The '1' for field 'one' is the problem"))
    }

    // TODO - dodgy
    "find but again incorrectly generate domain data when data types do not match" in {
      val key = datastore.keyFactory.newKey(42)

      val entity: Entity =
        Entity.newBuilder(key)
          .set("one", false)
          .set("two", false)
          .build()

      datastore.put(entity)

      datastore.get[Foo](42) mustBe Some(Foo("", ""))
    }

    // TODO - dodgy
    "blah" in {
      datastore.put(42, Foo1(one = false, 6))

      val result: Option[Foo] = datastore.get[Foo](42)

      result mustBe Some(Foo("", ""))
    }
  }
}