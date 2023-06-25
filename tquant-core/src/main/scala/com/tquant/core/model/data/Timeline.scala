package com.tquant.core.model.data

case class TimelinePoint(price: Double, avgPrice: Double, time: Long, volume: Long)

case class TimelineRange(items: List[TimelinePoint], beginTime: Long, endTime: Long)

/**
 *
 * @param symbol stock code
 * @param period day (e.g. 5day)
 * @param preClose close price of yesterday
 * @param intraday price data (no `endTime` for current day)
 * @param preMarket before open (US only)
 * @param afterHours after close (US only)
 */
case class TimelineQuote(symbol: String, period: String, preClose: Double,
                        intraday: TimelineRange, preMarket: TimelineRange, afterHours: TimelineRange)
