package com.tquant.core.model.enums

import enumeratum._

sealed trait BarType extends EnumEntry

// TODO
object BarType extends Enum[BarType] {
  val values = findValues

}
