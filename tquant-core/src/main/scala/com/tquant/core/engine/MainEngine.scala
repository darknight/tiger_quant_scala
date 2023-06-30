package com.tquant.core.engine

import cats.effect.IO

class MainEngine extends Engine {

  val engineName = "MainEngine"

  def start(): IO[Unit] = ???

  def stop(): IO[Unit] = ???

}
