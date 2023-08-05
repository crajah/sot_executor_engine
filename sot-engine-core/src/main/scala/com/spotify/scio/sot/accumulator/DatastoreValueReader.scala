package com.spotify.scio.sot.accumulator

import parallelai.sot.engine.generic.row.JavaRow
import parallelai.sot.engine.io.datastore.{Datastore, FromEntity}
import shapeless.HList

object DatastoreValueReader {

  def readValue[K, Value <: HList](key: K, value: Option[JavaRow[Value]],
                                   persistence: Option[Datastore],
                                   fromL: FromEntity[Value]): Option[JavaRow[Value]] = {
    val jrow = value match {
      case Some(st) => Option(st.hList)
      case None =>
        persistence match {
          case Some(p) =>
            key match {
              case k: Int => p.getHList[Value](k)(fromL)
              case k: String => p.getHList[Value](k)(fromL)
              case _ => throw new Exception("Only String and Int type keys supports persistence.")
            }
          case None => None
        }
    }
    jrow.map(JavaRow(_))
  }

}
