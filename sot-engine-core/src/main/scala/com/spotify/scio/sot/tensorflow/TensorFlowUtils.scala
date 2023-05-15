package com.spotify.scio.sot.tensorflow

import java.io.{File, IOException}
import java.util.UUID

import com.google.cloud.storage.{Blob, Storage, StorageOptions}
import com.google.cloud.storage.Storage.BlobListOption
import org.slf4j.LoggerFactory
import org.tensorflow.{Graph, SavedModelBundle, Session}

import scala.collection.JavaConverters._

object TensorFlowUtils {

  private def downloadModel(bucket: String, path: String): String = {
    @transient val log = LoggerFactory.getLogger(this.getClass)

    log.info("Setting up Storage")
    val storage = StorageOptions.getDefaultInstance.getService
    log.info("Downloading model")
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

  def tfModelLoader(modelBucket: String, modelPath: String) : SavedModelBundleSOT = {
    @transient val log = LoggerFactory.getLogger(this.getClass)

    val path = downloadModel(modelBucket, modelPath)
    log.info("Loading TensorFlow graph")
    val loadStart = System.currentTimeMillis
    try {
      val bundle = SavedModelBundle.load(path, "serve")
      log.info(s"TensorFlow graph loaded in ${System.currentTimeMillis - loadStart} ms")
      new SavedModelBundleSOT(bundle.graph(), bundle.session(), bundle.metaGraphDef())
    } catch {
      case e: Exception =>
        throw new IOException("Loading TensorFlow Model failed: " + e.getMessage)
    }
  }

  def tfModelLoaderFromGraph(graphBytes: Array[Byte]): SavedModelBundleSOT = {
    @transient val log = LoggerFactory.getLogger(this.getClass)

    log.info("Loading TensorFlow graph")
    val g = new Graph()
    val s = new Session(g)
    val loadStart = System.currentTimeMillis
    try {
      g.importGraphDef(graphBytes)
      log.info(s"TensorFlow graph loaded in ${System.currentTimeMillis - loadStart} ms")
      new SavedModelBundleSOT(g, s, graphBytes)
    } catch {
      case e: IllegalArgumentException =>
        throw new IOException("Not a valid TensorFlow Graph serialization: " + e.getMessage)
    }
  }

}
