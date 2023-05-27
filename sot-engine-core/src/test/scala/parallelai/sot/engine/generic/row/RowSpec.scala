package parallelai.sot.engine.generic.row

import shapeless._
import org.scalatest.{MustMatchers, WordSpec}

class RowSpec extends WordSpec with MustMatchers {
  "Row" should {
    "wrap a case class" in {
      case class Test(blah: String)

      val test = Test("Blah")
      val row = Row(test)

      Row.to[Test].from(row.hList) mustEqual test

      row.keys mustEqual 'blah :: HNil
      test.blah mustEqual row.get('blah)

      Row.to[Test].from(row.update('blah, "Blah Blah").hList) mustEqual Test("Blah Blah")

      "Blah Blah Blah" :: HNil mustEqual row.updatedAt(0, "Blah Blah Blah")

      Row.to[Test].from(row.updateWith('blah)(_ + " more Blah").hList) mustEqual Test("Blah more Blah")

      "Blah" :: "New Blah" :: HNil mustEqual row.append('newBlah, "New Blah").hList
      "Blah" :: "New Blah" :: HNil mustEqual row.appendList("New Blah" :: HNil).hList
      HNil mustEqual row.remove('blah).hList
    }
  }
}