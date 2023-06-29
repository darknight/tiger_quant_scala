package com.tquant.core.model.request

case class OrderRequest(symbol: String, exchange: String, direction: String,
                        orderType: String, quantity: Int, price: Double)
