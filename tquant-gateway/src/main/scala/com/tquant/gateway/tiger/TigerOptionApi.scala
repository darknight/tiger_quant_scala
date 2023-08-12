package com.tquant.gateway.tiger

import cats.data.EitherT
import cats.effect.kernel.Sync
import com.tigerbrokers.stock.openapi.client.https.client.TigerHttpClient
import com.tigerbrokers.stock.openapi.client.https.domain.option.item.{OptionBriefItem, OptionExpirationItem, OptionRealTimeQuoteGroup}
import com.tigerbrokers.stock.openapi.client.struct.enums.Right
import com.tquant.core.TigerQuantException
import com.tquant.core.model.request.OptionChainFilter


class TigerOptionApi[F[_]](private val client: TigerHttpClient)(implicit f: Sync[F]) {

  def getOptionBrief(symbol: String, right: Right, strike: String,
                     expiry: String): EitherT[F, TigerQuantException, List[OptionBriefItem]] = ???

  def getOptionChain(symbol: String, expiry: String,
                     filter: OptionChainFilter): EitherT[F, TigerQuantException, List[OptionRealTimeQuoteGroup]] = ???

  def getOptionExpiration(symbol: String): EitherT[F, TigerQuantException, OptionExpirationItem] = ???
}
