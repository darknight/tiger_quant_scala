package com.tquant.core.model.enums

import enumeratum._

sealed trait Direction extends EnumEntry
object Direction extends Enum[Direction] {
  val values = findValues

  case object BUY extends Direction
  case object SELL extends Direction
}
