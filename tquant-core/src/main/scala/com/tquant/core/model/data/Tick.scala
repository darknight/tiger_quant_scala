package com.tquant.core.model.data

import com.tquant.core.event.EventData

import java.time.LocalDateTime

case class Tick(contract: Contract, identifier: String, symbol: String, name: String, `type`: String,
                volume: Long, amount: Double, latestPrice: Double, latestVolume: Double,
                latestTime: LocalDateTime, time: Long, openInterest: Int,
                open: Double, close: Double, high: Double, low: Double, preClose: Double,
                bidPrice: Double, bidSize: Int, askPrice: Double, askSize: Int, midpoint: Double
               ) extends EventData {

  def update(t: Tick): Tick = {
    this.copy(
      volume = if (t.volume > 0) t.volume else volume,
      latestPrice = if (t.latestPrice > 0) t.latestPrice else latestPrice,
      latestVolume = if (t.latestVolume > 0) t.latestVolume else latestVolume,
      latestTime = t.latestTime,
      open = if (t.open > 0) t.open else open,
      close = if (t.close > 0) t.close else close,
      high = if (t.high > 0) t.high else high,
      low = if (t.low > 0) t.low else low,
      preClose = if (t.preClose > 0) t.preClose else preClose,
      askPrice = if (t.askPrice > 0) t.askPrice else askPrice,
      askSize = if (t.askSize > 0) t.askSize else askSize,
      bidPrice = if (t.bidPrice > 0) t.bidPrice else bidPrice,
      bidSize = if (t.bidSize > 0) t.bidSize else bidSize,
      amount = if (t.amount > 0) t.amount else amount
    )
  }
}

object Tick {
  // TODO
}