package com.tquant.core.config

import cats.effect.IO
import pureconfig.ConfigReader.Result
import pureconfig._
import pureconfig.generic.auto._

case class Jdbc(url: String, user: String, password: String)
case class Log(enable: Boolean, level: String, console: Boolean, file: Boolean, path: String)
case class Storage(enable: Boolean)
case class Subscribe(enable: Boolean)
case class Contract(loadEnable: Boolean)
case class TigerGateway(apiLogEnable: Boolean, apiLogPath: String,
                        tigerId: String, account: String, privateKey: String)
case class EventEngine(capacity: Int)

case class ServerConf(jdbc: Jdbc, log: Log, storage: Storage,
                      subscribe: Subscribe, contract: Contract,
                      tigerGateway: TigerGateway, eventEngine: EventEngine)

object ConfigLoader {

  def loadConfig(): IO[Result[ServerConf]] = {
    IO.delay(ConfigSource.default.load[ServerConf])
  }
}
