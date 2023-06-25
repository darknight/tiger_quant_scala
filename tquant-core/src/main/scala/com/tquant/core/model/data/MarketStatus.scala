package com.tquant.core.model.data

/**
 *
 * @param market US, CN, HK
 * @param status NOT_YET_OPEN, PRE_HOUR_TRADING, TRADING, MIDDLE_CLOSE, POST_HOUR_TRADING, CLOSING, EARLY_CLOSED, MARKET_CLOSED
 * @param marketStatus
 * @param openTime MM-dd HH:mm:ss z
 */
case class MarketStatus(market: String, status: String, marketStatus: String, openTime: String)
