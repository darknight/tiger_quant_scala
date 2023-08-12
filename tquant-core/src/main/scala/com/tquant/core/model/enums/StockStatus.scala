package com.tquant.core.model.enums

import enumeratum._

sealed trait StockStatus extends EnumEntry

// TODO
object StockStatus extends Enum[StockStatus] {
  val values = findValues

  case object UNKNOWN extends StockStatus
  case object NORMAL extends StockStatus
  case object HALTED extends StockStatus
  case object DELIST extends StockStatus
  case object NEW extends StockStatus
  case object ALTER extends StockStatus
}
