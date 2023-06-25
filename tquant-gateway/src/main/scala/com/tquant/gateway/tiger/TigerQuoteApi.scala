package com.tquant.gateway.tiger

import cats.data.EitherT
import cats.effect.kernel.Sync
import com.tigerbrokers.stock.openapi.client.https.client.TigerHttpClient
import com.tigerbrokers.stock.openapi.client.https.request.TigerHttpRequest
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteMarketRequest
import com.tigerbrokers.stock.openapi.client.struct.enums.{Market, MethodName}
import com.tigerbrokers.stock.openapi.client.util.builder.AccountParamBuilder
import com.tquant.core.TigerQuantException
import com.tquant.core.model.data.MarketStatus
import com.tquant.gateway.converter.Converters

import scala.jdk.CollectionConverters._

class TigerQuoteApi[F[_]](private val client: TigerHttpClient)(implicit f: Sync[F]) {

  def grabQuotePermission(): EitherT[F, RuntimeException, String] = {
    val request = new TigerHttpRequest(MethodName.GRAB_QUOTE_PERMISSION)
    val bizContent = AccountParamBuilder.instance().buildJson()
    request.setBizContent(bizContent)

    val response = Sync[F].blocking {
      val resp = client.execute(request)
      if(resp.isSuccess) {
        Right(resp.getData)
      }
      else {
        Left(new RuntimeException("grab quote permission error:" + resp.getMessage))
      }
    }

    EitherT(response)
  }

  def getMarketState(market: Market): EitherT[F, TigerQuantException, List[MarketStatus]] = {
    val response = Sync[F].blocking {
      val resp = client.execute(QuoteMarketRequest.newRequest(market))
      if(resp.isSuccess) {
        Right(Converters.toMarketStatuses(resp.getMarketItems.asScala.toList))
      }
      else {
        Left(new TigerQuantException("get market state error:" + resp.getMessage))
      }
    }

    EitherT(response)
  }
}
