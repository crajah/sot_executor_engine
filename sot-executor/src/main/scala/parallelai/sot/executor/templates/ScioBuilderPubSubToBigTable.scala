package parallelai.sot.executor.templates

import com.google.bigtable.v2.Mutation
import com.google.protobuf.ByteString
import com.spotify.scio.bigtable._
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.streaming.ACCUMULATING_FIRED_PANES
import com.spotify.scio.values.{SCollection, WindowOptions}
import com.spotify.scio.{Args, ScioContext}
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import org.apache.beam.sdk.transforms.windowing.{AfterProcessingTime, Repeatedly}
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import parallelai.sot.executor.bigtable.MapMutation
import parallelai.sot.executor.common.{SOTOptions, SOTUtils}
import parallelai.sot.executor.utils.AvroUtils
import shapeless.ops.hlist.{Mapper, ToTraversable}
import shapeless.{::, HList, Poly1}

import scala.collection.generic.CanBuildFrom

//Generic record assuming the following structure
//key :: col1 :: col2 ... colN :: HNil
//where col is Tuple3[String, String, Any] = (cf, colName, value)
class BigTableRecord[H <: HList](val hs: H)

object BigTableRecord {

  import shapeless.Generic

  def apply[P <: Product, L <: HList](p: P)(implicit gen: Generic.Aux[P, L]) = new BigTableRecord[L](gen.to(p))

}

class ScioBuilderPubSubToBigTable[In <: HasAvroAnnotation : Manifest, K <: Any, V <: HList]
(transform: SCollection[In] => SCollection[BigTableRecord[K :: V]], inArgs: PubSubArgs, outArgs: BigTableArgs)
  extends Serializable {

  private val logger = LoggerFactory.getLogger(this.getClass)

  lazy val avroT = AvroType[In]

  def execute[L <: HList](opts: SOTOptions, args: Args, exampleUtils: SOTUtils, sc: ScioContext)
                         (implicit ev1: Mapper.Aux[MapMutation.type, V, L],
                          ev2: ToTraversable.Aux[L, List, Mutation],
                          ev3: CanBuildFrom[List[Mutation], Mutation, List[Mutation]]) = {

    def toMutation(key: K, value: V): (ByteString, Iterable[Mutation]) = {
      val m = value.map(MapMutation).toList
      (ByteString.copyFromUtf8(key.toString), m)
    }

    val allowedLateness = Duration.standardMinutes(args.int("allowedLateness", 120))
    val project = opts.as(classOf[GcpOptions]).getProject

    val schema = avroT.schema
    val fromGenericRecord = avroT.fromGenericRecord
    val schemaString = schema.toString

    // Ensure that destination tables and column families exist
    sc.ensureTables(project, outArgs.instanceId, Map(
      outArgs.tableId -> outArgs.familyName
    ))

    val scIn = sc.pubsubTopic[Array[Byte]](s"projects/${project}/topics/${inArgs.topic}", timestampAttribute = "timestamp_ms")
      .map(f => fromGenericRecord(AvroUtils.decodeAvro(f, schemaString)))

    val windowing = scIn
      .withGlobalWindow(WindowOptions(
        trigger = Repeatedly.forever(AfterProcessingTime.pastFirstElementInPane()
          .plusDelayOf(Duration.standardSeconds(30))),
        accumulationMode = ACCUMULATING_FIRED_PANES,
        allowedLateness = allowedLateness)
      )

    val out = transform(windowing)

    //Note: this bit to work listSerializableCanBuildFrom should be in scope
    //so that it can provide a serializable version for ev3
    out.map { h =>
      val key :: value = h.hs
      toMutation(key, value)
    }.saveAsBigtable(project, outArgs.instanceId, outArgs.tableId)

    val result = sc.close()
    exampleUtils.waitToFinish(result.internal, true)

    // Bring down the number of nodes after the job ends.
    // There is no need to wait after bumping the nodes down.
    sc.updateNumberOfBigtableNodes(project, outArgs.instanceId, outArgs.numNodes, Duration.ZERO)
  }

}
