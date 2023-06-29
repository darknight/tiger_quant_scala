package com.tquant.core.model.data

import com.tquant.core.event.EventData

import java.time.LocalDateTime

case class Trade(id: String, orderId: Long, account: String, symbol: String,
                 exchange: String, direction: String, orderType: String,
                 price: Double, volume: Long, status: String, time: LocalDateTime) extends EventData
