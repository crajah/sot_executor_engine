package com.google.cloud.datastore

import com.google.cloud.datastore.{Entity => CEntity}
import com.google.datastore.v1.{Entity => DEntity}
import parallelai.sot.engine.io.datastore.{DatastoreType, FromEntity, ToEntity}
import shapeless.{HList, LabelledGeneric}
import scala.collection.JavaConverters._

class DatastoreDao[A](kind: String, projectId: String) extends Serializable {

  lazy val datastore = DatastoreOptions.getDefaultInstance().getService()
  lazy val keyFactory = datastore.newKeyFactory().setKind(kind)

  def dataStoreT: DatastoreType[A] = DatastoreType[A]


  def insertRec[L <: HList](key: Either[String, Int], rec: A)(implicit
                                                              gen: LabelledGeneric.Aux[A, L],
                                                              toL: ToEntity[L]): CEntity = {
    val e = key match {
      case Left(id) => createEntity(id, rec)
      case Right(name) => createEntity(name, rec)
    }
    val cEntity = CEntity.fromPb(e)
    datastore.put(cEntity)
  }

  def insertRec[L <: HList](rec: A)(implicit
                                    gen: LabelledGeneric.Aux[A, L],
                                    toL: ToEntity[L]): CEntity = {
    val e = createEntity(rec)
    val cEntity = CEntity.fromPb(e)
    datastore.put(cEntity)
  }

  def updateRec[L <: HList](key: Either[String, Int], rec: A)(implicit
                                                              gen: LabelledGeneric.Aux[A, L],
                                                              toL: ToEntity[L]) = {
    val e = key match {
      case Left(id) => createEntity(id, rec)
      case Right(name) => createEntity(name, rec)
    }
    val cEntity = CEntity.fromPb(e)
    datastore.update(cEntity)
  }

  def deleteRec(key: Either[String, Int]) = {
    val k: Key = key match {
      case Left(id) => keyFactory.newKey(id)
      case Right(name) => keyFactory.newKey(name)
    }
    datastore.delete(k)
  }

  def getRec[L <: HList](key: Either[String, Int])(implicit
                                                   gen: LabelledGeneric.Aux[A, L],
                                                   fromL: FromEntity[L]): Option[A] = {
    val k = key match {
      case Left(id) => keyFactory.newKey(id)
      case Right(name) => keyFactory.newKey(name)
    }
    val res = datastore.get(k, ReadOption.eventualConsistency())
    if (res == null) {
      None
    } else {
      dataStoreT.fromEntity(res.toPb)
    }
  }

  def listRecs[L <: HList](startCursorString: Option[String],
                           lm: Int = 10)(implicit
                                         gen: LabelledGeneric.Aux[A, L],
                                         fromL: FromEntity[L]): (Iterator[A], Option[String]) = {

    val startCursor: Cursor = if (startCursorString.isDefined) {
      Cursor.fromUrlSafe(startCursorString.get) // Where we left off
    } else null

    val query: Query[Entity] = Query.newEntityQueryBuilder().
      setKind(kind).
      setLimit(lm).
      setStartCursor(startCursor).
      build()

    val resultList = datastore.run(query, ReadOption.eventualConsistency())

    val resultRecs = resultList.asScala.flatMap {
      e => dataStoreT.fromEntity(e.toPb)
    }

    val cursor: Cursor = resultList.getCursorAfter() // Where to start next time
    if (cursor != null && resultRecs.length == lm) { // Are we paging? Save Cursor
      val cursorString = cursor.toUrlSafe() // Cursors are WebSafe
      (resultRecs, Some(cursorString))
    } else {
      (resultRecs, None)
    }
  }

  private def createEntity[L <: HList](name: String, rec: A)(implicit
                                                             gen: LabelledGeneric.Aux[A, L],
                                                             toL: ToEntity[L]): DEntity = {
    val entity = dataStoreT.toEntityBuilder(rec)
    val key = keyFactory.newKey(name)
    entity.setKey(key.toPb)
    entity.build()
  }

  private def createEntity[L <: HList](id: Int, rec: A)(implicit
                                                        gen: LabelledGeneric.Aux[A, L],
                                                        toL: ToEntity[L]): DEntity = {
    val entity = dataStoreT.toEntityBuilder(rec)
    val key = keyFactory.newKey(id)
    entity.setKey(key.toPb)
    entity.build()
  }

  private def createEntity[L <: HList](rec: A)(implicit
                                               gen: LabelledGeneric.Aux[A, L],
                                               toL: ToEntity[L]): DEntity = {
    val entity = dataStoreT.toEntityBuilder(rec)
    val key = keyFactory.newKey()
    entity.setKey(key.toPb)
    entity.build()
  }

}
