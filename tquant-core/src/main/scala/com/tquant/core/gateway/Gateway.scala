package com.tquant.core.gateway

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import com.tquant.core.TigerQuantException
import com.tquant.core.event.{Event, EventData, EventEngine, EventType}
import com.tquant.core.model.data.{Asset, Bar, Contract, Order, Position, Tick, Trade}
import com.tquant.core.model.enums.BarType
import com.tquant.core.model.request.{ModifyRequest, OrderRequest, SubscribeRequest}

abstract class Gateway(eventEngine: EventEngine, name: String) {

  def connect(): IO[Unit]

  def disconnect(): IO[Unit]

  def subscribe(request: SubscribeRequest): IO[Unit]

  def cancelSubscribe(request: SubscribeRequest): IO[Unit]

  def sendOrder(request: OrderRequest): EitherT[IO, TigerQuantException, Long]

  def cancelOrder(request: ModifyRequest): EitherT[IO, TigerQuantException, Unit]

  def modifyOrder(request: ModifyRequest): EitherT[IO, TigerQuantException, Long]

  def getBars(symbols: NonEmptyList[String], barType: BarType,
              limit: Int): EitherT[IO, TigerQuantException, Map[String, List[Bar]]]

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
