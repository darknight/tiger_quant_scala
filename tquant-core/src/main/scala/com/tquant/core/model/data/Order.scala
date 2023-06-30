package com.tquant.core.model.data

import com.tquant.core.event.EventData

case class Order(id: Long, gatewayName: String, name: String, account: String,
                 symbol: String, identifier: String, direction: String,
                 orderType: String, price: Double, volume: Long,
                 averageFilledPrice: Double, filledVolume: Long,
                 status: String, remark: String, time: Long) extends EventData {
  // TODO
  def isActive: Boolean = Order.activeStatus.contains(status.toLowerCase)
}

object Order {

  private val activeStatus = Set("initial", "pendingsubmit", "submitted", "pendingcancel")

}
