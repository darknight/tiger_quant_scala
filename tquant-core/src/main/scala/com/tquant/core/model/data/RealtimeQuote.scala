package com.tquant.core.model.data

import com.tquant.core.model.enums.StockStatus

case class RealtimeQuote(symbol: String, open: Double, high: Double, low: Double, close: Double,
                         preClose: Double, latestPrice: Double, latestTime: Long,
                         askPrice: Double, askSize: Long, bidPrice: Double, bidSize: Long,
                         volume: Long, status: StockStatus, hourTrading: HourTrading, openInterest: Int)
