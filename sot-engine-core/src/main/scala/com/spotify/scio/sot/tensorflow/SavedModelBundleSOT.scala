package com.spotify.scio.sot.tensorflow

import org.tensorflow.{Graph, SavedModelBundle, Session}

class SavedModelBundleSOT(val graph: Graph, val session: Session, val metaGraphDef: Array[Byte]) {

  def this(bundle: SavedModelBundle) = {
    this(bundle.graph(), bundle.session(), bundle.metaGraphDef())
  }

}
