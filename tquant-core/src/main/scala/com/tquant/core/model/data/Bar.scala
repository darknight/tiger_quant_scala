package com.tquant.core.model.data

import com.tquant.core.model.enums.BarType

import java.time.LocalDateTime
import scala.concurrent.duration._

// TODO: define `duration` as Duration, update BarDAO
case class Bar(symbol: String, duration: Long, period: String,
               open: Double, high: Double, low: Double, close: Double,
               volume: Long, amount: Double, time: LocalDateTime)

object Bar {

  // TODO
  def getDurationByKType(barType: BarType): Duration = {
    barType match {
      case _ => 1.days
    }
  }
}
