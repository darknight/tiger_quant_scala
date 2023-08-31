package com.tquant.algo

import cats.effect.{IO, Ref}
import com.tquant.core.algo.{AlgoEngine, AlgoTemplate}
import com.tquant.core.config.DmaSettings
import com.tquant.core.model.data.{Bar, Order, Tick, Trade}
import com.tquant.core.model.enums.Direction

class DmaAlgo(dmaSetting: DmaSettings,
              algoEngine: AlgoEngine,
              activeRef: Ref[IO, Boolean],
              activeOrderMapRef: Ref[IO, Map[Long, Order]],
              tickMapRef: Ref[IO, Map[String, Tick]])
  extends AlgoTemplate(algoEngine, activeRef, activeOrderMapRef, tickMapRef) {

  private val direction = Direction.withName(dmaSetting.direction.toUpperCase)
  private val price: Double = dmaSetting.price
  private val volume: Int = dmaSetting.volume

  private val orderIdRef = Ref.of[IO, Long](0)

  override def onBar(bar: Bar): IO[Unit] = IO.unit

  override def onStart(): IO[Unit] = IO.unit

  override def onStop(): IO[Unit] = IO.unit

  override def onTimer(): IO[Unit] = IO.unit

  override def onTick(tick: Tick): IO[Unit] = {
    def addOrder(currId: Long): IO[Unit] = {
      if (currId > 0) {
        IO.unit
      }
      else {
        for {
          orderId <- sendOrder(tick.symbol, direction, price, volume, true)
          ref <- orderIdRef
          _ <- ref.set(orderId)
        } yield ()
      }
    }

    for {
      ref <- orderIdRef
      orderId <- ref.get
      _ <- addOrder(orderId)
    } yield ()
  }

  override def onOrder(order: Order): IO[Unit] = {
    if (order.isActive) IO.unit else stop()
  }

  override def onTrade(trade: Trade): IO[Unit] = IO.unit
}
