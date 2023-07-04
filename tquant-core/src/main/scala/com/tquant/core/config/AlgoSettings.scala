package com.tquant.core.config

sealed trait AlgoSettings

case class DmaSettings(direction: String, price: Double, volume: Int) extends AlgoSettings
