package parallelai.sot.executor.builder

import java.lang
import scala.collection.JavaConverters._
import monocle.Lens
import monocle.macros.GenLens
import shapeless._
import org.apache.beam.sdk.coders.{SerializableCoder, StringUtf8Coder}
import org.apache.beam.sdk.testing.PAssert
import org.apache.beam.sdk.transforms.{Count, Create}
import org.apache.beam.sdk.values.{KV, PCollection}
import org.scalatest.{MustMatchers, WordSpec}
import com.spotify.scio.ScioContext
import com.spotify.scio.values.SCollection
import parallelai.sot.containers.{Container, ForAllContainersFixture}
import parallelai.sot.engine.ProjectFixture
import parallelai.sot.engine.generic.row.Row
import parallelai.sot.engine.io.datastore._
import parallelai.sot.engine.scio.PipelineSpec
import parallelai.sot.executor.builder.FirstMessage._

class DatastoreITSpec extends WordSpec with MustMatchers with PipelineSpec with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture {
  override val container: Container = datastoreContainer

  "Datastore SOT Builder" should {
    "utilise Beam directly" in pipe { pipeline =>
      val words = Seq("hi", "there", "hi", "hi", "sue", "bob", "hi", "sue", "", "", "ZOW", "bob", "")

      val input: PCollection[String] = pipeline.apply(Create.of(words.asJava)).setCoder(StringUtf8Coder.of())

      val output: PCollection[KV[String, lang.Long]] = input.apply(Count.perElement())

      PAssert.that(output)
        .containsInAnyOrder(
          KV.of("hi", 4L),
          KV.of("there", 1L),
          KV.of("sue", 2L),
          KV.of("bob", 2L),
          KV.of("", 3L),
          KV.of("ZOW", 1L))
    }

    "utilise Scio and transform data via aggregation" in pipe { pipeline =>
      val words = Seq("hi", "there", "hi", "hi", "sue", "bob", "hi", "sue", "", "", "ZOW", "bob", "")

      val inputP: PCollection[String] = pipeline.apply(Create.of(words.asJava)).setCoder(StringUtf8Coder.of())
      val input = ScioContext().wrap(inputP)

      val output: SCollection[(String, Long)] = input.countByValue

      output must containsInAnyOrder(("hi", 4L), ("there", 1L), ("sue", 2L), ("bob", 2L), ("", 3L), ("ZOW", 1L))
    }

    "utilise Scio and transform data via map" in pipe { pipeline =>
      val words = Seq("hi", "there", "hi", "hi", "sue", "bob", "hi", "sue", "", "", "ZOW", "bob", "")

      val inputP: PCollection[String] = pipeline.apply(Create.of(words.asJava)).setCoder(StringUtf8Coder.of())
      val input = ScioContext().wrap(inputP)

      val output: SCollection[String] = input.map(_.toUpperCase)

      output must containInAnyOrder(words.map(_.toUpperCase))
    }

    "utilise Scio and transform custom data via map" in pipe { pipeline =>
      val message = FirstMessage("user", "teamName", score = 3)

      val inputP: PCollection[FirstMessage] = pipeline.apply(Create.of(message)).setCoder(SerializableCoder.of(classOf[FirstMessage]))
      val input = ScioContext().wrap(inputP)

      val output: SCollection[FirstMessage] = input map score.set(5)

      output must containSingleValue(score.set(5)(message))
    }

    "utilise Scio and transform custom data as a Row via map" in pipe { pipeline =>
      val message = FirstMessage("user", "teamName", score = 3)

      val inputP: PCollection[FirstMessage] = pipeline.apply(Create.of(message)).setCoder(SerializableCoder.of(classOf[FirstMessage]))
      val input = ScioContext().wrap(inputP)

      val output: SCollection[Row] = input map(Row(_))

      output map { r =>
        r.hList mustEqual Row(message).hList
      }

      output map { r =>
        r.hList mustEqual "user" :: "teamName" :: 3 :: HNil
      }
    }

    "utilise Scio and transform custom data as a Row via map including 'step' transformations" in pipe { pipeline =>
      val message = FirstMessage("user", "teamName", score = 3)

      val inputP: PCollection[FirstMessage] = pipeline.apply(Create.of(message)).setCoder(SerializableCoder.of(classOf[FirstMessage]))
      val input = ScioContext().wrap(inputP)

      val output: SCollection[Row] = input map { m =>
        val row = Row(m)
        row.update('score, 5)
      }

      output map { r =>
        r.hList mustEqual "user" :: "teamName" :: 5 :: HNil
      }
    }

    "utilise Scio and transform custom data as a Row via map including 'Datastore step' transformations" in pipe { pipeline =>
      val message = FirstMessage("user", "teamName", score = 3)

      val inputP: PCollection[FirstMessage] = pipeline.apply(Create.of(message)).setCoder(SerializableCoder.of(classOf[FirstMessage]))
      val input = ScioContext().wrap(inputP)

      datastore.put("blah", message)

      val output: SCollection[Row] = input.map { m =>
        println(s"===> You gave me: $datastore") // TODO - WIP
        val row = Row(m)
        row.update('score, 5)
      }

      output map { r =>
        r.hList mustEqual "user" :: "teamName" :: 5 :: HNil
      }
    }

    "utilise Scio and transform custom data as a Row via map and map including 'Datastore step' transformations" in pipe { pipeline =>
      val message = FirstMessage("user", "teamName", score = 3)

      val inputP: PCollection[FirstMessage] = pipeline.apply(Create.of(message)).setCoder(SerializableCoder.of(classOf[FirstMessage]))
      val input = ScioContext().wrap(inputP)

      datastore.put("blah", message)

      val output: SCollection[Row] = input map { m =>
        (m, datastore)
      } map { case (m, ds) =>
        println(s"===> You gave me: $ds") // TODO - WIP
        val row = Row(m)
        row.update('score, 5)
      }

      output map { r =>
        r.hList mustEqual "user" :: "teamName" :: 5 :: HNil
      }
    }
  }
}

case class FirstMessage(user: String, teamName: String, score: Int)

case class SecondMessage(user: String, teamName: String, score: Int, count: Int)

object FirstMessage {
  implicit val messageGen = LabelledGeneric[FirstMessage]

  val score: Lens[FirstMessage, Int] = GenLens[FirstMessage](_.score)
}