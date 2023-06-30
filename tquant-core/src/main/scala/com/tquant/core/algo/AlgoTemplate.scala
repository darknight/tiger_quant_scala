package com.tquant.core.algo

import cats._
import cats.data.NonEmptyList
import cats.implicits._
import cats.effect.{IO, Ref}
import cats.effect.std.AtomicCell
import com.tquant.core.model.data.{Asset, Bar, Contract, Order, Position, Tick, Trade}
import com.tquant.core.model.enums.{BarType, Direction, OrderType}

abstract class AlgoTemplate(algoEngine: AlgoEngine) {

  private val algoName = this.getClass.getSimpleName

  private val activeIO = AtomicCell[IO].of(false)
  private val activeOrderMapIO = Ref.of[IO, Map[Long, Order]](Map.empty)
  private val tickMapIO = Ref.of[IO, Map[String, Tick]](Map.empty)

  def onBar(bar: Bar): IO[Unit]
  def onStart(): IO[Unit]
  def onStop(): IO[Unit]
  def onTimer(): IO[Unit]
  def onTick(tick: Tick): IO[Unit]
  def onOrder(order: Order): IO[Unit]
  def onTrade(trade: Trade): IO[Unit]

  def start(): IO[Unit] = {
    for {
      cell <- activeIO
      _ <- cell.set(true)
      _ <- onStart()
    } yield ()
  }

  def stop(): IO[Unit] = {
    for {
      cell <- activeIO
      _ <- cell.set(false)
      _ <- cancelAll()
      _ <- onStop()
      // TODO: logging
    } yield ()
  }

  def cancelAll(): IO[Unit] = {
    for {
      ref <- activeOrderMapIO
      map <- ref.get
      _ <- map.keys.toList.map(algoEngine.cancelOrder).sequence_
    } yield ()
  }

  def updateTick(tick: Tick): IO[Unit] = {
    def handleTick(): IO[Unit] = {
      for {
        ref <- tickMapIO
        map <- ref.updateAndGet(map => {
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
      activeCell <- activeIO
      active <- activeCell.get
      _ <- if (active) handleTick() else IO.unit
    } yield ()
  }

  def updateOrder(order: Order): IO[Unit] = {
    def addOrder(): IO[Unit] = {
      IO(order.isActive).ifM({
        for {
          ref <- activeOrderMapIO
          _ <- ref.update(m => m + (order.id -> order))
        } yield ()
      }, IO.unit)
    }

    for {
      activeCell <- activeIO
      active <- activeCell.get
      _ <- if (active) addOrder() *> onOrder(order) else IO.unit
    } yield ()
  }

  def updateTrade(trade: Trade): IO[Unit] = {
    for {
      activeCell <- activeIO
      active <- activeCell.get
      _ <- if (active) onTrade(trade) else IO.unit
    } yield ()
  }

  def updateTimer(): IO[Unit] = {
    for {
      activeCell <- activeIO
      active <- activeCell.get
      _ <- if (active) onTimer() else IO.unit
    } yield ()
  }

  def subscribe(symbol: String): IO[Unit] = ???

  def cancelSubscribe(symbol: String): IO[Unit] = ???

  def sendOrder(symbol: String, direction: Direction, price: Double, volume: Int,
                stop: Boolean): IO[Unit] = IO.unit

  def buy(symbol: String, price: Double, volume: Int, orderType: OrderType): IO[Unit] = {
    // TODO: logging
    algoEngine.sendOrder(algoName, symbol, Direction.BUY, price, volume, orderType)
  }

  def sell(symbol: String, price: Double, volume: Int, orderType: OrderType): IO[Unit] = {
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
