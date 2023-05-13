package com.spotify.scio.sot.tensorflow

import java.io.{File, FileOutputStream, IOException}
import java.nio.file.Files
import java.util.UUID
import javax.annotation.Nullable

import com.google.cloud.ReadChannel
import com.google.cloud.storage.Storage.BlobListOption
import com.google.cloud.storage.{Blob, Storage, StorageOptions}
import com.spotify.scio.values.{DistCache, SCollection}
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, Setup, Teardown}
import org.apache.beam.sdk.{io => gio}
import org.slf4j.LoggerFactory
import org.tensorflow._

import scala.collection.JavaConverters._

import scala.reflect.ClassTag

private class PredictDoFn[T, V](modelBucket: String, modelPath: String,
                                fetchOp: Seq[String],
                                inFn: T => Map[String, Tensor],
                                outFn: (T, Map[String, Tensor]) => V) extends DoFn[T, V] {

  @transient private var storage: Storage = _

  @transient private lazy val log = LoggerFactory.getLogger(this.getClass)
  @transient private var g: Graph = _
  @transient private var s: Session = _

  private def downloadModel(bucket: String, path: String): String = {
    val s = storage.list(bucket, BlobListOption.prefix(path))
    val basePath = s"/tmp/tf-models/${UUID.randomUUID.toString}/"
    for (blob <- s.getValues.asScala) {
      if (blob.getSize > 0) {
        val file = new File(s"${basePath}${blob.getName}")
        file.getParentFile.mkdirs()
        blob.downloadTo(file.toPath)
      }
    }
    s"${basePath}${path}"
  }

  @Setup
  def setup(): Unit = {
    log.info("Setting up Storage")
    storage = StorageOptions.getDefaultInstance.getService
    log.info("Downloading model")
    val path = downloadModel(modelBucket, modelPath)
    log.info("Loading TensorFlow graph")
    val loadStart = System.currentTimeMillis
    try {
      val bundle = SavedModelBundle.load(path, "serve")
      g = bundle.graph()
      s = bundle.session()
      log.info(s"TensorFlow graph loaded in ${System.currentTimeMillis - loadStart} ms")
    } catch {
      case e: Exception =>
        throw new IOException("Not a valid TensorFlow Graph serialization: " + e.getMessage)
    }
  }

  @ProcessElement
  def process(c: DoFn[T, V]#ProcessContext): Unit = {
    val runner = s.runner()
    import scala.collection.JavaConverters._
    val e: T = c.element()
    val i = inFn(e)
    try {
      i.foreach { case (op, t) => runner.feed(op, t) }
      fetchOp.foreach(runner.fetch)
      val outTensors = runner.run()
      try {
        import scala.collection.breakOut
        val outRes: Map[String, org.tensorflow.Tensor] = (fetchOp zip outTensors.asScala) (breakOut)
        c.output(outFn(e, outRes))
      } finally {
        log.debug("Closing down output tensors")
        outTensors.asScala.foreach(_.close())
      }
    } finally {
      log.debug("Closing down input tensors")
      i.foreach { case (_, t) => t.close() }
    }
  }

  @Teardown
  def teardown(): Unit = {
    log.info(s"Closing down predict DoFn $this")
    try {
      if (s != null) {
        log.info(s"Closing down TensorFlow session $s")
        s.close()
        s = null
      }
    } finally {
      if (g != null) {
        log.info(s"Closing down TensorFlow graph $s")
        g.close()
        g = null
      }
    }
  }
}

/**
  * Enhanced version of [[com.spotify.scio.values.SCollection SCollection]] with TensorFlow methods.
  */
class TensorFlowSCollectionFunctions[T: ClassTag](@transient val self: SCollection[T])
  extends Serializable {

  /**
    * Predict/infer/forward-pass on pre-trained GraphDef.
    *
    * @param modelBucket Cloud Storage bucket that contains the tensorflow model
    * @param path        path to the folder that contains the tensorflow model
    * @param fetchOps    names of [[org.tensorflow.Operation]]s to fetch the results from
    * @param inFn        translates input elements of T to map of input-operation ->
    *                    [[org.tensorflow.Tensor Tensor]]. This method takes ownership of the
    *                    [[org.tensorflow.Tensor Tensor]]s.
    * @param outFn       translates output of prediction from map of output-operation ->
    *                    [[org.tensorflow.Tensor Tensor]], to elements of V. This method takes ownership
    *                    of the [[org.tensorflow.Tensor Tensor]]s.
    */
  def predict[V: ClassTag](modelBucket: String,
                           path: String,
                           fetchOps: Seq[String])
                          (inFn: T => Map[String, Tensor])
                          (outFn: (T, Map[String, Tensor]) => V): SCollection[V] = {
    self.parDo(new PredictDoFn[T, V](modelBucket, path, fetchOps, inFn, outFn))
  }
}

