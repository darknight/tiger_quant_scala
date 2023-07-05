package com.tquant.core.engine
import cats.data.OptionT
import cats.effect.{IO, Ref}
import cats.implicits._
import com.tquant.core.event.{Event, EventEngine, EventHandler, EventType}
import com.tquant.core.log.logging
import com.tquant.core.model.data.{Asset, Contract, Order, Position, Tick, Trade}
import org.typelevel.log4cats.LoggerFactory

case class OrderEngineState(assetOptRef: Ref[IO, Option[Asset]],
                            tickMapRef: Ref[IO, Map[String, Tick]],
                            orderMapRef: Ref[IO, Map[Long, Order]],
                            activeOrderMapRef: Ref[IO, Map[Long, Order]],
                            tradeMapRef: Ref[IO, Map[Long, Trade]],
                            positionMapRef: Ref[IO, Map[String, Position]],
                            contractMapRef: Ref[IO, Map[String, Contract]])

/**
 * `OrderEngine` maintains a list of internal state and consume events.
 * It has no external dependency except EventEngine
 * @param eventEngine
 * @param state
 */
class OrderEngine(eventEngine: EventEngine, state: OrderEngineState) extends Engine {

  val engineName: String = "OrderEngine"
  val logger = LoggerFactory[IO].getLogger

  private val tickHandler = new EventHandler {
    def processEvent(event: Event): IO[Unit] = {
      event match {
        case Event(EventType.EVENT_TICK, Some(data)) if data.isInstanceOf[Tick] =>
          val tick = data.asInstanceOf[Tick]
          for {
            _ <- state.tickMapRef.update(map => map + (tick.symbol -> tick))
          } yield ()
        case _ => IO.unit
      }
    }
  }
  private val orderHandler = new EventHandler {
    def processEvent(event: Event): IO[Unit] = {
      def handleActiveOrder(order: Order): IO[Unit] = {
        IO(order.isActive).ifM({
          for {
            _ <- state.activeOrderMapRef.update(map => map + (order.id -> order))
          } yield ()
        }, {
          for {
            _ <- state.activeOrderMapRef.update(map => map - order.id)
          } yield ()
        })
      }

      event match {
        case Event(EventType.EVENT_ORDER, Some(data)) if data.isInstanceOf[Order] =>
          val order = data.asInstanceOf[Order]
          for {
            _ <- state.orderMapRef.update(map => map + (order.id -> order))
            _ <- handleActiveOrder(order)
          } yield ()
        case _ => IO.unit
      }
    }
  }
  private val tradeHandler = new EventHandler {
    def processEvent(event: Event): IO[Unit] = {
      event match {
        case Event(EventType.EVENT_TRADE, Some(data)) if data.isInstanceOf[Trade] =>
          val trade = data.asInstanceOf[Trade]
          for {
            _ <- state.tradeMapRef.update(map => map + (trade.orderId -> trade))
          } yield ()
        case _ => IO.unit
      }
    }
  }
  private val positionHandler = new EventHandler {
    override def processEvent(event: Event): IO[Unit] = {
      event match {
        case Event(EventType.EVENT_POSITION, Some(data)) if data.isInstanceOf[Position] =>
          val position = data.asInstanceOf[Position]
          for {
            _ <- state.positionMapRef.update(map => map + (position.identifier -> position))
            _ <- state.positionMapRef.update(map => map + (position.symbol -> position))
          } yield ()
        case _ => IO.unit
      }
    }
  }
  private val contractHandler = new EventHandler {
    def processEvent(event: Event): IO[Unit] = {
      event match {
        case Event(EventType.EVENT_CONTRACT, Some(data)) if data.isInstanceOf[Contract] =>
          val contract = data.asInstanceOf[Contract]
          for {
            _ <- state.contractMapRef.update(map => map + (contract.identifier -> contract))
          } yield ()
        case _ => IO.unit
      }
    }
  }
  private val assetHandler = new EventHandler {
    override def processEvent(event: Event): IO[Unit] = {
      event match {
        case Event(EventType.EVENT_ASSET, Some(data)) if data.isInstanceOf[Asset] =>
          val asset = data.asInstanceOf[Asset]
          for {
            _ <- state.assetOptRef.set(Some(asset))
          } yield ()
        case _ => IO.unit
      }
    }
  }

