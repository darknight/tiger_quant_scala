package dao

import cats.effect.{IO, Resource}
import com.tquant.core.model.data.Bar
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres.implicits._ // For LocalDateTime

import java.time.LocalDateTime

// TODO: unit tests
class BarDAO(private val xaRes: Resource[IO, HikariTransactor[IO]]) {

  val QUERY_COLUMN = "symbol,period,open,high,low,close,volume,duration,amount,time"
  val ALL_COLUMN = QUERY_COLUMN + "create_time"

  def saveBar(bar: Bar): IO[Int] = {
    val res = xaRes.use { xa =>
      for {
        insert <-
          sql"""
          INSERT INTO bar($ALL_COLUMN) VALUES (${bar.symbol},${bar.period},${bar.open},${bar.high},
          ${bar.low},${bar.close},${bar.volume},${bar.duration},${bar.amount},
          ${bar.time},now())
           """
          .update.run.transact(xa)
      } yield insert
    }
    res
  }

  def queryBar(symbol: String, limit: Int): IO[List[Bar]] = {
    val res = xaRes.use { xa =>
      for {
        query <- sql"""SELECT $QUERY_COLUMN FROM bar WHERE symbol=$symbol LIMIT $limit"""
          .query[Bar]
          .to[List]
          .transact(xa)
      } yield query
    }
    res
  }

  def queryBar(symbol: String, period: String,
               beginDate: LocalDateTime, endDate: LocalDateTime): IO[List[Bar]] = {
    val res = xaRes.use { xa =>
      for {
        query <-
          sql"""SELECT $QUERY_COLUMN FROM bar
               WHERE symbol=$symbol AND period=$period AND time>=$beginDate AND time<=$endDate"""
          .query[Bar]
          .to[List]
          .transact(xa)
      } yield query
    }
    res
  }
}
