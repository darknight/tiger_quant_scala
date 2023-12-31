package com.tquant.core.model.data

import com.tquant.core.event.EventData
import com.tquant.core.model.enums.SecType

case class Contract(identifier: String, name: String, symbol: String, secType: String,
                    currency: String, exchange: String, market: String, expiry: String,
                    contractMonth: String, strike: Double, multiplier: Double, right: String,
                    minTick: Double, lotSize: Int) extends EventData {

  def isOption: Boolean = secType.equalsIgnoreCase(SecType.OPT.entryName)
}

object Contract {
  def empty: Contract = Contract("", "", "", "", "", "", "", "", "", 0.0, 0.0, "", 0.0, 0)

  def apply(symbol: String, secType: SecType): Contract = {
    val contract = empty
    contract.copy(
      symbol = symbol,
      secType = secType.entryName
    )
  }

  def apply(symbol: String, secType: SecType, right: String): Contract = {
    val contract = empty
    contract.copy(
      symbol = symbol,
      secType = secType.entryName,
      right = right
    )
  }

  def apply(symbol: String, secType: SecType, right: String, expiry: String, strike: Double): Contract = {
    val contract = empty
    contract.copy(
      symbol = symbol,
      secType = secType.entryName,
      right = right,
      expiry = expiry,
      strike = strike
    )
  }
}
