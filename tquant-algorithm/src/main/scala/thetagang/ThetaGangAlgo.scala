package thetagang

import cats.effect.{IO, Ref}
import com.tquant.core.algo.{AlgoEngine, AlgoTemplate}
import com.tquant.core.model.data.{Bar, Order, Tick, Trade}
import com.tquant.gateway.TigerGateway

class ThetaGangAlgo(algoEngine: AlgoEngine,
                    tigerGateway: TigerGateway,
                    portfolioConf: RollingSellPutConfig,
                    activeRef: Ref[IO, Boolean],
                    activeOrderMapRef: Ref[IO, Map[Long, Order]],
                    tickMapRef: Ref[IO, Map[String, Tick]])
  extends AlgoTemplate(algoEngine, activeRef, activeOrderMapRef, tickMapRef) {

  private val portfolioMgr = new PortfolioManager(algoEngine, tigerGateway, portfolioConf)

  override def onBar(bar: Bar): IO[Unit] = ???

  override def onStart(): IO[Unit] = {
    portfolioMgr.manage()
  }

  override def onStop(): IO[Unit] = ???

  override def onTimer(): IO[Unit] = ???

  override def onTick(tick: Tick): IO[Unit] = ???

  override def onOrder(order: Order): IO[Unit] = {
    portfolioMgr.orderStatusEvent(order)
  }

  override def onTrade(trade: Trade): IO[Unit] = ???
}

object ThetaGangAlgo {
  def createAndAttach(algoEngine: AlgoEngine, tigerGateway: TigerGateway): IO[Unit] = {
    for {
      activeRef <- Ref.of[IO, Boolean](false)
      activeOrderMapRef <- Ref.of[IO, Map[Long, Order]](Map.empty)
      tickMapRef <- Ref.of[IO, Map[String, Tick]](Map.empty)
      algo = new ThetaGangAlgo(algoEngine,
        tigerGateway,
        RollingSellPutConfig.empty, // FIXME
        activeRef,
        activeOrderMapRef,
        tickMapRef
      )
      _ <- algoEngine.addAlgoImpl(algo)
    } yield ()
  }
}
