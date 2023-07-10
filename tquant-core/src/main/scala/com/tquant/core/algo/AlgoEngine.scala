package com.tquant.core.algo

import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.syntax.parallel._
import cats.effect.{IO, Ref}
import cats.implicits._
import com.tquant.core.TigerQuantException
import com.tquant.core.engine.{Engine, OrderEngine}
import com.tquant.core.event.{Event, EventEngine, EventHandler, EventType}
import com.tquant.core.gateway.Gateway
import com.tquant.core.log.logging
import com.tquant.core.model.data.{Asset, Bar, Contract, MarketStatus, Order, Position, Tick, Trade}
import com.tquant.core.model.enums.{BarType, Direction, OrderType}
import com.tquant.core.model.request.{ModifyRequest, OrderRequest, SubscribeRequest}
import org.typelevel.log4cats.LoggerFactory

/**
 * `AlgoEngine` consumes events, call public APIs of `OrderEngine` & `Gateway`
 *
 * @param eventEngine
 * @param symbolAlgoNameListMapRef
 * @param nameAlgoMapRef
 * @param orderAlgoNameMapRef
 */
class AlgoEngine(eventEngine: EventEngine,
                 orderEngine: OrderEngine,
                 gateway: Gateway,
                 symbolAlgoNameListMapRef: Ref[IO, Map[String, List[String]]],
                 nameAlgoMapRef: Ref[IO, Map[String, AlgoTemplate]],
                 orderAlgoNameMapRef: Ref[IO, Map[Long, String]]) extends Engine {

  val engineName = "AlgoEngine"
  val logger = LoggerFactory[IO].getLogger

  private val tickHandler = new EventHandler {
    def processEvent(event: Event): IO[Unit] = {
      event match {
        case Event(EventType.EVENT_TICK, Some(data)) if data.isInstanceOf[Tick] =>
          val tick = data.asInstanceOf[Tick]
          for {
            symbolAlgoNameListMap <- symbolAlgoNameListMapRef.get
            algoNames = symbolAlgoNameListMap.getOrElse(tick.symbol, List.empty)
            nameAlgoMap <- nameAlgoMapRef.get
            algos = algoNames.flatMap(nameAlgoMap.get)
            _ <- algos.map(_.updateTick(tick)).sequence_
          } yield ()
        case _ => IO.unit
      }
    }
  }
  private val timerHandler = new EventHandler {
    override def processEvent(event: Event): IO[Unit] = {
      for {
        algoMap <- nameAlgoMapRef.get
        _ <- algoMap.values.toList.map(_.updateTimer()).sequence_
      } yield ()
    }
  }
  private val tradeHandler = new EventHandler {
    def processEvent(event: Event): IO[Unit] = {
      event match {
        case Event(EventType.EVENT_TRADE, Some(data)) if data.isInstanceOf[Trade] =>
          val trade = data.asInstanceOf[Trade]
          for {
            orderAlgoNameMap <- orderAlgoNameMapRef.get
            algoNameOpt = orderAlgoNameMap.get(trade.orderId)
            algoMap <- nameAlgoMapRef.get
            algo = algoNameOpt.flatMap(algoMap.get)
            _ <- algo.map(_.updateTrade(trade)).getOrElse(IO.unit)
          } yield ()
        case _ => IO.unit
      }
    }
  }
  private val orderHandler = new EventHandler {
    def processEvent(event: Event): IO[Unit] = {
      event match {
        case Event(EventType.EVENT_ORDER, Some(data)) if data.isInstanceOf[Order] =>
          val order = data.asInstanceOf[Order]
          for {
            orderAlgoNameMap <- orderAlgoNameMapRef.get
            algoNameOpt = orderAlgoNameMap.get(order.id)
            algoMap <- nameAlgoMapRef.get
            algo = algoNameOpt.flatMap(algoMap.get)
            _ <- algo.map(_.updateOrder(order)).getOrElse(IO.unit)
          } yield ()
        case _ => IO.unit
      }
    }
  }

  // TODO: run in parallel
  private def registerHandlers(): IO[Unit] = {
    List(
      eventEngine.registerHandler(EventType.EVENT_TICK, tickHandler),
      eventEngine.registerHandler(EventType.EVENT_TIMER, timerHandler),
      eventEngine.registerHandler(EventType.EVENT_TRADE, tradeHandler),
      eventEngine.registerHandler(EventType.EVENT_ORDER, orderHandler)
    ).sequence_
  }

  def addAlgoImpl(algo: AlgoTemplate): IO[Unit] = {
    for {
      _ <- nameAlgoMapRef.update(map => map + (algo.algoName -> algo))
    } yield ()
  }

  /**
   * register event handlers to event engine, then start all the algo implementations
   *
   * @return
   */
  def start(): IO[Unit] = {
    for {
      _ <- registerHandlers()
      _ <- logger.info(s"$engineName registered handlers to EventEngine")
      algoMap <- nameAlgoMapRef.get
      _ <- algoMap.values.toList.map(_.start()).sequence_
    } yield ()
  }

  private def stopAlgos(): IO[Unit] = {
    for {
      algoMap <- nameAlgoMapRef.get
      _ <- algoMap.values.toList.map(_.stop()).sequence_
      _ <- nameAlgoMapRef.set(Map.empty)
    } yield ()
  }

  def stop(): IO[Unit] = {
    for {
      _ <- stopAlgos()
    } yield ()
  }

  def getMarketStatus(market: String): IO[List[MarketStatus]] = gateway.getMarketStatus(market)

  def getBars(symbol: String, barType: BarType, limit: Int): IO[List[Bar]] = {
    for {
      resp <- gateway.getBars(NonEmptyList.of(symbol), barType, limit).value
      res <- resp match {
        case Left(err) =>
          logger.error(s"getBars failed => $err") *> IO.pure(List.empty)
        case Right(map) => IO(map.getOrElse(symbol, List.empty))
      }
    } yield res
  }

  def getSymbolBarMap(symbols: NonEmptyList[String], barType: BarType,
                      limit: Int): IO[Map[String, List[Bar]]] = {
    for {
      resp <- gateway.getBars(symbols, barType, limit).value
      res <- resp match {
        case Left(err) =>
          logger.error(s"getBars failed => $err") *> IO(Map.empty[String, List[Bar]])
        case Right(map) => IO(map)
      }
    } yield res
  }

  /**
   * This method returns IO(0) indicates failure to send order to tiger service
   * @param algoName
   * @param symbol
   * @param direction
   * @param price
   * @param volume
   * @param orderType
   * @return
   */
  def sendOrder(algoName: String, symbol: String, direction: Direction,
                price: Double, volume: Int, orderType: OrderType): IO[Long] = {
    for {
      contractOpt <- orderEngine.getContract(symbol).value
      id <- contractOpt match {
        case None =>
          logger.error(s"sendOrder failed, contract not found, identifier = $symbol")
            .as(0L)
        case Some(contract) =>
          if (contract.lotSize > 0 && volume % contract.lotSize != 0) {
            logger.error(s"sendOrder failed, volume($volume) is not multiple of lotSize(${contract.lotSize})")
              .as(0L)
          } else if (contract.minTick > 0 &&
            (price - Math.floor(price / contract.minTick) * contract.minTick) >= 0.000001) {
            logger.error(s"sendOrder failed, price($price) is not multiple of minTick(${contract.minTick})")
              .as(0L)
          } else {
            val request = OrderRequest(symbol, contract.exchange, direction.entryName,
              orderType.entryName, volume, price)
            gateway.sendOrder(request).value
              .flatMap {
                case Left(err) => logger.error(s"sendOrder failed => $err").as(0L)
                case Right(id) => IO.pure(id)
              }
          }
      }
      _ <- IO(id > 0).ifM(orderAlgoNameMapRef.update(m => m + (id -> algoName)), IO.unit)
    } yield id
  }

  // TODO: improve impl
  def cancelOrder(orderId: Long): IO[Unit] = {
    for {
      orderOpt <- orderEngine.getOrder(orderId).value
      resp <- orderOpt match {
        case Some(order) =>
          gateway.cancelOrder(order.createCancelRequest).value
        case None =>
          logger.error(s"cancelOrder failed, order not found, id = $orderId").map { _ =>
            val res: Either[TigerQuantException, Unit] = Right(())
            res
          }
      }
      result <- resp match {
        case Left(err) => logger.error(s"cancelOrder error => $err")
        case Right(_) => IO.unit
      }
    } yield result
  }

  def subscribe(algoName: String, symbol: String): IO[Unit] = ???

  def cancelSubscribe(algoName: String, symbol: String): IO[Unit] = ???

  //
  // orderEngine operations
  //
  def getTick(symbol: String): OptionT[IO, Tick] = orderEngine.getTick(symbol)

  def getOrder(orderId: Long): OptionT[IO, Order] = orderEngine.getOrder(orderId)

  def getTrade(orderId: Long): OptionT[IO, Trade] = orderEngine.getTrade(orderId)

  def getPosition(posId: String): OptionT[IO, Position] = orderEngine.getPosition(posId)

  def getContract(identifier: String): OptionT[IO, Contract] = orderEngine.getContract(identifier)

  def getAsset: OptionT[IO, Asset] = orderEngine.getAsset

  def getAllTicks: IO[List[Tick]] = orderEngine.getAllTicks

  def getAllOrders: IO[List[Order]] = orderEngine.getAllOrders

  def getAllTrades: IO[List[Trade]] = orderEngine.getAllTrades

  def getAllPositions: IO[List[Position]] = orderEngine.getAllPositions

  def getAllContracts: IO[List[Contract]] = orderEngine.getAllContracts

  def getAllActiveOrders(symbol: String): IO[List[Order]] = orderEngine.getAllActiveOrders(symbol)

  //
  // DB operations
  //
  def queryBars(symbol: String, limit: Int): IO[List[Bar]] = {
    gateway.queryBars(symbol, limit)
  }
}

object AlgoEngine {
  def apply(eventEngine: EventEngine, orderEngine: OrderEngine, gateway: Gateway): IO[AlgoEngine] = {
    for {
      symbolAlgoNameListMapRef <- Ref.of[IO, Map[String, List[String]]](Map.empty)
      nameAlgoMapRef <- Ref.of[IO, Map[String, AlgoTemplate]](Map.empty)
      orderAlgoNameMapRef <- Ref.of[IO, Map[Long, String]](Map.empty)
      engine = new AlgoEngine(
        eventEngine,
        orderEngine,
        gateway,
        symbolAlgoNameListMapRef,
        nameAlgoMapRef,
        orderAlgoNameMapRef
      )
    } yield engine
  }
}
