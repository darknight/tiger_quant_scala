package com.tquant.gateway

import cats.data.{EitherT, NonEmptyList}
import cats.effect.std.AtomicCell
import cats.effect.{IO, Ref, Resource}
import cats.implicits._
import com.tigerbrokers.stock.openapi.client.config.ClientConfig
import com.tigerbrokers.stock.openapi.client.https.client.TigerHttpClient
import com.tquant.core.TigerQuantException
import com.tquant.core.config.ServerConf
import com.tquant.core.event.EventEngine
import com.tquant.core.gateway.Gateway
import com.tquant.core.model.data.{Asset, Bar, Contract, Order}
import com.tquant.core.model.enums.BarType
import com.tquant.core.model.request.{ModifyRequest, OrderRequest, SubscribeRequest}
import com.tquant.gateway.converter.Converters
import com.tquant.gateway.tiger.{SymbolBarMap, TigerOptionApi, TigerQuoteApi, TigerTradeApi}
import com.tquant.storage.DAOInstance
import com.tquant.storage.dao.ContractDAO
import doobie.hikari.HikariTransactor

// TODO: init `WebSocketClient` & `TigerSubscribeApi`
class TigerGateway(conf: ServerConf, eventEngine: EventEngine,
                   private val xaRes: Resource[IO, HikariTransactor[IO]]) extends Gateway(eventEngine) {

  val name: String = getClass.getSimpleName
  private val clientConf = Converters.toClientConfig(conf)
  private val httpClient = TigerHttpClient.getInstance().clientConfig(clientConf)
  private val contractDAO = new ContractDAO(xaRes)

  val tradeApi = new TigerTradeApi[IO](httpClient)
  val quoteApi = new TigerQuoteApi[IO](httpClient)
  val optionApi = new TigerOptionApi[IO](httpClient)

  private val contractMapIO = Ref[IO].of(Map.empty[String, Contract])
  private val orderMapIO = Ref[IO].of(Map.empty[Long, Order])
  private val openOrderMapIO = Ref[IO].of(Map.empty[Long, Order])
  private val assetMapIO = Ref[IO].of(Map.empty[String, Asset])

  override def connect(): IO[Unit] = {
    for {
      _ <- queryContract()
    } yield ()

    val res = for {
      resp <- quoteApi.grabQuotePermission()
    } yield resp

    res.value.map {
      case Left(e) => // TODO: log error
      case Right(value) => // TODO: log response
    }
  }

  override def disconnect(): IO[Unit] = ???

  override def subscribe(request: SubscribeRequest): IO[Unit] = ???

  override def cancelSubscribe(request: SubscribeRequest): IO[Unit] = ???

  override def sendOrder(request: OrderRequest): EitherT[IO, TigerQuantException, Long] = {
    val contractIO = for {
      contractMapRef <- contractMapIO
      contractMap <- contractMapRef.get
    } yield contractMap.get(request.symbol)

    val res = contractIO.map {
      case Some(contract) =>
        if (request.price > 0) tradeApi.placeLimitOrder(contract, request.direction, request.price, request.quantity)
        else tradeApi.placeMarketOrder(contract, request.direction, request.quantity)
      case None =>
        val err: EitherT[IO, TigerQuantException, Long] = EitherT.leftT(
          new TigerQuantException(s"No contract found for symbol: ${request.symbol}"))
        err
    }

    EitherT(res.map(_.value).flatten)
  }

  override def cancelOrder(request: ModifyRequest): EitherT[IO, TigerQuantException, Unit] = ???

  override def modifyOrder(request: ModifyRequest): EitherT[IO, TigerQuantException, Long] = ???

  override def getBars(symbols: NonEmptyList[String], barType: BarType,
                       limit: Int): EitherT[IO, TigerQuantException, SymbolBarMap] = ???

  private def queryContract(): IO[Unit] = {
    // TODO: enable flag
    // TODO: logging
    val contractsIO = contractDAO.queryContracts()
    val res = contractsIO.map(contracts => contracts.map(contract => {
      for {
        contractMap <- contractMapIO
        _ <- contractMap.update(m => m + (contract.identifier -> contract))
        _ <- onContract(contract)
      } yield ()
    }))
    res.flatMap(_.sequence_)
  }
}

object TigerGateway {
  def apply(conf: ServerConf, eventEngine: EventEngine): TigerGateway = {
    val xaRes = DAOInstance.createXaRes(conf)
    new TigerGateway(conf, eventEngine, xaRes)
  }
}
