package com.tquant.algo.thetagang

case class Account(accountId: String = "", cancelOrder: Boolean = false, marginUsage: Double = 0.0)

/**
 * @param expiration - required, must be positive
 * @param strike
 */
case class OptionChain(expiration: Int = 0, strike: Int = 0)

case class RollCall(isItm: Boolean = false, creditOnly: Boolean = false)
case class RollPut(isItm: Boolean = false, creditOnly: Boolean = false)
case class RollWhen(pnl: Double, dte: Int, minDte: Int, maxDte: Int,
                    closeAtPnl: Double, minPnl: Double, call: RollCall, put: RollPut)

case class WriteCall(green: Boolean = false, capFactor: Double = 0.0)
case class WritePut(red: Boolean = false)
case class WriteWhen(call: WriteCall, put: WritePut)

case class Target(dte: Int = 0, delta: Double = 0.0,
                  maxNewContractPercent: Double = 0.0, minOpenInterest: Int = 0)

case class SymbolCall(delta: Double = 0.0, strikeLimit: Double = 0.0)
case class SymbolPut(delta: Double = 0.0, strikeLimit: Double = 0.0)
case class Symbol(weight: Double, put: SymbolPut, call: SymbolCall, primaryExchange: String, delta: Double)

case class RollingSellPutConfig(enable: Boolean, account: Account, optionChain: OptionChain,
                                rollWhen: RollWhen, writeWhen: WriteWhen, target: Target,
                                symbols: Map[String, Symbol])

object RollingSellPutConfig {
  def empty: RollingSellPutConfig = RollingSellPutConfig(
    false, Account(), OptionChain(),
    RollWhen(0.0, 0, 0, 0, 0.0, 0.0, RollCall(), RollPut()),
    WriteWhen(WriteCall(), WritePut()),
    Target(),
    Map.empty
  )
}
