package com.tquant.storage.dao

import cats.effect.{IO, Resource}
import com.tquant.core.model.data.Bar
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres.implicits._ // For LocalDateTime

import java.time.LocalDateTime

// TODO: unit tests
class BarDAO(private val xaRes: Resource[IO, HikariTransactor[IO]]) {

  def saveBar(bar: Bar): IO[Int] = {
    val res = xaRes.use { xa =>
      for {
        insert <-
          sql"""
          INSERT INTO bar(symbol,duration,period,open,high,low,close,volume,amount,time)
          VALUES (${bar.symbol},${bar.duration},${bar.period},${bar.open},${bar.high},
          ${bar.low},${bar.close},${bar.volume},${bar.amount},${bar.time})"""
          .update
          .run
          .transact(xa)
      } yield insert
    }
    res
  }

  def queryBars(symbol: String, limit: Int): IO[List[Bar]] = {
    val res = xaRes.use { xa =>
      for {
        query <-
          sql"""SELECT symbol,duration,period,open,high,low,close,volume,amount,time FROM bar
               WHERE symbol=$symbol LIMIT $limit"""
          .query[Bar]
          .to[List]
          .transact(xa)
      } yield query
    }
    res
  }

  def queryBars(symbol: String, period: String,
                beginDate: LocalDateTime, endDate: LocalDateTime): IO[List[Bar]] = {
    val res = xaRes.use { xa =>
      for {
        query <-
          sql"""SELECT symbol,duration,period,open,high,low,close,volume,amount,time FROM bar
               WHERE symbol=$symbol AND period=$period AND time>=$beginDate AND time<=$endDate"""
          .query[Bar]
          .to[List]
          .transact(xa)
      } yield query
    }
    res
  }
}
