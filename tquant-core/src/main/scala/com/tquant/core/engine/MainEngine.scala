package com.tquant.core.engine

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import com.tquant.core.TigerQuantException
import com.tquant.core.algo.AlgoEngine
import com.tquant.core.event.EventEngine
import com.tquant.core.gateway.Gateway
import com.tquant.core.log.logging
import com.tquant.core.model.data.{Asset, Contract, Order, Position, Tick, Trade}
import com.tquant.core.model.request.{ModifyRequest, OrderRequest, SubscribeRequest}
import org.typelevel.log4cats.LoggerFactory

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainEngine(gateway: Gateway, orderEngine: OrderEngine, algoEngine: AlgoEngine,
                 eventEngine: EventEngine) extends Engine {

  val engineName = "MainEngine"
  val logger = LoggerFactory[IO].getLogger

  private val dateToday = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

  def start(): IO[Unit] = {
    for {
      _ <- gateway.connect().ifM({
        orderEngine.start() *>
          algoEngine.start() *>
          logger.info("event engine start...") *>
          eventEngine.start()
      }, {
        logger.error("gateway init failed, just return...") *>
          IO.unit
      })
    } yield ()
  }

  def stop(): IO[Unit] = {
    for {
      _ <- logger.info(s"$engineName stop...")
      _ <- gateway.disconnect()
      _ <- eventEngine.stop()
      _ <- orderEngine.stop()
      _ <- algoEngine.stop()
    } yield ()
  }
}

object MainEngine {
}
