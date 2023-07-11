package com.tquant.gateway.tiger

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.tigerbrokers.stock.openapi.client.socket.ApiComposeCallback
import com.tigerbrokers.stock.openapi.client.socket.data.TradeTick
import com.tigerbrokers.stock.openapi.client.socket.data.pb.{AssetData, OrderStatusData, OrderTransactionData, PositionData, QuoteBBOData, QuoteBasicData, QuoteDepthData}
import com.tigerbrokers.stock.openapi.client.struct.SubscribedSymbol
import com.tquant.core.gateway.Gateway
import com.tquant.core.log.logging
import com.tquant.core.model.data.{Order, Tick}
import org.typelevel.log4cats.LoggerFactory

class TigerSubscribeApi(gateway: Gateway) extends ApiComposeCallback {

  private val logger = LoggerFactory[IO].getLogger

  // TODO: call gateway.onLog to send log event
  override def error(errorMsg: String): Unit = {}

  override def error(id: Int, errorCode: Int, errorMsg: String): Unit = {}

  override def connectionClosed(): Unit = {}

  override def connectionKickout(errorCode: Int, errorMsg: String): Unit = {}

  override def connectionAck(): Unit = {}

  override def connectionAck(serverSendInterval: Int, serverReceiveInterval: Int): Unit = {}

  override def hearBeat(heartBeatContent: String): Unit = {}

  override def serverHeartBeatTimeOut(channelId: String): Unit = {}

  override def orderStatusChange(data: OrderStatusData): Unit = {
    val order = Order(
      account = data.getAccount,
      averageFilledPrice = data.getAvgFillPrice,
      direction = data.getAction,
      id = data.getId,
      orderType = data.getOrderType,
      symbol = data.getSymbol,
      volume = data.getTotalQuantity,
      filledVolume = data.getFilledQuantity,
      status = data.getStatus,
      gatewayName = "TigerGateway",
      name = "",
      identifier = "",
      price = 0.0,
      remark = "",
      time = 0
    )
    // FIXME: use dedicated IORuntime
    gateway.onOrder(order).unsafeRunSync()
  }

  override def orderTransactionChange(data: OrderTransactionData): Unit = {}

  override def positionChange(data: PositionData): Unit = {}

  override def assetChange(data: AssetData): Unit = {}

  override def tradeTickChange(data: TradeTick): Unit = {}

  override def quoteChange(data: QuoteBasicData): Unit = {
    val empty = Tick.empty
    val tick = empty.copy(
      symbol = data.getSymbol,
      identifier = data.getIdentifier,
      volume = data.getVolume,
      latestPrice = data.getLatestPrice,
      amount = data.getAmount,
      open = data.getOpen,
      high = data.getHigh,
      low = data.getLow,
      preClose = data.getPreClose
    )
    // FIXME: use dedicated IORuntime
    gateway.onTick(tick).unsafeRunSync()
  }

  override def quoteAskBidChange(data: QuoteBBOData): Unit = {}

  override def optionChange(data: QuoteBasicData): Unit = {}

  override def optionAskBidChange(data: QuoteBBOData): Unit = {}

  override def futureChange(data: QuoteBasicData): Unit = {}

  override def futureAskBidChange(data: QuoteBBOData): Unit = {}

  override def depthQuoteChange(data: QuoteDepthData): Unit = {}

  override def subscribeEnd(id: Int, subject: String, result: String): Unit = {}

  override def cancelSubscribeEnd(id: Int, subject: String, result: String): Unit = {}

  override def getSubscribedSymbolEnd(subscribedSymbol: SubscribedSymbol): Unit = {}
}
