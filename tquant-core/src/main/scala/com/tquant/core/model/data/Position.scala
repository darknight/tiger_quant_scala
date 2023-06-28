package com.tquant.core.model.data

import com.tquant.core.model.enums.SecType

case class Position(contract: Contract, account: String, symbol: String, secType: String,
                    right: String, identifier: String, exchange: String, direction: String,
                    position: Int, averageCost: Double, marketPrice: Double, marketValue: Double,
                    realizedPnl: Double, unrealizedPnl: Double) {

  def isStock: Boolean = secType.equalsIgnoreCase(SecType.STK.entryName)

  def isOption: Boolean = secType.equalsIgnoreCase(SecType.OPT.entryName)

  def isOptionCall: Boolean = isOption && right.equalsIgnoreCase("CALL")

  def isOptionPut: Boolean = isOption && right.equalsIgnoreCase("PUT")
}
