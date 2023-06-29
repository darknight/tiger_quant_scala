package com.tquant.gateway.tiger

import cats.data.{EitherT, NonEmptyList}
import cats.effect.kernel.Sync
import com.tigerbrokers.stock.openapi.client.https.client.TigerHttpClient
import com.tigerbrokers.stock.openapi.client.https.domain.financial.item.{CorporateDividendItem, CorporateSplitItem, FinancialDailyItem, FinancialReportItem}
import com.tigerbrokers.stock.openapi.client.https.request.TigerHttpRequest
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteMarketRequest
import com.tigerbrokers.stock.openapi.client.struct.enums.{FinancialPeriodType, Market, MethodName, RightOption}
import com.tigerbrokers.stock.openapi.client.util.builder.AccountParamBuilder
import com.tquant.core.TigerQuantException
import com.tquant.core.model.data.{Bar, MarketStatus, RealtimeQuote, SymbolName, Tick, TimelineQuote, TradeCalendar}
import com.tquant.core.model.enums.BarType
import com.tquant.gateway.converter.Converters

import java.time.LocalDate
import scala.jdk.CollectionConverters._

object TigerQuoteApi {
}

class TigerQuoteApi[F[_]](private val client: TigerHttpClient)(implicit f: Sync[F]) {

  def grabQuotePermission(): EitherT[F, TigerQuantException, String] = {
    val request = new TigerHttpRequest(MethodName.GRAB_QUOTE_PERMISSION)
    val bizContent = AccountParamBuilder.instance().buildJson()
    request.setBizContent(bizContent)

    val response = Sync[F].blocking {
      val resp = client.execute(request)
      if(resp.isSuccess) {
        Right(resp.getData)
      }
      else {
        Left(new TigerQuantException("grab quote permission error:" + resp.getMessage))
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

  def getTradingCalendar(market: Market, beginDate: String, endDate: String): EitherT[F, TigerQuantException, List[TradeCalendar]] = ???

  def getSymbols(market: Market): EitherT[F, TigerQuantException, List[String]] = ???

  def getSymbolNames(market: Market): EitherT[F, TigerQuantException, List[SymbolName]] = ???

  def getBars(symbols: NonEmptyList[String], barType: BarType, rightOption: RightOption,
              limit: Int): EitherT[F, TigerQuantException, SymbolBarMap] = ???

  def getBars(symbols: NonEmptyList[String], barType: BarType, start: LocalDate,
              end: LocalDate, rightOption: RightOption): EitherT[F, TigerQuantException, SymbolBarMap] = ???

  def getFutureBars(symbols: NonEmptyList[String], barType: BarType,
                    limit: Int): EitherT[F, TigerQuantException, SymbolBarMap] = ???

  def getRealTimeQuotes(symbols: NonEmptyList[String]): EitherT[F, TigerQuantException, SymbolRealtimeQuoteMap] = ???

  def getTradeTicks(symbols: NonEmptyList[String]): EitherT[F, TigerQuantException, SymbolTickMap] = ???

  def getTimeShareQuotes(symbols: NonEmptyList[String], beginTime: Long): EitherT[F, TigerQuantException, SymbolTimelineQuoteMap] = ???

  def getFinancialDaily(symbols: NonEmptyList[String], fields: List[String],
                        beginDate: LocalDate, endDate: LocalDate): EitherT[F, TigerQuantException, SymbolFinDailyMap] = ???

  def getFinancialReport(symbols: NonEmptyList[String], fields: List[String],
                         periodType: FinancialPeriodType): EitherT[F, TigerQuantException, SymbolFinReportMap] = ???

  def getCorporateSplit(symbols: NonEmptyList[String], beginDate: LocalDate,
                        endDate: LocalDate): EitherT[F, TigerQuantException, SymbolCorpSplitMap] = ???

  def getCorporateDividend(symbols: NonEmptyList[String], beginDate: LocalDate,
                           endDate: LocalDate): EitherT[F, TigerQuantException, SymbolCorpDividendMap] = ???
}
