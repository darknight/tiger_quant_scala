package com.tquant.core.engine
import cats.effect.IO

class OrderEngine extends Engine {
  val engineName: String = "OrderEngine"

  def start(): IO[Unit] = ???

  def stop(): IO[Unit] = ???
}
