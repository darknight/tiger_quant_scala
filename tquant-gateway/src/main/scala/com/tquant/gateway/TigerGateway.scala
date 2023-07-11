package com.tquant.gateway

import cats.data.{EitherT, NonEmptyList}
import cats.effect.{IO, Ref, Resource, Sync}
import cats.implicits._
import com.tigerbrokers.stock.openapi.client.https.client.TigerHttpClient
import com.tigerbrokers.stock.openapi.client.socket.data.TradeTick
import com.tigerbrokers.stock.openapi.client.socket.data.pb.{AssetData, OrderStatusData, OrderTransactionData, PositionData, QuoteBBOData, QuoteBasicData, QuoteDepthData}
import com.tigerbrokers.stock.openapi.client.socket.{ApiComposeCallback, WebSocketClient}
import com.tigerbrokers.stock.openapi.client.struct.SubscribedSymbol
import com.tigerbrokers.stock.openapi.client.struct.enums.Market
import com.tquant.core.TigerQuantException
import com.tquant.core.config.ServerConf
import com.tquant.core.event.EventEngine
import com.tquant.core.gateway.Gateway
import com.tquant.core.log.logging
import com.tquant.core.model.data.{Asset, Bar, Contract, MarketStatus, Order}
import com.tquant.core.model.enums.BarType
import com.tquant.core.model.request.{ModifyRequest, OrderRequest, SubscribeRequest}
import com.tquant.gateway.converter.Converters
import com.tquant.gateway.tiger.{SymbolBarMap, TigerOptionApi, TigerQuoteApi, TigerSubscribeApi, TigerTradeApi}
import com.tquant.storage.DAOInstance
import com.tquant.storage.dao.{BarDAO, ContractDAO}
import doobie.hikari.HikariTransactor
import org.typelevel.log4cats.LoggerFactory

// TODO: init `WebSocketClient` & `TigerSubscribeApi`

/**
 * `TigerGateway` is responsible to talk to tiger API, fire event to `EventEngine`
 * @param conf
 * @param eventEngine
 * @param xaRes
 * @param contractMapRef
 * @param orderMapRef
 * @param openOrderMapRef
 * @param assetMapRef
 */
class TigerGateway(conf: ServerConf,
                   eventEngine: EventEngine,
                   xaRes: Resource[IO, HikariTransactor[IO]],
                   contractMapRef: Ref[IO, Map[String, Contract]],
                   orderMapRef: Ref[IO, Map[Long, Order]],
                   openOrderMapRef: Ref[IO, Map[Long, Order]],
                   assetMapRef: Ref[IO, Map[String, Asset]]) extends Gateway(eventEngine) {

  val name: String = getClass.getSimpleName
  val logger = LoggerFactory[IO].getLogger

  private val clientConf = Converters.toClientConfig(conf)
  private val httpClient = TigerHttpClient.getInstance().clientConfig(clientConf)
  private val contractDAO = new ContractDAO(xaRes)
  private val barDAO = new BarDAO(xaRes)

  val tradeApi = new TigerTradeApi[IO](httpClient)
  val quoteApi = new TigerQuoteApi[IO](httpClient)
  val optionApi = new TigerOptionApi[IO](httpClient)
  val subscribeApi = new TigerSubscribeApi(this)
  val socketClient = WebSocketClient
    .getInstance()
    .clientConfig(clientConf)
    .apiComposeCallback(subscribeApi)

  override def connect(): IO[Boolean] = {
    // FIXME
//    for {
//      _ <- queryContract()
//    } yield ()

    for {
      resp <- quoteApi.grabQuotePermission().value
      apiRes <- resp match {
        case Left(e) =>
          logger.error(s"grab quote perm failed => $e") *> IO.pure(false)
        case Right(value) =>
          logger.info(s"grab quote resp => $value") *> IO.pure(true)
      }
      wsRes <- Sync[IO].blocking(socketClient.connect()).attempt.flatMap {
        case Left(err) =>
          logger.error(s"websocket connection error => $err") *> IO.pure(false)
        case Right(_) =>
          logger.info(s"websocket connection succeed") *> IO.pure(true)
      }
    } yield apiRes && wsRes
  }

  override def disconnect(): IO[Unit] = ???

  override def subscribe(request: SubscribeRequest): IO[Unit] = ???

  override def cancelSubscribe(request: SubscribeRequest): IO[Unit] = ???

  override def sendOrder(request: OrderRequest): EitherT[IO, TigerQuantException, Long] = {
    val contractIO = for {
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

  override def getMarketStatus(market: String): IO[List[MarketStatus]] = {
    for {
      resp <- quoteApi.getMarketState(Market.valueOf(market)).value
      res <- resp match {
        case Left(e) =>
          logger.error(s"fetch market status failed => $e") *> IO.pure(List.empty)
        case Right(value) =>
          logger.info(s"fetch market status success => $value") *> IO.pure(value)
      }
    } yield res
  }

  def queryContract(): IO[Unit] = {
    // TODO: enable flag
    // TODO: logging
    val contractsIO = contractDAO.queryContracts()
    val res = contractsIO.map(contracts => contracts.map(contract => {
      for {
        _ <- contractMapRef.update(m => m + (contract.identifier -> contract))
        _ <- onContract(contract)
      } yield ()
    }))
    res.flatMap(_.sequence_)
  }

  def queryBars(symbol: String, limit: Int): IO[List[Bar]] = {
    barDAO.queryBars(symbol, limit)
  }
}

object TigerGateway {
  def apply(conf: ServerConf, eventEngine: EventEngine): IO[TigerGateway] = {
    for {
      contractMapRef <- Ref.of[IO, Map[String, Contract]](Map.empty)
      orderMapRef <- Ref.of[IO, Map[Long, Order]](Map.empty)
      openOrderMapRef <- Ref.of[IO, Map[Long, Order]](Map.empty)
      assetMapRef <- Ref.of[IO, Map[String, Asset]](Map.empty)
      xaRes = DAOInstance.createXaRes(conf)
      gateway = new TigerGateway(
        conf,
        eventEngine,
        xaRes,
        contractMapRef,
        orderMapRef,
        openOrderMapRef,
        assetMapRef
      )
    } yield gateway
  }
}
