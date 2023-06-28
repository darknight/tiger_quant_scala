package com.tquant.core.event

import cats.effect.IO

trait EventHandler {
  def processEvent(event: Event): IO[Unit]
}
