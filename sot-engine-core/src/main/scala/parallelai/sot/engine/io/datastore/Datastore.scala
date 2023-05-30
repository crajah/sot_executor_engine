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

class Datastore private(project: Project,
                        kind: Kind,
                        host: Option[URI] = None,
                        credentials: Option[Credentials] = None,
                        retry: Option[RetrySettings] = None) extends Serializable {
  @transient
  implicit protected lazy val datastoreOptions: DatastoreOptions = createDatastoreOptions

  @transient
  implicit protected lazy val datastore: com.google.cloud.datastore.Datastore = datastoreOptions.getService

  @transient
  implicit lazy val keyFactory: KeyFactory = datastore.newKeyFactory().setKind(kind.value)

  def put[A, L <: HList](id: Long, a: A)(implicit gen: LabelledGeneric.Aux[A, L], toL: ToEntity[L]): Entity =
    datastore.put(toEntity(id, a))

  def put[A, L <: HList](id: String, a: A)(implicit gen: LabelledGeneric.Aux[A, L], toL: ToEntity[L]): Entity =
    datastore.put(toEntity(id, a))

  def put(entity: Entity): Entity =
    datastore.put(entity)

  def run[T](query: Query[T], options: Seq[ReadOption] = Seq(ReadOption.eventualConsistency())): QueryResults[T] =
    datastore.run(query, options: _*)

  def delete(keys: Key*): Unit =
    datastore.delete(keys: _*)

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