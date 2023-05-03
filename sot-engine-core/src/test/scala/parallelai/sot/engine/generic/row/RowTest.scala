package parallelai.sot.engine.generic.row

import org.scalatest.{Matchers, WordSpec}
import shapeless._
import shapeless.labelled.FieldType
import shapeless.record._
import shapeless.syntax.singleton._
import shapeless.tag.Tagged

class RowTest extends WordSpec with Matchers {

  case class FlatCaseClass(a: Int, b: String, c: Double)

  case class NestedRecord(i: Int)

  case class NestedCaseClass(a: Int, b: String, c: Double, n: NestedRecord)

  "Row parser" should {

    "parse a flat structure" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      row.hl should be('a ->> 1 :: 'b ->> "b" :: 'c ->> 1.0 :: HNil)

    }

    "parse a nested structure" in {

      val ncc = NestedCaseClass(1, "b", 1.0, NestedRecord(1))

      val row = Row(ncc)

      row.hl should be('a ->> 1 :: 'b ->> "b" :: 'c ->> 1.0 :: 'n ->> ('i ->> 1 :: HNil) :: HNil)

    }

  }

  "Row operations" should {

    "append the row object with a new column" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowAppended = row.append('d, 2.0)

      rowAppended.hl should be('a ->> 1 :: 'b ->> "b" :: 'c ->> 1.0 :: 'd ->> 2.0 :: HNil)

    }

    "remove a column from the row object" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowAppended = row.remove('a)

      rowAppended.hl should be('b ->> "b" :: 'c ->> 1.0 :: HNil)

    }

    "update an existing column" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowUpdated = row.update('a, 2)

      rowUpdated.hl should be('a ->> 2 :: 'b ->> "b" :: 'c ->> 1.0 :: HNil)

    }

    "update an existing column based on the value of the column" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowUpdated = row.updateWith('a)(_ + 20)


      rowUpdated.hl should be('a ->> 21 :: 'b ->> "b" :: 'c ->> 1.0 :: HNil)

    }

    "update an existing column with different type" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      //a gets converted from int to string
      val rowUpdated = row.update('a, "2")

      rowUpdated.hl should be('a ->> "2" :: 'b ->> "b" :: 'c ->> 1.0 :: HNil)

    }

    "select a single field from a row" in {

      val ncc = NestedCaseClass(1, "b", 1.0, NestedRecord(1))

      val row = Row(ncc)

      row.get('n).get('i) should be(1)

    }

    "select multiple fields from a row" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowProjected = row.projectTyped[HList.`'a, 'b`.T]

      rowProjected.hl should be ('a ->> 1 :: 'b ->> "b" :: HNil)

    }

    "select multiple fields from a nested row" in {

      val ncc = NestedCaseClass(1, "b", 1.0, NestedRecord(1))

      val row = Row(ncc)

      type v1 = Witness.`'a`.T :: Witness.`'n`.T :: HNil

      type v2 = HList.`'a, 'n`.T

      val rowProjected = row.projectTyped[v1]

      rowProjected.hl should be ('a ->> 1 :: 'n ->> ('i ->> 1 :: HNil) :: HNil)

    }

    "select nested fields from a row" in {

      case class Level3Record(iii: Int)

      case class Level2Record(ii: Int, l3: Level3Record)

      case class Level1Record(l2: Level2Record, i: Int)

      case class NestedCaseClass(l1: Level1Record, a: Int, b: String, c: Double)

      val ncc = NestedCaseClass(a = 123, b = "bbb", c = 1.23,
        l1 = Level1Record(l2 = Level2Record(ii = 2233, l3 = Level3Record(iii = 32423)), i = 3333))

      val row = Row(ncc)

      type selector = Nested[Witness.`'l1`.T, Nested[Witness.`'l2`.T, Nested[Witness.`'l3`.T, Witness.`'iii`.T]]] ::
        Nested[Witness.`'l1`.T, Nested[Witness.`'l2`.T, Witness.`'l3`.T]] ::
        Witness.`'a`.T :: HNil

      val rowProjected = row.projectTyped[selector]

      rowProjected.hl should be ('iii ->> 32423 :: 'l3 ->> ('iii ->> 32423 :: HNil) :: 'a ->> 123 :: HNil)

    }

    "select nested fields from a row where projection is passed as a parameter" in {

      case class Level3Record(iii: Int)

      case class Level2Record(ii: Int, l3: Level3Record)

      case class Level1Record(l2: Level2Record, i: Int)

      case class NestedCaseClass(l1: Level1Record, a: Int, b: String, c: Double)

      val ncc = NestedCaseClass(a = 123, b = "bbb", c = 1.23,
        l1 = Level1Record(l2 = Level2Record(ii = 2233, l3 = Level3Record(iii = 32423)), i = 3333))

      val row = Row(ncc)

      val selector = Nested(Witness('l1), Nested(Witness('l2), Nested(Witness('l3), Witness('iii)))) ::
        Nested(Witness('l1), Nested(Witness('l2), Witness('l3))) ::
        Witness('a) :: HNil

      val rowProjected = row.project(selector)

      rowProjected.hl should be ('iii ->> 32423 :: 'l3 ->> ('iii ->> 32423 :: HNil) :: 'a ->> 123 :: HNil)

    }

  }

  "Row converter" should {

    "convert a flat row back to a case class" in {

      case class FlatCaseClassAppended(a: Int, b: String, c: Double, d: Double)

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowAppended = row.append('d, 2.0)

      val fccAppended = Row.to[FlatCaseClassAppended].from(rowAppended.hl)

      fccAppended should be(FlatCaseClassAppended(1, "b", 1.0, 2.0))

    }

    "convert a nested row back to a case class" in {

      case class NestedCaseClassAppended(a: Int, b: String, c: Double, n: NestedRecord, m: Option[Long])

      val ncc = NestedCaseClass(1, "b", 1.0, NestedRecord(1))

      val row = Row(ncc)

      val rowAppended = row.append('m, Option(1L))

      val nccAppended = Row.to[NestedCaseClassAppended].from(rowAppended.hl)

      nccAppended should be(NestedCaseClassAppended(1, "b", 1.0, NestedRecord(1), Some(1L)))

    }

  }

}

