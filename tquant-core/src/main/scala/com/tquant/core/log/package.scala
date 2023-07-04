package com.tquant.core

import cats.effect.IO
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

package object log {
  implicit val logging: LoggerFactory[IO] = Slf4jFactory[IO]
}
