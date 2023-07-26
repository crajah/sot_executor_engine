package parallelai.sot.engine.io.datastore

import shapeless.{HList, LabelledGeneric}

case class Lookup[T] private(datastore: Datastore) extends Serializable {
  def get[L <: HList](id: String)(implicit gen: LabelledGeneric.Aux[T, L], fromL: FromEntity[L]): Option[T]  = {
    datastore.get[T](id)
  }

  def get[L <: HList](id: Int)(implicit gen: LabelledGeneric.Aux[T, L], fromL: FromEntity[L]): Option[T]  = {
    datastore.get[T](id)
  }
}