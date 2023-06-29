package com.tquant.gateway.tiger

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import cats.effect.kernel.Sync
import com.tigerbrokers.stock.openapi.client.https.client.TigerHttpClient
import com.tquant.core.TigerQuantException
import com.tquant.core.model.data.{Asset, Contract, Order}

class TigerTradeApi[F[_]](private val client: TigerHttpClient)(implicit f: Sync[F]) {

  def getOrderId(): EitherT[F, TigerQuantException, Int] = ???

  def placeLimitOrder(contract: Contract, actionType: String, price: Double,
                      quantity: Int): EitherT[F, TigerQuantException, Long] = ???

  def placeMarketOrder(contract: Contract, actionType: String,
                       quantity: Int): EitherT[F, TigerQuantException, Long] = ???

  def cancelOrder(id: Long): EitherT[F, TigerQuantException, String] = ???

  def modifyOrder(id: Long, limitPrice: Double, quantity: Int): EitherT[F, TigerQuantException, String] = ???

  def getOrder(id: Long): EitherT[F, TigerQuantException, Order] = ???

  def getOrders: EitherT[F, TigerQuantException, List[Order]] = ???

  def getCancelledOrders: EitherT[F, TigerQuantException, List[Order]] = ???

  def getOpenOrders: EitherT[F, TigerQuantException, List[Order]] = ???

  def getFilledOrders: EitherT[F, TigerQuantException, List[Order]] = ???

  def getOrdersByServiceType(serviceType: String, secType: String): EitherT[F, TigerQuantException, List[Order]] = ???

  def getAccounts: EitherT[F, TigerQuantException, List[String]] = ???

  def getAsset(secType: String): EitherT[F, TigerQuantException, Asset] = ???

  def getPositions: EitherT[F, TigerQuantException, SymbolPositionMap] = ???

  def getContracts(secType: String): EitherT[F, TigerQuantException, List[Contract]] = ???

  def getContracts(symbols: NonEmptyList[String], secType: String): EitherT[F, TigerQuantException, List[Contract]] = ???
}
