package com.tquant.core.model.request

case class ModifyRequest(orderId: Long, symbol: String, limitPrice: Double,
                         quantity: Int, exchange: String)
