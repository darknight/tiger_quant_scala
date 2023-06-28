package dao

import cats.effect.{IO, Resource}
import com.tquant.core.model.data.Tick
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres.implicits._ // For LocalDateTime

import java.time.LocalDateTime

// TODO: unit tests
class TickDAO(private val xaRes: Resource[IO, HikariTransactor[IO]]) {

  val ALL_COLUMN = "symbol,volume,amount,latest_price,latest_volume,latest_time,time,type,create_time"

  def saveTick(tick: Tick): IO[Int] = {
    val res = xaRes.use { xa =>
      for {
        insert <-
          sql"""
          INSERT INTO tick($ALL_COLUMN) VALUES (${tick.symbol},${tick.volume},${tick.amount},
          ${tick.latestPrice},${tick.latestVolume},${tick.latestTime},${tick.time},${tick.`type`},now())
           """.update.run.transact(xa)
      } yield insert
    }
    res
  }

  def queryTicks(symbol: String, start: LocalDateTime, end: LocalDateTime): IO[List[Tick]] = {
    val res = xaRes.use { xa =>
      for {
        query <- sql"""SELECT $ALL_COLUMN FROM tick WHERE symbol=$symbol AND latest_time>=$start AND time<=$end"""
          .query[Tick]
          .to[List]
          .transact(xa)
      } yield query
    }
    res
  }
}
