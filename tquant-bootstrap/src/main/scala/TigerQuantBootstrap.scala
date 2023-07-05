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

  /**
   * The topology of engine:
   * gateway, mainEngine, algoEngine, orderEngine -> eventEngine
   * algoEngine -> gateway, orderEngine
   * mainEngine -> gateway, orderEngine, algoEngine
   * @param conf
   * @return
   */
  def init(conf: ServerConf): IO[Unit] = {
    for {
      eventEngine <- EventEngine(conf.eventEngine.capacity)
      gateway <- TigerGateway(conf, eventEngine)
      orderEngine <- OrderEngine(eventEngine)
      algoEngine <- AlgoEngine(eventEngine, orderEngine, gateway)
      // TODO: load algos and add to algo engine elegantly
      _ <- TestAlgo.createAndAttach(algoEngine)
      mainEngine = new MainEngine(gateway, orderEngine, algoEngine, eventEngine)
      _ <- mainEngine.start()
    } yield ()
  }

  val run: IO[Unit] = {
    for {
      result <- ConfigLoader.loadConfig()
      _ <- result match {
        case Left(err) => logger.error(s"config load failed => $err")
        case Right(conf) =>
          logger.info(s"config => $conf") *> init(conf) *> logger.info("exit....")
      }
    } yield ()
  }
}
