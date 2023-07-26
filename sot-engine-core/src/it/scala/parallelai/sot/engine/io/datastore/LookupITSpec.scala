package parallelai.sot.engine.io.datastore

import org.scalatest.{MustMatchers, WordSpec}
import parallelai.sot.containers.{Container, ForAllContainersFixture}
import parallelai.sot.engine.ProjectFixture
import shapeless._


class LookupITSpec extends WordSpec with MustMatchers with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture {
  case class Foo(one: String, two: String)

  case class Foo1(one: Boolean, two: Int)

  implicit val fooGen: LabelledGeneric[Foo] = LabelledGeneric[Foo]

  implicit val foo1Gen: LabelledGeneric[Foo1] = LabelledGeneric[Foo1]

  override val container: Container = datastoreContainer

  "Lookup" should {
    "persist data by ID of type Long" in {
      val foo = Foo("oneValue", "twoValue")
      datastore.put(42, foo)
      val option: Option[Boolean] = Lookup[Boolean](datastore).get("42")
      option mustBe Some(foo)
    }

    "not find a value given a non existing key" in {
      Lookup[Int](datastore).get("blah") mustBe None
    }
  }
}