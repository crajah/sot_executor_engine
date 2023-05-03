package parallelai.sot.engine.generic.row

object Syntax {

  implicit class nested[A](a : A) {
    def ->>>[B](b : B) = Nested(a, b)
  }

  val Col = shapeless.Witness
  val Projector = shapeless.HList

}
