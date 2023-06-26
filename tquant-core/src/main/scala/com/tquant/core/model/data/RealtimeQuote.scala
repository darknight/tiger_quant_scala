package com.tquant.core.model.data

import com.tquant.core.model.enums.StockStatus

/**
 *
 * @param symbol
 * @param open
 * @param high
 * @param low
 * @param close
 * @param preClose
 * @param latestPrice
 * @param latestTime
 * @param askPrice
 * @param askSize
 * @param bidPrice
 * @param bidSize
 * @param volume
 * @param status
 * @param hourTrading
 * @param openInterest
 */
case class RealtimeQuote(symbol: String, open: Double, high: Double, low: Double, close: Double,
                         preClose: Double, latestPrice: Double, latestTime: Long,
                         askPrice: Double, askSize: Long, bidPrice: Double, bidSize: Long,
                         volume: Long, status: StockStatus, hourTrading: HourTrading, openInterest: Int)
