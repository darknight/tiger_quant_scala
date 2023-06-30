package com.tquant.core.model.enums

import enumeratum._

sealed trait OrderType extends EnumEntry
object OrderType extends Enum[OrderType] {
  val values = findValues

  /**
   * Market order
   */
  case object MKT extends OrderType

  /**
   * Limit order
   */
  case object LMT extends OrderType
}
