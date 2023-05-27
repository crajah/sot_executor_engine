package com.google.cloud.datastore

import grizzled.slf4j.Logging
import shapeless.{HList, LabelledGeneric}
import parallelai.sot.engine.io.datastore.{DatastoreType, FromEntity, ToEntity}

object EntityOps extends Logging {
  def toEntity[A, L <: HList](id: Long, a: A)(implicit keyFactory: KeyFactory, gen: LabelledGeneric.Aux[A, L], toL: ToEntity[L]): Entity =
    toEntity(keyFactory.newKey(id), a)

  def toEntity[A, L <: HList](id: String, a: A)(implicit keyFactory: KeyFactory, gen: LabelledGeneric.Aux[A, L], toL: ToEntity[L]): Entity =
    toEntity(keyFactory.newKey(id), a)

  def toEntity[A, L <: HList](key: Key, a: A)(implicit gen: LabelledGeneric.Aux[A, L], toL: ToEntity[L]): Entity = {
    val datastoreType: DatastoreType[A] = DatastoreType[A]
    val entityBuilder = datastoreType.toEntityBuilder(a).setKey(key.toPb)

    Entity fromPb entityBuilder.build()
  }

  def fromEntity[A, L <: HList](entity: Entity)(implicit gen: LabelledGeneric.Aux[A, L], fromL: FromEntity[L]): Option[A] = {
    val datastoreType: DatastoreType[A] = DatastoreType[A]

    datastoreType.fromEntity(entity.toPb)
  }
}