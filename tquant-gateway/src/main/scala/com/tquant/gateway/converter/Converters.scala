package com.tquant.gateway.converter

import com.tigerbrokers.stock.openapi.client.https.domain.contract.item.ContractItem
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.{FutureContractItem, FutureKlineItem}
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.{KlinePoint, MarketItem, RealTimeQuoteItem, SymbolNameItem, TimelineItem, TimelinePoint => SDKTimelinePoint, TradeCalendar => SDKTradeCalendar}
import com.tigerbrokers.stock.openapi.client.struct.enums.{FutureKType, KType}
import com.tigerbrokers.stock.openapi.client.util.SymbolUtil
import com.tquant.core.model.data.{Bar, Contract, MarketStatus, RealtimeQuote, SymbolName, TimelinePoint, TimelineQuote, TradeCalendar}
import com.tquant.core.model.enums.{BarType, SecType}

import java.time.{Instant, ZoneId}

object Converters {

  def getZoneId(symbol: String): String = {
    if(SymbolUtil.isUsStockSymbol(symbol)) "America/New_York" else "Asia/Shanghai"
  }

  def toBars(symbol: String, barType: BarType, klinePoints: List[KlinePoint]): List[Bar] = {
    klinePoints.map { point =>
      val time = Instant.ofEpochMilli(point.getTime).atZone(ZoneId.of(getZoneId(symbol))).toLocalDateTime
      val duration = Bar.getDurationByKType(barType)
      Bar(
        symbol = symbol,
        period = barType.entryName,
        duration = duration,
        open = point.getOpen,
        close = point.getClose,
        high = point.getHigh,
        low = point.getLow,
        volume = point.getVolume,
        amount = 0.0,
        time = time)
    }
  }

  // TODO
  def toFutureBars(symbol: String, barType: BarType, klineItem: List[FutureKlineItem]): List[Bar] = ???

  def toKType(barType: BarType): KType = KType.valueOf(barType.entryName)

  def toFutureKType(barType: BarType): FutureKType = FutureKType.valueOf(barType.entryName)

  def toContracts(contractItems: List[ContractItem]): List[Contract] = {
    contractItems.map(item => Contract(
      identifier = item.getSymbol,
      name = item.getName,
      symbol = item.getSymbol,
      gatewayName = "",
      secType = item.getSecType,
      currency = item.getCurrency,
      exchange = item.getExchange,
      market = item.getMarket,
      expiry = item.getExpiry,
      contractMonth = item.getContractMonth,
      strike = item.getStrike,
      multiplier = item.getMultiplier,
      right = item.getRight,
      minTick = 0.0,
      lotSize = 0
    ))
  }

  def toFutureContracts(contractItems: List[FutureContractItem]): List[Contract] = {
    contractItems.filterNot(item => item.getContractMonth == null || item.getContractMonth.isEmpty)
      .map { item =>
        val contractMonthLen = item.getContractCode.length - 4
        val symbol = item.getContractCode.substring(0, contractMonthLen)
        val multiplier = if(item.getMultiplier == null) 0 else item.getMultiplier.doubleValue()
        Contract(
          identifier = item.getContractCode,
          name = item.getName,
          symbol = symbol,
          gatewayName = "",
          secType = SecType.FUT.entryName,
          currency = "",
          exchange = item.getExchangeCode,
          market = "",
          expiry = item.getLastTradingDate,
          contractMonth = item.getContractMonth,
          strike = 0.0,
          multiplier = multiplier,
          right = "",
          minTick = 0.0,
          lotSize = 0
        )
      }
  }

  def toMarketStatuses(marketItems: List[MarketItem]): List[MarketStatus] = {
    marketItems.map(item => MarketStatus(
      market = item.getMarket,
      status = item.getStatus,
      marketStatus = item.getMarketStatus,
      openTime = item.getOpenTime
    ))
  }

  def toTradeCalendars(calendarItems: List[SDKTradeCalendar]): List[TradeCalendar] = {
    calendarItems.map(item => TradeCalendar(
      date = item.getDate,
      `type` = item.getType
    ))
  }

  def toSymbolNames(symbolNameItems: List[SymbolNameItem]): List[SymbolName] = {
    symbolNameItems.map(item => SymbolName(
      symbol = item.getSymbol,
      name = item.getName
    ))
  }

  // TODO
  def toRealtimeQuotes(realtimeQuoteItems: List[RealTimeQuoteItem]): List[RealtimeQuote] = ???

  def toTimelinePoints(timelinePoints: List[SDKTimelinePoint]): List[TimelinePoint] = {
    timelinePoints.map(item => TimelinePoint(
      price = item.getPrice,
      avgPrice = item.getAvgPrice,
      time = item.getTime,
      volume = item.getVolume
    ))
  }

  // TODO
  def toTimelineQuote(timelineItems: List[TimelineItem]): List[TimelineQuote] = ???

}
