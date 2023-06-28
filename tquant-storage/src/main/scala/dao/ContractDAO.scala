package dao

import cats.data.OptionT
import cats.effect.{IO, Resource}
import com.tquant.core.model.data.Contract
import doobie.hikari.HikariTransactor
import doobie.implicits._

// TODO: unit tests
class ContractDAO(private val xaRes: Resource[IO, HikariTransactor[IO]]) {

  val ALL_COLUMN = "identifier,name,symbol,sec_type,currency,exchange,market,expiry," +
    "contract_month,strike,multiplier,right,min_tick,lot_size,create_time"

  def saveContract(contract: Contract): IO[Int] = {
    val res = xaRes.use { xa =>
      for {
        insert <-
          sql"""
            INSERT INTO contract($ALL_COLUMN) VALUES (${contract.identifier},${contract.name},
            ${contract.symbol},${contract.secType},${contract.currency},${contract.exchange},
            ${contract.market},${contract.expiry},${contract.contractMonth},${contract.strike},
            ${contract.multiplier},${contract.right},${contract.minTick},${contract.lotSize},now())
             """.update.run.transact(xa)
      } yield insert
    }
    res
  }

  def queryContract(identifier: String): OptionT[IO, Contract] = {
    val res = xaRes.use { xa =>
      for {
        query <-
          sql"""SELECT $ALL_COLUMN FROM contract WHERE identifier=$identifier"""
            .query[Contract]
            .option
            .transact(xa)
      } yield query
    }
    OptionT(res)
  }

  def queryContracts(): IO[List[Contract]] = {
    xaRes.use { xa =>
      for {
        query <-
          sql"""SELECT $ALL_COLUMN FROM contract"""
            .query[Contract]
            .to[List]
            .transact(xa)
      } yield query
    }
  }
}
