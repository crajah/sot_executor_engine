package parallelai.sot.engine.io.datastore

import com.google.cloud.datastore.Entity
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
//      val option: Option[Boolean] = Lookup(datastore).get("42")(LabelledGeneric[String],)
//      option mustBe Some(foo)
    }

//    "persist data by ID of type String" in {
//      val foo = Foo("oneValue", "twoValue")
//      datastore.put("myFoo", foo)
//
//      datastore.get[Foo]("myFoo") mustBe Some(foo)
//    }
//
//    "not find a value given a non existing key" in {
//      datastore.get[Foo]("blah") mustBe None
//    }
//
//    // TODO - This seems dodgy
//    "find and generate domain data even when there is extra data" in {
//      val key = datastore.keyFactory.newKey(42)
//
//      val entity: Entity =
//        Entity.newBuilder(key)
//          .set("one", "oneAgain")
//          .set("two", "twoAgain")
//          .set("three", "extra!")
//          .build()
//
//      datastore.put(entity)
//
//      datastore.get[Foo](42) mustBe Some(Foo("oneAgain", "twoAgain"))
//    }
//
//    "find but not generate domain data when there is not enough data" in {
//      val key = datastore.keyFactory.newKey(42)
//
//      val entity: Entity =
//        Entity.newBuilder(key)
//          .set("one", "oneAgain")
//          .build()
//
//      datastore.put(entity)
//
//      datastore.get[Foo](42) mustBe None
//    }
//
//    // TODO - dodgy
//    "find but incorrectly generate domain data when data types do not match" in {
//      val key = datastore.keyFactory.newKey(42)
//
//      val entity: Entity =
//        Entity.newBuilder(key)
//          .set("one", 1, 2, 3)
//          .set("two", "The '1' for field 'one' is the problem")
//          .build()
//
//      datastore.put(entity)
//
//      datastore.get[Foo](42) mustBe Some(Foo("", "The '1' for field 'one' is the problem"))
//    }
//
//    // TODO - dodgy
//    "find but again incorrectly generate domain data when data types do not match" in {
//      val key = datastore.keyFactory.newKey(42)
//
//      val entity: Entity =
//        Entity.newBuilder(key)
//          .set("one", false)
//          .set("two", false)
//          .build()
//
//      datastore.put(entity)
//
//      datastore.get[Foo](42) mustBe Some(Foo("", ""))
//    }
//
//    // TODO - dodgy
//    "blah" in {
//      datastore.put(42, Foo1(one = false, 6))
//
//      val result: Option[Foo] = datastore.get[Foo](42)
//
//      result mustBe Some(Foo("", ""))
//    }
  }
}