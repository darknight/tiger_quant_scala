package dao

import cats.effect.{IO, Resource}
import doobie.hikari.HikariTransactor

class ContractDAO(private val xaRes: Resource[IO, HikariTransactor[IO]]) {

}
