package parallelai.sot.engine.generic.row

import org.scalatest.{Matchers, WordSpec}
import shapeless._
import shapeless.labelled.FieldType
import shapeless.record._
import shapeless.syntax.singleton._
import shapeless.test.illTyped

class RowTest extends WordSpec with Matchers {

  case class FlatCaseClass(a: Int, b: String, c: Double)

  case class NestedRecord(i: Int)

  case class NestedCaseClass(a: Int, b: String, c: Double, n: NestedRecord)

  "Row parser" should {

    "parse a flat structure" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      row.hList should be('a ->> 1 :: 'b ->> "b" :: 'c ->> 1.0 :: HNil)

      row.get('a) should be(1)
      row.get('b) should be("b")
      row.get('c) should be(1.0)

    }

    "parse a nested structure" in {

      val ncc = NestedCaseClass(1, "b", 1.0, NestedRecord(1))

      val row = Row(ncc)

      row.hList should be('a ->> 1 :: 'b ->> "b" :: 'c ->> 1.0 :: 'n ->> ('i ->> 1 :: HNil) :: HNil)

      row.get('a) should be(1)
      row.get('b) should be("b")
      row.get('c) should be(1.0)
      row.get('n) should be('i ->> 1 :: HNil)
      row.get('n).get('i) should be(1)

    }

  }

  "Row operations" should {

    "concat two rows" in {

      val r1 = Row('a ->> 1 :: 'b ->> "b" :: HNil)
      val r2 = Row('c ->> 1.0 :: 'd ->> 2.0 :: HNil)

      val rconcat = r1.concat(r2)

      rconcat.hList should be ('a ->> 1 :: 'b ->> "b" :: 'c ->> 1.0 :: 'd ->> 2.0 :: HNil)

    }

    "append the row object with a new column" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowAppended = row.append('d, 2.0)

      rowAppended.hList should be('a ->> 1 :: 'b ->> "b" :: 'c ->> 1.0 :: 'd ->> 2.0 :: HNil)

      rowAppended.get('a) should be(1)
      rowAppended.get('b) should be("b")
      rowAppended.get('c) should be(1.0)
      rowAppended.get('d) should be(2.0)

    }

    "remove a column from the row object" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowAppended = row.remove('a)

      rowAppended.hList should be('b ->> "b" :: 'c ->> 1.0 :: HNil)

      rowAppended.get('b) should be("b")
      rowAppended.get('c) should be(1.0)

      illTyped("row.remove('z)")

    }

    "update an existing column" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowUpdated = row.update('a, 2)

      rowUpdated.hList should be('a ->> 2 :: 'b ->> "b" :: 'c ->> 1.0 :: HNil)

      rowUpdated.get('a) should be(2)
      rowUpdated.get('b) should be("b")
      rowUpdated.get('c) should be(1.0)

      illTyped("row.update('z, 2)")

    }

    "update an existing column based on the value of the column" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowUpdated = row.updateWith('a)(_ + 20)


      rowUpdated.hList should be('a ->> 21 :: 'b ->> "b" :: 'c ->> 1.0 :: HNil)

      rowUpdated.get('a) should be(21)
      rowUpdated.get('b) should be("b")
      rowUpdated.get('c) should be(1.0)

      illTyped("row.updateWith('z)(1)")

    }

    "update an existing column with different type" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      //a gets converted from int to string
      val rowUpdated = row.update('a, "2")

      rowUpdated.hList should be('a ->> "2" :: 'b ->> "b" :: 'c ->> 1.0 :: HNil)

      rowUpdated.get('a) should be("2")
      rowUpdated.get('b) should be("b")
      rowUpdated.get('c) should be(1.0)

    }

    "select a single field from a row" in {

      val ncc = NestedCaseClass(1, "b", 1.0, NestedRecord(1))

      val row = Row(ncc)

      row.get('n).get('i) should be(1)

      illTyped("row.get('n).get('k)")

    }

    "select multiple fields from a row" in {

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowProjected = row.projectTyped[HList.`'a, 'b`.T]

      rowProjected.hList should be('a ->> 1 :: 'b ->> "b" :: HNil)

      rowProjected.get('a) should be(1)
      rowProjected.get('b) should be("b")

    }

    "select multiple fields from a nested row" in {

      val ncc = NestedCaseClass(1, "b", 1.0, NestedRecord(1))

      val row = Row(ncc)

      type v1 = Witness.`'a`.T :: Witness.`'n`.T :: HNil

      type v2 = HList.`'a, 'n`.T

      val rowProjected = row.projectTyped[v1]

      rowProjected.hList should be('a ->> 1 :: 'n ->> ('i ->> 1 :: HNil) :: HNil)

      rowProjected.get('a) should be(1)
      rowProjected.get('n).get('i) should be(1)

      illTyped("rowProjected.get('z)")
      illTyped("rowProjected.get('n).get('j)")

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

      rowProjected.hList should be('iii ->> 32423 :: 'l3 ->> ('iii ->> 32423 :: HNil) :: 'a ->> 123 :: HNil)

      rowProjected.get('iii) should be(32423)
      rowProjected.get('l3).get('iii) should be(32423)
      rowProjected.get('a) should be(123)

      illTyped("rowProjected.get('l3).get('iii).get('xx)")

    }

    "select nested fields from a row with simplified syntax" in {

      case class Level3Record(iii: Int)

      case class Level2Record(ii: Int, l3: Level3Record)

      case class Level1Record(l2: Level2Record, i: Int)

      case class NestedCaseClass(l1: Level1Record, a: Int, b: String, c: Double, iii: String)

      val ncc = NestedCaseClass(a = 123, b = "bbb", c = 1.23, iii = "t",
        l1 = Level1Record(l2 = Level2Record(ii = 2233, l3 = Level3Record(iii = 32423)), i = 3333))

      val row = Row(ncc)

      val selector = Nested(Witness('l1), Nested(Witness('l2), Nested(Witness('l3), Witness('iii)))) ::
        Nested(Witness('l1), Nested(Witness('l2), Witness('l3))) ::
        Witness('a) :: HNil

      val rowProjected = row.project(selector)

      rowProjected.hList should be('iii ->> 32423 :: 'l3 ->> ('iii ->> 32423 :: HNil) :: 'a ->> 123 :: HNil)

      rowProjected.get('iii) should be(32423)
      rowProjected.get('l3).get('iii) should be(32423)
      rowProjected.get('a) should be(123)

      val ambiguousSelector = Witness('a) :: Witness('a) :: HNil
      illTyped("row.project(ambiguousSelector)")

      val ambiguousSelector1 = Witness('iii) :: Nested(Witness('l1), Nested(Witness('l2), Nested(Witness('l3), Witness('iii)))) :: HNil
      illTyped("row.project(ambiguousSelector1)")
    }

    "select nested fields from a row with simplified syntax with overridden keywords" in {

      import Syntax._

      case class Level3Record(iii: Int)

      case class Level2Record(ii: Int, l3: Level3Record)

      case class Level1Record(l2: Level2Record, i: Int)

      case class NestedCaseClass(l1: Level1Record, a: Int, b: String, c: Double)

      val ncc = NestedCaseClass(a = 123, b = "bbb", c = 1.23,
        l1 = Level1Record(l2 = Level2Record(ii = 2233, l3 = Level3Record(iii = 32423)), i = 3333))

      val row = Row(ncc)

      val selector = projector(Col('l1) ->: (Col('l2) ->: (Col('l3) ->: Col('iii))),
        Col('l1) ->: (Col('l2) ->: Col('l3)),
        Col('a))

      val rowProjected = row.project(selector)

      rowProjected.hList should be('iii ->> 32423 :: 'l3 ->> ('iii ->> 32423 :: HNil) :: 'a ->> 123 :: HNil)

      rowProjected.get('iii) should be(32423)
      rowProjected.get('l3).get('iii) should be(32423)
      rowProjected.get('a) should be(123)

    }

    "select nested optional field and list from a row at the leaf node" in {

      import Syntax._

      case class Level1Record(i: Option[Int], ii: List[Int])

      case class NestedCaseClass(l1: Level1Record, a: Int, b: String, c: Double)

      val ncc = NestedCaseClass(a = 123, b = "bbb", c = 1.23,
        l1 = Level1Record(i = Some(3333), ii = List(1, 2, 3)))

      val row = Row(ncc)

      val selector = Witness('l1) ->: Witness('i) :: Witness('l1) ->: Witness('ii) :: HNil

      val rowProjected = row.project(selector)

      rowProjected.hList should be('i ->> 3333 :: 'ii ->> 1 :: HNil)

      rowProjected.get('i) should be(3333)
      rowProjected.get('ii) should be(1)

    }

    "select nested options and lists from a row that where options and lists are not at the leaf node" in {

      import Syntax._

      case class Level2ListRecord(l2s: String, l2i: Int)

      case class Level1Record(i: Int, ii: List[Int], l2: List[Level2ListRecord])

      case class NestedCaseClass(l1: Option[Level1Record], a: Int, b: String, c: Double)

      val ncc = NestedCaseClass(a = 123, b = "bbb", c = 1.23,
        l1 = Some(Level1Record(i = 3333, ii = List(1, 2, 3), l2 = List(Level2ListRecord("s1", 122), Level2ListRecord("s2", 122)))))

      val row = Row(ncc)

      val selector = Witness('l1) ->: Witness('i) :: Witness('l1) ->: (Witness('l2) ->: Witness('l2s)) :: Witness('a) :: HNil

      val rowProjected = row.project(selector)

      rowProjected.hList should be('i ->> 3333 :: 'l2s ->> "s1" :: 'a ->> 123 :: HNil)

      rowProjected.get('i) should be(3333)
      rowProjected.get('l2s) should be("s1")
      rowProjected.get('a) should be(123)

    }

    "select nested options and lists from a row that where options and lists are at the leaf node and above" in {

      import Syntax._

      case class Level2ListRecord(l2s: List[String], l2i: Option[Int])

      case class Level1Record(i: Int, ii: List[Int], l2: List[Level2ListRecord])

      case class NestedCaseClass(l1: Option[Level1Record], a: Int, b: String, c: Double)

      val ncc = NestedCaseClass(a = 123, b = "bbb", c = 1.23,
        l1 = Some(Level1Record(i = 3333, ii = List(1, 2, 3), l2 = List(
          Level2ListRecord(List("s1", "s2"), Some(122)),
          Level2ListRecord(List("s3", "s4"), Some(122))))))

      val row = Row(ncc)

      val selector = Witness('l1) ->: Witness('i) :: Witness('l1) ->: (Witness('l2) ->: Witness('l2s)) :: Witness('a) :: HNil

      val rowProjected = row.project(selector)

      rowProjected.hList should be('i ->> 3333 :: 'l2s ->> "s1" :: 'a ->> 123 :: HNil)

      rowProjected.get('i) should be(3333)
      rowProjected.get('l2s) should be("s1")
      rowProjected.get('a) should be(123)

    }

    "select fields from complex data structure" in {

      val wins = Nested(Witness('state), Nested(Witness('bets), Nested(Witness('payout), Witness('winnings))))
      val customerRef = Nested(Witness('state), Nested(Witness('bets), Witness('customerRef)))
      val activityId = Nested(Witness('header), Witness('activityId))

      val pr = HList(customerRef, wins, activityId)

      case class Payout(winnings: Option[String])
      case class Bet(id: String, customerRef: Option[String], payout: Option[Payout])
      case class BetState(bets: List[Bet])
      case class ActivityHeader(activityId: Long, timeStamp: Option[Long])
      case class Activity(header: ActivityHeader, state: Option[BetState])

      val activity = Activity(header = ActivityHeader(activityId = 12L, timeStamp = Some(1L)),
        state = Some(BetState(bets = List(Bet(id = "betid",
          customerRef = Some("custRef"),
          payout = Some(Payout(winnings = Some("1.2"))))))))

      val row = Row(activity)

      val rowProjected = row.project(pr)

      rowProjected.hList should be('customerRef ->> "custRef" :: 'winnings ->> "1.2" :: 'activityId ->> 12L :: HNil)

      rowProjected.get('customerRef) should be("custRef")
      rowProjected.get('winnings) should be("1.2")
      rowProjected.get('activityId) should be(12l)

    }

    "rename field at the leaf node" in {

      import Syntax._

      val wins = Nested(Witness('state), Nested(Witness('bets), Nested(Witness('payout), Rename(Witness('winnings), Witness('winnings2)))))

      val pr = projector(wins)

      case class Payout(winnings: Option[String])
      case class Bet(id: String, customerRef: Option[String], payout: Option[Payout])
      case class BetState(bets: List[Bet])
      case class ActivityHeader(activityId: Long, timeStamp: Option[Long])
      case class Activity(header: ActivityHeader, state: Option[BetState])

      val activity = Activity(header = ActivityHeader(activityId = 12L, timeStamp = Some(1L)),
        state = Some(BetState(bets = List(Bet(id = "betid",
          customerRef = Some("custRef"),
          payout = Some(Payout(winnings = Some("1.2"))))))))

      val row = Row(activity)

      val rowProjected = row.project(pr)

      rowProjected.get('winnings2) should be("1.2")

    }

    "rename field at the root" in {

      import Syntax._

      val header = Rename(Witness('header), Witness('header2))

      val pr = projector(header)

      case class Payout(winnings: Option[String])
      case class Bet(id: String, customerRef: Option[String], payout: Option[Payout])
      case class BetState(bets: List[Bet])
      case class ActivityHeader(activityId: Long, timeStamp: Option[Long])
      case class Activity(header: ActivityHeader, state: Option[BetState])

      val activity = Activity(header = ActivityHeader(activityId = 12L, timeStamp = Some(1L)),
        state = Some(BetState(bets = List(Bet(id = "betid",
          customerRef = Some("custRef"),
          payout = Some(Payout(winnings = Some("1.2"))))))))

      val row = Row(activity)

      val rowProjected = row.project(pr)

      rowProjected.get('header2).get('activityId) should be(12L)

    }

    "rename nested structure with simplified syntax" in {

      import Syntax._

      val wins = Col('state) ->: Col('bets) ->: Col('payout) ->: Col('winnings) ** Col('winnings2)
      val customerRef = Col('state) ->: Col('bets) ->: Col('customerRef)
      val activityId = Col('header) ->: Col('activityId)
      val header = Col('header) ** Col('header2)


      val pr = HList(customerRef, wins, activityId, header)

      case class Payout(winnings: Option[String])
      case class Bet(id: String, customerRef: Option[String], payout: Option[Payout])
      case class BetState(bets: List[Bet])
      case class ActivityHeader(activityId: Long, timeStamp: Option[Long])
      case class Activity(header: ActivityHeader, state: Option[BetState])

      val activity = Activity(header = ActivityHeader(activityId = 12L, timeStamp = Some(1L)),
        state = Some(BetState(bets = List(Bet(id = "betid",
          customerRef = Some("custRef"),
          payout = Some(Payout(winnings = Some("1.2"))))))))

      val row = Row(activity)

      val rowProjected = row.project(pr)

      rowProjected.get('activityId) should be(12L)
      rowProjected.get('winnings2) should be("1.2")
      rowProjected.get('header2).get('activityId) should be(12L)
      rowProjected.get('customerRef) should be("custRef")

    }

  }


  "Row converter" should {

    "convert a flat row back to a case class" in {

      case class FlatCaseClassAppended(a: Int, b: String, c: Double, d: Double)

      val fcc = FlatCaseClass(1, "b", 1.0)

      val row = Row(fcc)

      val rowAppended = row.append('d, 2.0)

      val fccAppended = Row.to[FlatCaseClassAppended].from(rowAppended.hList)

      fccAppended should be(FlatCaseClassAppended(1, "b", 1.0, 2.0))

    }

    "convert a nested row back to a case class" in {

      case class NestedCaseClassAppended(a: Int, b: String, c: Double, n: NestedRecord, m: Option[Long])

      val ncc = NestedCaseClass(1, "b", 1.0, NestedRecord(1))

      val row = Row(ncc)

      val rowAppended = row.append('m, Option(1L))

      val nccAppended = Row.to[NestedCaseClassAppended].from(rowAppended.hList)

      nccAppended should be(NestedCaseClassAppended(1, "b", 1.0, NestedRecord(1), Some(1L)))

    }

  }

}

