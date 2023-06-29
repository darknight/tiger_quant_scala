package com.tquant.gateway

import com.tigerbrokers.stock.openapi.client.https.domain.financial.item.{CorporateDividendItem, CorporateSplitItem, FinancialDailyItem, FinancialReportItem}
import com.tquant.core.model.data.{Bar, Position, RealtimeQuote, Tick, TimelineQuote}

package object tiger {
  type SymbolBarMap = Map[String, List[Bar]]
  type SymbolRealtimeQuoteMap = Map[String, List[RealtimeQuote]]
  type SymbolTickMap = Map[String, List[Tick]]
  type SymbolTimelineQuoteMap = Map[String, List[TimelineQuote]]
  type SymbolFinDailyMap = Map[String, List[FinancialDailyItem]]
  type SymbolFinReportMap = Map[String, List[FinancialReportItem]]
  type SymbolCorpSplitMap = Map[String, List[CorporateSplitItem]]
  type SymbolCorpDividendMap = Map[String, List[CorporateDividendItem]]
  type SymbolPositionMap = Map[String, List[Position]]
}
