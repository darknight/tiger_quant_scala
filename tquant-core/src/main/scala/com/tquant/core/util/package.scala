package com.tquant.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

package object util {

  val YYYY_MM_DD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
  val YYYY_MM_DD_SEP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def optionDte(expiration: String): Int = {
    val today = LocalDate.now()
    val contractDate = expiration.length match {
      case 8 => LocalDate.parse(expiration, YYYY_MM_DD_FORMAT)
      case 10 => LocalDate.parse(expiration, YYYY_MM_DD_SEP_FORMAT)
      case _ => LocalDate.now()
    }
    java.time.Duration.between(today.atStartOfDay(), contractDate.atStartOfDay()).toDays.toInt
  }

}
