package com.tquant.core.event

import enumeratum._

sealed trait EventType extends EnumEntry

object EventType extends Enum[EventType] {
  val values = findValues

  /**
   * Timer event, sent every 1 second
   */
  case object EVENT_TIMER extends EventType

  /**
   * Log event
   */
  case object EVENT_LOG extends EventType

  /**
   * Tick market return
   */
   case object EVENT_TICK extends EventType

  /**
   * Bar event
   */
   case object EVENT_BAR extends EventType

  /**
   * Trade event
   */
   case object EVENT_TRADE extends EventType

  /**
   * Order event
   */
   case object EVENT_ORDER extends EventType

  /**
   * Assets return
   */
   case object EVENT_ASSET extends EventType

  /**
   * Position return
   */
   case object EVENT_POSITION extends EventType

  /**
   * Contract return
   */
   case object EVENT_CONTRACT extends EventType

  /**
   * Account return
   */
   case object EVENT_ACCOUNT extends EventType

  /**
   * Algo event
   */
   case object EVENT_ALGO extends EventType

  /**
   * Error return
   */
   case object EVENT_ERROR extends EventType
}
