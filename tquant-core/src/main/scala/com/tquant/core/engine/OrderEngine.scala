package com.tquant.core.engine
import cats.data.OptionT
import cats.effect.{IO, Ref}
import cats.implicits._
import com.tquant.core.event.{Event, EventEngine, EventHandler, EventType}
import com.tquant.core.model.data.{Asset, Contract, Order, Position, Tick, Trade}

class OrderEngine(eventEngine: EventEngine) extends Engine {
  val engineName: String = "OrderEngine"

  private val assetOptIO = Ref.of[IO, Option[Asset]](None)
  private val tickMapIO = Ref.of[IO, Map[String, Tick]](Map.empty)
  private val orderMapIO = Ref.of[IO, Map[Long, Order]](Map.empty)
  private val activeOrderMapIO = Ref.of[IO, Map[Long, Order]](Map.empty)
  private val tradeMapIO = Ref.of[IO, Map[Long, Trade]](Map.empty)
  private val positionMapIO = Ref.of[IO, Map[String, Position]](Map.empty)
  private val contractMapIO = Ref.of[IO, Map[String, Contract]](Map.empty)

  private val tickHandler = new EventHandler {
    def processEvent(event: Event): IO[Unit] = {
      event match {
        case Event(EventType.EVENT_TICK, Some(data)) if data.isInstanceOf[Tick] =>
          val tick = data.asInstanceOf[Tick]
          for {
            tickRef <- tickMapIO
            _ <- tickRef.update(map => map + (tick.symbol -> tick))
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
            activeOrderRef <- activeOrderMapIO
            _ <- activeOrderRef.update(map => map + (order.id -> order))
          } yield ()
        }, {
          for {
            activeOrderRef <- activeOrderMapIO
            _ <- activeOrderRef.update(map => map - order.id)
          } yield ()
        })
      }

      event match {
        case Event(EventType.EVENT_ORDER, Some(data)) if data.isInstanceOf[Order] =>
          val order = data.asInstanceOf[Order]
          for {
            orderRef <- orderMapIO
            _ <- orderRef.update(map => map + (order.id -> order))
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
            tradeRef <- tradeMapIO
            _ <- tradeRef.update(map => map + (trade.orderId -> trade))
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
            posRef <- positionMapIO
            _ <- posRef.update(map => map + (position.identifier -> position))
            _ <- posRef.update(map => map + (position.symbol -> position))
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
            contractRef <- contractMapIO
            _ <- contractRef.update(map => map + (contract.identifier -> contract))
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
            assetOptRef <- assetOptIO
            _ <- assetOptRef.set(Some(asset))
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
    } yield ()
  }

  def stop(): IO[Unit] = ???

  def getTick(symbol: String): OptionT[IO, Tick] = {
    val res = for {
      ref <- tickMapIO
      map <- ref.get
      tickOpt = map.get(symbol)
    } yield tickOpt

    OptionT(res)
  }

  def getOrder(orderId: Long): OptionT[IO, Order] = {
    val res = for {
      ref <- orderMapIO
      map <- ref.get
      orderOpt = map.get(orderId)
    } yield orderOpt

    OptionT(res)
  }

  def getTrade(orderId: Long): OptionT[IO, Trade] = {
    val res = for {
      ref <- tradeMapIO
      map <- ref.get
      tradeOpt = map.get(orderId)
    } yield tradeOpt

    OptionT(res)
  }

  def getPosition(posId: String): OptionT[IO, Position] = {
    val res = for {
      ref <- positionMapIO
      map <- ref.get
      posOpt = map.get(posId)
    } yield posOpt

    OptionT(res)
  }

  def getContract(identifier: String): OptionT[IO, Contract] = {
    val res = for {
      ref <- contractMapIO
      map <- ref.get
      contractOpt = map.get(identifier)
    } yield contractOpt

    OptionT(res)
  }

  def getAsset: OptionT[IO, Asset] = {
    val res = for {
      ref <- assetOptIO
      assetOpt <- ref.get
    } yield assetOpt

    OptionT(res)
  }

  def getAllTicks: IO[List[Tick]] = {
    for {
      ref <- tickMapIO
      map <- ref.get
      res = map.values.toList
    } yield res
  }

  def getAllOrders: IO[List[Order]] = {
    for {
      ref <- orderMapIO
      map <- ref.get
      res = map.values.toList
    } yield res
  }

  def getAllTrades: IO[List[Trade]] = {
    for {
      ref <- tradeMapIO
      map <- ref.get
      res = map.values.toList
    } yield res
  }

  def getAllPositions: IO[List[Position]] = {
    for {
      ref <- positionMapIO
      map <- ref.get
      res = map.values.toList
    } yield res
  }

  def getAllContracts: IO[List[Contract]] = {
    for {
      ref <- contractMapIO
      map <- ref.get
      res = map.values.toList
    } yield res
  }

  def getAllActiveOrders(symbol: String): IO[List[Order]] = {
    for {
      ref <- activeOrderMapIO
      map <- ref.get
      res = map.values.toList.filter(_.symbol.equalsIgnoreCase(symbol))
    } yield res
  }
}
