package com.tquant.core.engine

import cats.effect.IO

trait Engine {
  def engineName: String
  def start(): IO[Unit]
  def stop(): IO[Unit]
}
