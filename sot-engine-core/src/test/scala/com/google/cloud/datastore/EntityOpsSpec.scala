package com.google.cloud.datastore

import shapeless._
import org.scalatest.{MustMatchers, WordSpec}
import com.google.cloud.datastore.EntityOps._
import parallelai.sot.engine.io.datastore._

class EntityOpsSpec extends WordSpec with MustMatchers {
  case class Superhero(name: String, age: Int)

  implicit val superheroGen = LabelledGeneric[Superhero]

  implicit val keyFactory: KeyFactory = new KeyFactory("blah").setKind("blahKind")

  "Entity" should {
    "be generated from some class" in {
      val entity = toEntity(1, Superhero("Batman", 42))

      entity.getString("name") mustEqual "Batman"
      entity.getLong("age") mustEqual 42
    }

    "generate a class instance" in {
      val superhero = Superhero("Batman", 42)
      val entity = toEntity("one", superhero)

      fromEntity(entity) mustBe Some(superhero)
    }

    // TODO - Whoops!
    "be incorrectly generated" in {
      val key = keyFactory.newKey(42)

      val entity: Entity =
        Entity.newBuilder(key)
          .set("name", false)
          .set("age", "42")
          .build()

      fromEntity(entity) mustBe Some(Superhero("", 0))
    }
  }
}
