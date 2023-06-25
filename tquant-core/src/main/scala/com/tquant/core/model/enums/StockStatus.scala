package com.tquant.core.model.enums

import enumeratum._

sealed trait StockStatus extends EnumEntry

// TODO
object StockStatus extends Enum[StockStatus] {
  val values = findValues


}
