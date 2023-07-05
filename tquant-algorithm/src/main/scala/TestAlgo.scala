import cats.effect.{IO, Ref}
import com.tquant.core.algo.{AlgoEngine, AlgoTemplate}
import com.tquant.core.log.logging
import com.tquant.core.model.data.{Bar, Order, Tick, Trade}
import org.typelevel.log4cats.LoggerFactory

class TestAlgo(algoEngine: AlgoEngine,
               activeRef: Ref[IO, Boolean],
               activeOrderMapRef: Ref[IO, Map[Long, Order]],
               tickMapRef: Ref[IO, Map[String, Tick]])
  extends AlgoTemplate(algoEngine, activeRef, activeOrderMapRef, tickMapRef) {

  override def onBar(bar: Bar): IO[Unit] = IO.unit

  override def onStart(): IO[Unit] = {
    for {
      _ <- logger.info(s"${algoName}.onStart is called")
    } yield ()
  }

  override def onStop(): IO[Unit] = IO.unit

  override def onTimer(): IO[Unit] = IO.unit

  override def onTick(tick: Tick): IO[Unit] = IO.unit

  override def onOrder(order: Order): IO[Unit] = IO.unit

  override def onTrade(trade: Trade): IO[Unit] = IO.unit
}

object TestAlgo {
  def createAndAttach(algoEngine: AlgoEngine): IO[Unit] = {
    for {
      activeRef <- Ref.of[IO, Boolean](false)
      activeOrderMapRef <- Ref.of[IO, Map[Long, Order]](Map.empty)
      tickMapRef <- Ref.of[IO, Map[String, Tick]](Map.empty)
      algo = new TestAlgo(algoEngine, activeRef, activeOrderMapRef, tickMapRef)
      _ <- algoEngine.addAlgoImpl(algo)
    } yield ()
  }
}
