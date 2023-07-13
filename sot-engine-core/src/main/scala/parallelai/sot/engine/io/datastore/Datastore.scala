package parallelai.sot.engine.io.datastore

import java.net.URI

import scalaz.Scalaz._
import scalaz._
import shapeless.{HList, LabelledGeneric}
import com.google.api.gax.retrying.RetrySettings
import com.google.auth.Credentials
import com.google.cloud.datastore.EntityOps._
import com.google.cloud.datastore.{Query, ReadOption, _}
import parallelai.sot.engine.Project
import com.google.cloud.datastore.Key
import parallelai.sot.engine.generic.row.Row
import org.slf4j.LoggerFactory

class Datastore private(project: Project,
                        kind: Kind,
                        host: Option[URI] = None,
                        credentials: Option[Credentials] = None,
                        retry: Option[RetrySettings] = None) extends Serializable {


  @transient val log = LoggerFactory.getLogger(this.getClass)

  @transient
  implicit protected lazy val datastoreOptions: DatastoreOptions = createDatastoreOptions

  @transient
  implicit protected lazy val datastore: com.google.cloud.datastore.Datastore = datastoreOptions.getService

  @transient
  implicit lazy val keyFactory: KeyFactory = datastore.newKeyFactory().setKind(kind.value)

  //Puts in entities (e.g if the entity exists it overwrites it)
  def put[A, L <: HList](id: Long, a: A)(implicit gen: LabelledGeneric.Aux[A, L], toL: ToEntity[L]): Entity =
    datastore.put(toEntity(id, a))

  def put[A, L <: HList](id: String, a: A)(implicit gen: LabelledGeneric.Aux[A, L], toL: ToEntity[L]): Entity =
    datastore.put(toEntity(id, a))

  def put[L <: HList](id: Long, a: Row.Aux[L])(implicit toL: ToEntity[L]): Entity =
    datastore.put(toEntityHList(id, a.hList))

  def put[L <: HList](id: String, a: Row.Aux[L])(implicit toL: ToEntity[L]): Entity =
    datastore.put(toEntityHList(id, a.hList))

  def put(entity: Entity): Entity =
    datastore.put(entity)

  //Updates entities (e.g. if the entity does not exists it throws an exception)
  def update[A, L <: HList](id: Long, a: A)(implicit gen: LabelledGeneric.Aux[A, L], toL: ToEntity[L]): Unit =
    datastore.update(toEntity(id, a))

  def update[A, L <: HList](id: String, a: A)(implicit gen: LabelledGeneric.Aux[A, L], toL: ToEntity[L]): Unit =
    datastore.update(toEntity(id, a))

  def update(entity: Entity): Unit =
    datastore.update(entity)

  //Adds entities (e.g. if the key already exists it throws an exception)
  def add[A, L <: HList](id: Long, a: A)(implicit gen: LabelledGeneric.Aux[A, L], toL: ToEntity[L]): Unit =
    datastore.add(toEntity(id, a))

  def add[A, L <: HList](id: String, a: A)(implicit gen: LabelledGeneric.Aux[A, L], toL: ToEntity[L]): Unit =
    datastore.add(toEntity(id, a))

  def add(entity: Entity): Unit =
    datastore.add(entity)

  def run[T](query: Query[T], options: Seq[ReadOption] = Seq(ReadOption.eventualConsistency())): QueryResults[T] =
    datastore.run(query, options: _*)

  def delete(keys: Key*): Unit =
    datastore.delete(keys: _*)

  //NOTE: incrementField operation is not reliable as the Runner might execute intermediate steps multiple times
  def incrementField[L](id: Long, field: String, value: L)(implicit
                                                           numeric: Numeric[L],
                                                           entityWrapper: EntityWrapper[L]): Unit = {
    val key = keyFactory.newKey(id)
    retry(5)(key, transaction(field, value, _: Key))

  }

  //NOTE: incrementField operation is not reliable as the Runner might execute intermediate steps multiple times
  def incrementField[L](id: String, field: String, value: L)(implicit
                                                             numeric: Numeric[L],
                                                             entityWrapper: EntityWrapper[L]): Unit = {
    val key = keyFactory.newKey(id)
    retry(5)(key, transaction(field, value, _: Key))
  }

  def retry[T](n: Int)(key: Key, fn: Key => T): T = {
    try {
      fn(key)
    } catch {
      case e: DatastoreException =>
        if (n > 1) retry(n - 1)(key, fn)
        else {
          log.error(s"Retry exceeded for $key")
          throw e
        }
    }
  }

  private def transaction[L](field: String, value: L, id: Key)(implicit
                                                               numeric: Numeric[L],
                                                               entityWrapper: EntityWrapper[L]) = {
    val txn = datastore.newTransaction()
    try {
      val entity = Option(txn.get(id))
      val addedValue: L = numeric.plus(entityWrapper.get(entity, field), value)
      val newEntity = entityWrapper.set(id, entity, field, addedValue).build()
      txn.put(newEntity)
      txn.commit()
    } finally {
      if (txn.isActive) {
        txn.rollback()
      }
    }
  }

  protected def createDatastoreOptions: DatastoreOptions = {
    def modify[A](o: Option[A])(modifyState: A => DatastoreOptions.Builder => DatastoreOptions.Builder) =
      State.modify[DatastoreOptions.Builder] { builder =>
        o.fold(builder)(a => modifyState(a)(builder))
      }

    val datastoreOptionsBuilder: State[DatastoreOptions.Builder, DatastoreOptions] = for {
      _ <- init[DatastoreOptions.Builder]
      _ <- modify(host)(h => _.setHost(h.toString))
      _ <- modify(credentials)(c => _.setCredentials(c))
      _ <- modify(retry)(r => _.setRetrySettings(r))
      s <- State.get[DatastoreOptions.Builder]
    } yield s.build()

    datastoreOptionsBuilder eval DatastoreOptions.newBuilder().setProjectId(project.id)
  }
}

