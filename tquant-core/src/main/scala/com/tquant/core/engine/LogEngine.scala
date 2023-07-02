package com.tquant.core.engine
import cats.effect.IO

class LogEngine extends Engine {

  val engineName: String = "LogEngine"

  def start(): IO[Unit] = ???
  def stop(): IO[Unit] = ???
}
