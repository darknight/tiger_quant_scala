package com.tquant.core.model.data

import com.tquant.core.model.enums.BarType

import java.time.LocalDateTime
import scala.concurrent.duration._

case class Bar(symbol: String, period: String, duration: Duration,
               open: Double, close: Double, high: Double, low: Double,
               volume: Long, amount: Double, time: LocalDateTime)

object Bar {

  // TODO
  def getDurationByKType(barType: BarType): Duration = {
    barType match {
      case _ => 1.days
    }
  }
}