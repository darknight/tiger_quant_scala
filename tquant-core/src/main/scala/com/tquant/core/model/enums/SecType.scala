package com.tquant.core.model.enums

import enumeratum._

sealed trait SecType extends EnumEntry

object SecType extends Enum[SecType] {
  val values = findValues

  case object STK extends SecType
  case object OPT extends SecType
  case object WAR extends SecType
  case object IOPT extends SecType
  case object FUT extends SecType
}
