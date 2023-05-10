package parallelai.sot.engine.tensorflow

import java.nio.file.Files

import com.spotify.scio.ContextAndArgs
import com.spotify.scio.testing.{DistCacheIO, PipelineSpec, TextIO}
import org.tensorflow._
import com.spotify.scio.tensorflow._

private object TFJob {
  def main(argv: Array[String]): Unit = {
    val (sc, args) = ContextAndArgs(argv)
    sc.parallelize(1L to 10)
      .predict(args("graphURI"), Seq("multiply"))
      {e => Map("input" -> Tensor.create(e))}
      {o => o.map{case (_, t) => t.longValue()}.head}
      .saveAsTextFile(args("output"))
    sc.close()
  }
}

private object TFJob2Inputs {
  def main(argv: Array[String]): Unit = {
    val (sc, args) = ContextAndArgs(argv)
    sc.parallelize(1L to 10)
      .predict(args("graphURI"), Seq("multiply"))
      {e => Map("input" -> Tensor.create(e),
        "input2" -> Tensor.create(3L))}
      {o => o.map{case (_, t) => t.longValue()}.head}
      .saveAsTextFile(args("output"))
    sc.close()
  }
}

private object TFJobLinearModel {
  def main(argv: Array[String]): Unit = {
    val (sc, args) = ContextAndArgs(argv)
    sc.parallelize(1L to 10)
      .predict(args("graphURI"), Seq("predict"))
      {e => Map("input" -> Tensor.create(e.toFloat))}
      {o => o.map{case (_, t) => t.floatValue()}.head}
      .saveAsTextFile(args("output"))
    sc.close()
  }
}

class TensorflowSpec extends PipelineSpec {

  private def createHelloWorldGraph = {
    val const = "MyConst"
    val graph = new Graph()
    val helloworld = s"Hello from ${TensorFlow.version()}"
    val t = Tensor.create(helloworld.getBytes("UTF-8"))
    graph.opBuilder("Const", const).setAttr("dtype", t.dataType()).setAttr("value", t).build()
    (graph, const, helloworld)
  }

  "Tensorflow" should "work for hello-wold" in {
    val (graph, const, helloworld) = createHelloWorldGraph
    val session = new Session(graph)
    val r = session.runner().fetch(const).run().get(0)
    new String(r.bytesValue()) should be (helloworld)
  }

  it should "work for serde model" in {
    val (graph, const, helloworld) = createHelloWorldGraph
    val graphFile = Files.createTempFile("tf-grap", ".bin")
    Files.write(graphFile, graph.toGraphDef)
    val newGraph = new Graph()
    newGraph.importGraphDef(Files.readAllBytes(graphFile))
    val session = new Session(graph)
    val r = session.runner().fetch(const).run().get(0)
    new String(r.bytesValue()) should be (helloworld)
  }

  it should "allow to predict" in {
    val g = new Graph()
    val t3 = Tensor.create(3L)
    val input = g.opBuilder("Placeholder", "input").setAttr("dtype", t3.dataType).build.output(0)
    val c3 = g.opBuilder("Const", "c3")
      .setAttr("dtype", t3.dataType)
      .setAttr("value", t3).build.output(0)
    g.opBuilder("Mul", "multiply").addInput(c3).addInput(input).build()


  }

  it should "allow to predict with 2 inputs" in {
    val g = new Graph()
    val input = g.opBuilder("Placeholder", "input")
      .setAttr("dtype", DataType.INT64).build.output(0)
    val input2 = g.opBuilder("Placeholder", "input2")
      .setAttr("dtype", DataType.INT64).build.output(0)
    g.opBuilder("Mul", "multiply").addInput(input2).addInput(input).build()
    JobTest[TFJob2Inputs.type]
      .distCache(DistCacheIO[Array[Byte]]("tf-graph.bin"), g.toGraphDef)
      .args("--graphURI=tf-graph.bin", "--output=output")
      .output(TextIO("output"))(_ should containInAnyOrder((1L to 10).map(_ * 3).map(_.toString)))
      .run()
  }

  it should "allow to predict a linear model" in {
    val g = new Graph()

    val input = g.opBuilder("Placeholder", "input")
      .setAttr("dtype", DataType.FLOAT).build.output(0)

    val w1 = Tensor.create(1.5f)
    val weight = g.opBuilder("Const", "weight")
      .setAttr("dtype", w1.dataType())
      .setAttr("value", w1).build().output(0)

    val b1 = Tensor.create(2.0f)
    val bias = g.opBuilder("Const", "bias")
      .setAttr("dtype", b1.dataType())
      .setAttr("value", b1).build().output(0)

    val mul = g.opBuilder("Mul", "multiply").addInput(input).addInput(weight).build().output(0)

    g.opBuilder("Add", "predict").addInput(mul).addInput(bias).build()

    JobTest[TFJobLinearModel.type]
      .distCache(DistCacheIO[Array[Byte]]("tf-graph.bin"), g.toGraphDef)
      .args("--graphURI=tf-graph.bin", "--output=output")
      .output(TextIO("output"))(_ should containInAnyOrder((1 to 10).map(a => (a.toFloat * 1.5f) + 2.0f).map(_.toString)))
      .run()

  }

}
