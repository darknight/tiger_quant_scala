import cats.effect.{IO, IOApp}
import com.tquant.core.algo.AlgoEngine
import com.tquant.core.config.{ConfigLoader, ServerConf}
import com.tquant.core.engine.{MainEngine, OrderEngine}
import com.tquant.core.event.EventEngine
import com.tquant.core.log.logging
import com.tquant.gateway.TigerGateway
import org.typelevel.log4cats.LoggerFactory

object TigerQuantBootstrap extends IOApp.Simple {

  val logger = LoggerFactory[IO].getLogger

  def init(conf: ServerConf): IO[Unit] = {
    val eventEngine = EventEngine(conf.eventEngine.capacity)
    val algoEngine = new AlgoEngine(eventEngine)
    val orderEngine = new OrderEngine(eventEngine)
    val gateway = TigerGateway(conf, eventEngine)
    val mainEngine = new MainEngine(gateway, orderEngine, algoEngine, eventEngine)
    mainEngine.start()
  }

  val run: IO[Unit] = {
    for {
      result <- ConfigLoader.loadConfig()
      _ <- result match {
        case Left(err) => logger.error(s"config load failed => $err")
        case Right(conf) =>
          logger.info(s"config => $conf")
          init(conf)
      }
    } yield ()
  }
}
