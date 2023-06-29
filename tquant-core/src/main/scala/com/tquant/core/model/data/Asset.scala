package com.tquant.core.model.data

import com.tquant.core.event.EventData

case class Asset(account: String, capability: String, netLiquidation: Double,
                 availableFunds: Double, initMarginReq: Double,
                 maintenanceMarginReq: Double, buyingPower: Double,
                 grossPositionValue: Double, realizedPnL: Double,
                 unrealizedPnL: Double) extends EventData