  private def registerHandlers(): IO[Unit] = {
    List(
      eventEngine.registerHandler(EventType.EVENT_TICK, tickHandler),
      eventEngine.registerHandler(EventType.EVENT_ORDER, orderHandler),
      eventEngine.registerHandler(EventType.EVENT_TRADE, tradeHandler),
      eventEngine.registerHandler(EventType.EVENT_POSITION, positionHandler),
      eventEngine.registerHandler(EventType.EVENT_CONTRACT, contractHandler),
      eventEngine.registerHandler(EventType.EVENT_ASSET, assetHandler),
    ).sequence_
  }

  def start(): IO[Unit] = {
    for {
      _ <- registerHandlers()
      _ <- logger.info(s"$engineName registered handlers to EventEngine")
    } yield ()
  }

  def stop(): IO[Unit] = ???

  def getTick(symbol: String): OptionT[IO, Tick] = {
    val res = for {
      map <- state.tickMapRef.get
      tickOpt = map.get(symbol)
    } yield tickOpt

    OptionT(res)
  }

  def getOrder(orderId: Long): OptionT[IO, Order] = {
    val res = for {
      map <- state.orderMapRef.get
      orderOpt = map.get(orderId)
    } yield orderOpt

    OptionT(res)
  }

  def getTrade(orderId: Long): OptionT[IO, Trade] = {
    val res = for {
      map <- state.tradeMapRef.get
      tradeOpt = map.get(orderId)
    } yield tradeOpt

    OptionT(res)
  }

  def getPosition(posId: String): OptionT[IO, Position] = {
    val res = for {
      map <- state.positionMapRef.get
      posOpt = map.get(posId)
    } yield posOpt

    OptionT(res)
  }

  def getContract(identifier: String): OptionT[IO, Contract] = {
    val res = for {
      map <- state.contractMapRef.get
      contractOpt = map.get(identifier)
    } yield contractOpt

    OptionT(res)
  }

  def getAsset: OptionT[IO, Asset] = {
    val res = for {
      assetOpt <- state.assetOptRef.get
    } yield assetOpt

    OptionT(res)
  }

  def getAllTicks: IO[List[Tick]] = {
    for {
      map <- state.tickMapRef.get
      res = map.values.toList
    } yield res
  }

  def getAllOrders: IO[List[Order]] = {
    for {
      map <- state.orderMapRef.get
      res = map.values.toList
    } yield res
  }

  def getAllTrades: IO[List[Trade]] = {
    for {
      map <- state.tradeMapRef.get
      res = map.values.toList
    } yield res
  }

  def getAllPositions: IO[List[Position]] = {
    for {
      map <- state.positionMapRef.get
      res = map.values.toList
    } yield res
  }

  def getAllContracts: IO[List[Contract]] = {
    for {
      map <- state.contractMapRef.get
      res = map.values.toList
    } yield res
  }

  def getAllActiveOrders(symbol: String): IO[List[Order]] = {
    for {
      map <- state.activeOrderMapRef.get
      res = map.values.toList.filter(_.symbol.equalsIgnoreCase(symbol))
    } yield res
  }
}

object OrderEngine {
  def apply(eventEngine: EventEngine): IO[OrderEngine] = {
    for {
      assetOptRef <- Ref.of[IO, Option[Asset]](None)
      tickMapRef <- Ref.of[IO, Map[String, Tick]](Map.empty)
      orderMapRef <- Ref.of[IO, Map[Long, Order]](Map.empty)
      activeOrderMapRef <- Ref.of[IO, Map[Long, Order]](Map.empty)
      tradeMapRef <- Ref.of[IO, Map[Long, Trade]](Map.empty)
      positionMapRef <- Ref.of[IO, Map[String, Position]](Map.empty)
      contractMapRef <- Ref.of[IO, Map[String, Contract]](Map.empty)
      state = OrderEngineState(
        assetOptRef,
        tickMapRef,
        orderMapRef,
        activeOrderMapRef,
        tradeMapRef,
        positionMapRef,
        contractMapRef
      )
      engine = new OrderEngine(eventEngine, state)
    } yield engine
  }
}
