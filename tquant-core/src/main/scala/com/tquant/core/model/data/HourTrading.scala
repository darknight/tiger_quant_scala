package com.tquant.core.model.data

case class HourTrading(tag: String, latestPrice: Double, preClose: Double,
                       latestTime: String, volume: Long, timestamp: Long)

object HourTrading {
  def empty: HourTrading = HourTrading("", 0.0, 0.0, "", 0, 0)
}
