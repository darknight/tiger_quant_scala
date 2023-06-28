import cats.effect.IO
import cats.effect.kernel.Resource
import com.zaxxer.hikari.HikariConfig
import dao.{BarDAO, ContractDAO, TickDAO}
import doobie.hikari.HikariTransactor

// TODO: respect `"storage.enable"` settings from ConfigLoader
object DAOInstance {
  val xaRes: Resource[IO, HikariTransactor[IO]] = for {
    hikariConfig <- Resource.pure {
      val config = new HikariConfig()
      config.setDriverClassName("org.postgresql.Driver")
      // TODO: read setting from config
      config.setJdbcUrl("jdbc:postgresql:tiger_quant")
      config.setUsername("test")
      config.setPassword("test")
      config
    }
    xa <- HikariTransactor.fromHikariConfig[IO](hikariConfig)
  } yield xa

  val barDAO = new BarDAO(xaRes)
  val contractDAO = new ContractDAO(xaRes)
  val tickDAO = new TickDAO(xaRes)
}
