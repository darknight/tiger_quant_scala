package com.tquant.core.algo

import cats._
import cats.data.NonEmptyList
import cats.implicits._
import cats.effect.{IO, Ref}
import com.tquant.core.log.logging
import com.tquant.core.model.data.{Asset, Bar, Contract, Order, Position, Tick, Trade}
import com.tquant.core.model.enums.{BarType, Direction, OrderType}
import org.typelevel.log4cats.LoggerFactory

abstract class AlgoTemplate(algoEngine: AlgoEngine,
                            activeRef: Ref[IO, Boolean],
                            activeOrderMapRef: Ref[IO, Map[Long, Order]],
                            tickMapRef: Ref[IO, Map[String, Tick]]) {

  val algoName = this.getClass.getSimpleName
  val logger = LoggerFactory[IO].getLogger

  def onBar(bar: Bar): IO[Unit]
  def onStart(): IO[Unit]
  def onStop(): IO[Unit]
  def onTimer(): IO[Unit]
  def onTick(tick: Tick): IO[Unit]
  def onOrder(order: Order): IO[Unit]
  def onTrade(trade: Trade): IO[Unit]

  def start(): IO[Unit] = {
    for {
      _ <- activeRef.set(true)
      _ <- onStart()
    } yield ()
  }

  def stop(): IO[Unit] = {
    for {
      _ <- activeRef.set(false)
      _ <- cancelAll()
      _ <- onStop()
    } yield ()
  }

  def cancelAll(): IO[Unit] = {
    for {
      map <- activeOrderMapRef.get
      _ <- map.keys.toList.map(algoEngine.cancelOrder).sequence_
    } yield ()
  }

  def updateTick(tick: Tick): IO[Unit] = {
    def handleTick(): IO[Unit] = {
      for {
        map <- tickMapRef.updateAndGet(map => {
          val newTick = map.get(tick.symbol) match {
            case Some(history) => history.update(tick)
            case None => tick
          }
          map + (tick.symbol -> newTick)
        })
        newTick = map(tick.symbol)
        _ <- onTick(newTick)
      } yield ()
    }

    for {
      active <- activeRef.get
      _ <- if (active) handleTick() else IO.unit
    } yield ()
  }

  def updateOrder(order: Order): IO[Unit] = {
    def addOrder(): IO[Unit] = {
      IO(order.isActive).ifM({
        for {
          _ <- activeOrderMapRef.update(m => m + (order.id -> order))
        } yield ()
      }, IO.unit)
    }

    for {
      active <- activeRef.get
      _ <- if (active) addOrder() *> onOrder(order) else IO.unit
    } yield ()
  }

  def updateTrade(trade: Trade): IO[Unit] = {
    for {
      active <- activeRef.get
      _ <- if (active) onTrade(trade) else IO.unit
    } yield ()
  }

  def updateTimer(): IO[Unit] = {
    for {
      active <- activeRef.get
      _ <- if (active) onTimer() else IO.unit
    } yield ()
  }

  def subscribe(symbol: String): IO[Unit] = ???

  def cancelSubscribe(symbol: String): IO[Unit] = ???

  def sendOrder(symbol: String, direction: Direction, price: Double, volume: Int,
                stop: Boolean): IO[Long] = {
    val orderType = if (stop) OrderType.LMT else OrderType.MKT
    if (direction == Direction.BUY) {
      buy(symbol, price, volume, orderType)
    } else {
      sell(symbol, price, volume, orderType)
    }
  }

  def buy(symbol: String, price: Double, volume: Int, orderType: OrderType): IO[Long] = {
    // TODO: logging
    algoEngine.sendOrder(algoName, symbol, Direction.BUY, price, volume, orderType)
  }

  def sell(symbol: String, price: Double, volume: Int, orderType: OrderType): IO[Long] = {
    // TODO: logging
    algoEngine.sendOrder(algoName, symbol, Direction.SELL, price, volume, orderType)
  }

  def getAllActiveOrders: IO[List[Order]] = ???

  def getTick(symbol: String): IO[Tick] = algoEngine.getTick(symbol)

  def getContract(symbol: String): IO[Contract] = ???

  def getAsset: IO[Asset] = ???

  def getPosition(positionId: String): IO[Position] = ???

  def getAllPositions: IO[List[Position]] = ???

  def getBars(symbol: String, barType: BarType, limit: Int): IO[List[Bar]] = ???

  def getSymbolBarMap(symbols: NonEmptyList[String], barType: BarType,
                      limit: Int): IO[Map[String, List[Bar]]] = ???

}
