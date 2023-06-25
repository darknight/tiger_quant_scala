package com.tquant.core.model.data

/**
 *
 * @param date trading day date，yyyy-MM-dd
 * @param `type` trading day type:NORMAL/EARLY_CLOSE
 */
case class TradeCalendar(date: String, `type`: String)