object Datastore {
  def apply(project: Project,
            kind: Kind,
            host: Option[URI] = None,
            credentials: Option[Credentials] = None,
            retry: Option[RetrySettings] = None) =
    new Datastore(project, kind, host, credentials, retry)

  implicit class DatastoreOps[L <: HList](datastore: Datastore) {
    def get[A](id: String)(implicit gen: LabelledGeneric.Aux[A, L], fromL: FromEntity[L]): Option[A] =
      get(datastore.keyFactory.newKey(id))

    def get[A](id: Int)(implicit gen: LabelledGeneric.Aux[A, L], fromL: FromEntity[L]): Option[A] =
      get(datastore.keyFactory.newKey(id))

    def get[A](key: Key)(implicit gen: LabelledGeneric.Aux[A, L], fromL: FromEntity[L]): Option[A] = Option {
      datastore.datastore.get(key, ReadOption.eventualConsistency())
    } flatMap { entity =>
      fromEntity(entity)
    }
  }

}

trait EntityWrapper[A] {
  def set(key: Key, e: Option[Entity], field: String, value: A): Entity.Builder

  def get(e: Option[Entity], field: String): A
}

object EntityWrapper {

  implicit val longSetter: EntityWrapper[Long] = new EntityWrapper[Long] {

    val defaultValue = 0L

    def set(key: Key, e: Option[Entity], field: String, value: Long): Entity.Builder = e match {
      case Some(entity) => Entity.newBuilder(entity).set(field, value)
      case _ => Entity.newBuilder(key).set(field, value)
    }

    def get(e: Option[Entity], field: String): Long = e match {
      case Some(entity) => try {
        entity.getLong(field)
      } catch {
        case m: com.google.datastore.v1.client.DatastoreException if m.getMessage == s"No such property $field" => defaultValue
      }
      case _ => defaultValue
    }
  }

  implicit val doubleSetter: EntityWrapper[Double] = new EntityWrapper[Double] {

    val defaultValue = 0.0d

    def set(key: Key, e: Option[Entity], field: String, value: Double): Entity.Builder = e match {
      case Some(entity) => Entity.newBuilder(entity).set(field, value)
      case _ => Entity.newBuilder(key).set(field, value)
    }

    def get(e: Option[Entity], field: String): Double = e match {
      case Some(entity) => try {
        entity.getDouble(field)
      } catch {
        case m: com.google.datastore.v1.client.DatastoreException if m.getMessage == s"No such property $field" => defaultValue
      }
      case _ => defaultValue
    }
  }
}