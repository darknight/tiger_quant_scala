package com.tquant.core.model.data

import com.tquant.core.event.EventData

import java.time.LocalDateTime

case class Tick(contract: Contract, identifier: String, symbol: String, name: String, `type`: String,
                volume: Long, amount: Double, latestPrice: Double, latestVolume: Double,
                latestTime: LocalDateTime, time: Long, openInterest: Int,
                open: Double, close: Double, high: Double, low: Double, preClose: Double,
                bidPrice: Double, bidSize: Int, askPrice: Double, askSize: Int, midpoint: Double
               ) extends EventData {

  // TODO
}

object Tick {
  // TODO
}