package com.tquant.core.algo

import cats.data.NonEmptyList
import cats.syntax.parallel._
import cats.effect.{IO, Ref}
import cats.implicits._
import com.tquant.core.engine.Engine
import com.tquant.core.event.{Event, EventEngine, EventHandler, EventType}
import com.tquant.core.model.data.{Asset, Bar, Contract, Order, Position, Tick, Trade}
import com.tquant.core.model.enums.{BarType, Direction, OrderType}

class AlgoEngine(eventEngine: EventEngine) extends Engine {

  val engineName = "AlgoEngine"

  private val symbolAlgoNameListMapIO = Ref.of[IO, Map[String, List[String]]](Map.empty)
  private val nameAlgoMapIO = Ref.of[IO, Map[String, AlgoTemplate]](Map.empty)
  private val orderAlgoNameMapIO = Ref.of[IO, Map[Long, String]](Map.empty)

  private val tickHandler = new EventHandler {
    def processEvent(event: Event): IO[Unit] = {
      event match {
        case Event(EventType.EVENT_TICK, Some(data)) if data.isInstanceOf[Tick] =>
          val tick = data.asInstanceOf[Tick]
          for {
            symbolAlgoRef <- symbolAlgoNameListMapIO
            symbolAlgoMap <- symbolAlgoRef.get
            algoNames = symbolAlgoMap.getOrElse(tick.symbol, List.empty)
            algoRef <- nameAlgoMapIO
            algoMap <- algoRef.get
            algos = algoNames.flatMap(algoMap.get)
            _ <- algos.map(_.updateTick(tick)).sequence_
          } yield ()
        case _ => IO.unit
      }
    }
  }
  private val timerHandler = new EventHandler {
    override def processEvent(event: Event): IO[Unit] = {
      for {
        algoRef <- nameAlgoMapIO
        algoMap <- algoRef.get
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
            orderAlgoNameRef <- orderAlgoNameMapIO
            orderAlgoNameMap <- orderAlgoNameRef.get
            algoNameOpt = orderAlgoNameMap.get(trade.orderId)
            algoRef <- nameAlgoMapIO
            algoMap <- algoRef.get
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
            orderAlgoNameRef <- orderAlgoNameMapIO
            orderAlgoNameMap <- orderAlgoNameRef.get
            algoNameOpt = orderAlgoNameMap.get(order.id)
            algoRef <- nameAlgoMapIO
            algoMap <- algoRef.get
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

  def start(): IO[Unit] = {
    for {
      _ <- registerHandlers()
    } yield ()
  }

  private def stopAlgos(): IO[Unit] = {
    for {
      algoRef <- nameAlgoMapIO
      algoMap <- algoRef.get
      _ <- algoMap.values.toList.map(_.stop()).sequence_
      _ <- algoRef.set(Map.empty)
    } yield ()
  }

  def stop(): IO[Unit] = {
    for {
      _ <- stopAlgos()
    } yield ()
  }

  def cancelOrder(orderId: Long): IO[Unit] = ???

  def sendOrder(algoName: String, symbol: String, direction: Direction,
                price: Double, volume: Int, orderType: OrderType): IO[Unit] = ???

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
