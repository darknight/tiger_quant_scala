package com.tquant.core.algo

import cats.data.NonEmptyList
import cats.syntax.parallel._
import cats.effect.{IO, Ref}
import cats.implicits._
import com.tquant.core.engine.Engine
import com.tquant.core.event.{Event, EventEngine, EventHandler, EventType}
import com.tquant.core.log.logging
import com.tquant.core.model.data.{Asset, Bar, Contract, Order, Position, Tick, Trade}
import com.tquant.core.model.enums.{BarType, Direction, OrderType}
import org.typelevel.log4cats.LoggerFactory

class AlgoEngine(eventEngine: EventEngine,
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

  def cancelOrder(orderId: Long): IO[Unit] = ???

  def sendOrder(algoName: String, symbol: String, direction: Direction,
                price: Double, volume: Int, orderType: OrderType): IO[Long] = ???

  def getTick(symbol: String): IO[Tick] = ???

  def subscribe(algoName: String, symbol: String): IO[Unit] = ???

  def cancelSubscribe(algoName: String, symbol: String): IO[Unit] = ???

  def getBars(symbol: String, barType: BarType, limit: Int): IO[List[Bar]] = ???

  def getSymbolBarMap(symbols: NonEmptyList[String], barType: BarType,
                      limit: Int): IO[Map[String, List[Bar]]] = ???

  def getContract(symbol: String): IO[Contract] = ???

  def getAsset: IO[Asset] = ???

  def getPosition(positionId: String): IO[Position] = ???

  def getAllPositions: IO[List[Position]] = ???

  def getAllActiveOrders: IO[List[Order]] = ???
}

object AlgoEngine {
  def apply(eventEngine: EventEngine): IO[AlgoEngine] = {
    for {
      symbolAlgoNameListMapRef <- Ref.of[IO, Map[String, List[String]]](Map.empty)
      nameAlgoMapRef <- Ref.of[IO, Map[String, AlgoTemplate]](Map.empty)
      orderAlgoNameMapRef <- Ref.of[IO, Map[Long, String]](Map.empty)
      engine = new AlgoEngine(eventEngine, symbolAlgoNameListMapRef, nameAlgoMapRef, orderAlgoNameMapRef)
    } yield engine
  }
}
