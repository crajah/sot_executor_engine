package com.spotify.scio.sot.tensorflow

import java.io.{File, FileOutputStream, IOException}
import java.nio.file.Files
import java.util.UUID
import javax.annotation.Nullable

import com.spotify.scio.values.{DistCache, SCollection}
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, Setup, Teardown}
import org.apache.beam.sdk.{io => gio}
import org.slf4j.LoggerFactory
import org.tensorflow._

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

private class PredictDoFn[T, V](fetchOp: Seq[String],
                                inFn: T => Map[String, Tensor],
                                outFn: (T, Map[String, Tensor]) => V,
                                modelLoader: => SavedModelBundleSOT
                               ) extends DoFn[T, V] {

  @transient private lazy val log = LoggerFactory.getLogger(this.getClass)
  @transient private var g: Graph = _
  @transient private var s: Session = _

  @Setup
  def setup(): Unit = {
    val bundle = modelLoader
    g = bundle.graph
    s = bundle.session
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
    * @param modelPath   path to the folder that contains the tensorflow model
    * @param fetchOps    names of [[org.tensorflow.Operation]]s to fetch the results from
    * @param inFn        translates input elements of T to map of input-operation ->
    *                    [[org.tensorflow.Tensor Tensor]]. This method takes ownership of the
    *                    [[org.tensorflow.Tensor Tensor]]s.
    * @param outFn       translates output of prediction from map of output-operation ->
    *                    [[org.tensorflow.Tensor Tensor]], to elements of V. This method takes ownership
    *                    of the [[org.tensorflow.Tensor Tensor]]s.
    */
  def predict[V: ClassTag](modelBucket: String,
                           modelPath: String,
                           fetchOps: Seq[String])
                          (inFn: T => Map[String, Tensor])
                          (outFn: (T, Map[String, Tensor]) => V): SCollection[V] = {
    lazy val modelLoader = TensorFlowUtils.tfModelLoader(modelBucket, modelPath)
    self.parDo(new PredictDoFn[T, V](fetchOps, inFn, outFn, modelLoader))
  }

  def predictFromBundle[V: ClassTag](graphUri: String,
                                     fetchOps: Seq[String])
                                    (inFn: T => Map[String, Tensor])
                                    (outFn: (T, Map[String, Tensor]) => V): SCollection[V] = {
    val graphBytes = self.context.distCache(graphUri)(f => Files.readAllBytes(f.toPath))
    lazy val modelLoader = TensorFlowUtils.tfModelLoaderFromGraph(graphBytes())
    self.parDo(new PredictDoFn[T, V](fetchOps, inFn, outFn, modelLoader))
  }


}

