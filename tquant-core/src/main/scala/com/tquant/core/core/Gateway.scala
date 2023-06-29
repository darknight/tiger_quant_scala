package com.tquant.core.core

import cats.data.NonEmptyList
import cats.effect.IO
import com.tquant.core.event.{Event, EventData, EventEngine, EventType}
import com.tquant.core.model.data.{Asset, Bar, Contract, Order, Position, Tick, Trade}
import com.tquant.core.model.enums.BarType
import com.tquant.core.model.request.{ModifyRequest, OrderRequest, SubscribeRequest}

abstract class Gateway(eventEngine: EventEngine, name: String) {

  def connect(): IO[Unit]

  def subscribe(request: SubscribeRequest): IO[Unit]

  def cancelSubscribe(request: SubscribeRequest): IO[Unit]

  def sendOrder(request: OrderRequest): IO[Long]

  def cancelOrder(request: ModifyRequest): IO[Unit]

  def modifyOrder(request: ModifyRequest): IO[Long]

  def getBars(symbols: NonEmptyList[String], barType: BarType, limit: Int): IO[Map[String, List[Bar]]]

  def onEvent(eventType: EventType, eventData: EventData): IO[Unit] = {
    eventEngine.put(Event(eventType, Some(eventData)))
  }

  def onTick(tick: Tick): IO[Unit] = onEvent(EventType.EVENT_TICK, tick)

  def onTrade(trade: Trade): IO[Unit] = onEvent(EventType.EVENT_TRADE, trade)

  def onOrder(order: Order): IO[Unit] = onEvent(EventType.EVENT_ORDER, order)

  def onPosition(position: Position): IO[Unit] = onEvent(EventType.EVENT_POSITION, position)

  @deprecated("Account class is empty, so this method is useless")
  def onAccount(account: EventData): IO[Unit] = onEvent(EventType.EVENT_ACCOUNT, account)

  def onContract(contract: Contract): IO[Unit] = onEvent(EventType.EVENT_CONTRACT, contract)

  def onAsset(asset: Asset): IO[Unit] = onEvent(EventType.EVENT_ASSET, asset)

  // FIXME: proper logging different info type?
  def log(): IO[Unit] = onEvent(EventType.EVENT_LOG, new EventData {})
}
