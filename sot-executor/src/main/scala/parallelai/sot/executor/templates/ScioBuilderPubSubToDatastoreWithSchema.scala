package parallelai.sot.executor.templates

import com.google.cloud.datastore.DatastoreDao
import com.google.datastore.v1.Query
import com.google.datastore.v1.client.DatastoreHelper.makeKey
import com.google.datastore.v1.client.{DatastoreFactory, DatastoreHelper, DatastoreOptions}
import com.spotify.scio._
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.streaming.ACCUMULATING_FIRED_PANES
import com.spotify.scio.values.{SCollection, WindowOptions}
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import org.apache.beam.sdk.transforms.windowing.{AfterProcessingTime, Repeatedly}
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import parallelai.sot.executor.common.{SOTOptions, SOTUtils}
import parallelai.sot.executor.datastore._
import parallelai.sot.executor.utils.AvroUtils
import shapeless.{::, HList, Witness, _}
import labelled._

import scala.reflect.ClassTag

class ScioBuilderPubSubToDatastoreWithSchema[In <: HasAvroAnnotation : Manifest, Out <: Product]
(transform: SCollection[In] => SCollection[Out], inArgs: PubSubArgs, outArgs: DatastoreArgs, keyBuilder: Out => Either[String, Int])
  extends Serializable {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val avroT = AvroType[In]

  def dataStoreT: DatastoreType[Out] = DatastoreType[Out]

  def execute[Y <: HList](opts: SOTOptions, args: Args, exampleUtils: SOTUtils, sc: ScioContext)
                                                                        (implicit
                                                                         gen: LabelledGeneric.Aux[Out, Y],
                                                                         toL: ToEntity[Y],
                                                                         fromL: FromEntity[Y]) = {

    val allowedLateness = Duration.standardMinutes(args.int("allowedLateness", 120))
    val projectId = opts.as(classOf[GcpOptions]).getProject

    val schema = avroT.schema
    val fromGenericRecord = avroT.fromGenericRecord

    val schemaString = schema.toString

    val scIn = sc.pubsubTopic[Array[Byte]](s"projects/${projectId}/topics/${inArgs.topic}", timestampAttribute = "timestamp_ms")
      .map(f => fromGenericRecord(AvroUtils.decodeAvro(f, schemaString)))

    val in = scIn
      .withGlobalWindow(WindowOptions(
        trigger = Repeatedly.forever(AfterProcessingTime.pastFirstElementInPane()
          .plusDelayOf(Duration.standardSeconds(30))),
        accumulationMode = ACCUMULATING_FIRED_PANES,
        allowedLateness = allowedLateness)
      )


    val xx = transform(in).map{
      rec =>
        val datastoreDAO = new DatastoreDao[Out](outArgs.kind, projectId)
        val keyValue = keyBuilder(rec)
        val res = datastoreDAO.getRec(keyValue)(gen, fromL)
        logger.info("looking up key: " + keyValue + " results: " + res.toString)
    }

    val outTest = transform(in).map { rec =>
      val genList: Y = gen.to(rec)
      val entity = dataStoreT.toEntityBuilder(genList)
      val keyValue = keyBuilder(rec)
      val keyEntity = keyValue match {
        case Left(name) => makeKey(outArgs.kind, name.asInstanceOf[AnyRef])
        case Right(id) => makeKey(outArgs.kind, id.asInstanceOf[AnyRef])
      }
      entity.setKey(keyEntity)
      entity.build()
    }.saveAsDatastore(projectId)

    val result = sc.close()
    exampleUtils.waitToFinish(result.internal, true)

  }

}
