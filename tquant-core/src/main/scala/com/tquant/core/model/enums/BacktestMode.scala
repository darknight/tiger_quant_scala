package com.tquant.core.model.enums

import enumeratum._

sealed trait BacktestMode extends EnumEntry
object BacktestMode extends Enum[BacktestMode] {
  val values = findValues

  case object BAR extends BacktestMode
  case object TICK extends BacktestMode
}
