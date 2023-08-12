package com.tquant.core.model.data

import com.tquant.core.event.EventData
import com.tquant.core.model.enums.SecType

case class Position(contract: Contract, account: String, symbol: String, secType: String,
                    right: String, identifier: String, exchange: String, direction: String,
                    position: Int, averageCost: Double, marketPrice: Double, marketValue: Double,
                    realizedPnl: Double, unrealizedPnl: Double) extends EventData {

  def isStock: Boolean = secType.equalsIgnoreCase(SecType.STK.entryName)

  def isOption: Boolean = secType.equalsIgnoreCase(SecType.OPT.entryName)

  def isOptionForRight(rightStr: String): Boolean = isOption && right.equalsIgnoreCase(rightStr)

  def isOptionCall: Boolean = isOptionForRight("CALL")

  def isOptionPut: Boolean = isOptionForRight("PUT")

  def positionPnl: Double = {
    val multiplier = if (contract.multiplier > 0.0) contract.multiplier else 1.0
    unrealizedPnl / Math.abs(averageCost * position * multiplier)
  }
}
