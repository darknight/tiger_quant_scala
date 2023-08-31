package com.tquant.algo.thetagang

import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.effect.{IO, Ref}
import cats.implicits._
import com.tigerbrokers.stock.openapi.client.https.domain.option.item.{OptionBriefItem, OptionRealTimeQuoteGroup}
import com.tquant.core.algo.AlgoEngine
import com.tquant.core.log.logging
import com.tquant.core.model.data.{Asset, Contract, HourTrading, Order, Position, RealtimeQuote, Tick}
import org.typelevel.log4cats.LoggerFactory
import com.tigerbrokers.stock.openapi.client.struct.enums.{OrderStatus, Right => TRight}
import com.tquant.core.model.enums.{Direction, SecType, StockStatus}
import com.tquant.core.model.request.OptionChainFilter
import com.tquant.core.util.optionDte
import com.tquant.gateway.TigerGateway
import com.tquant.gateway.tiger.SymbolPositionMap

import scala.jdk.CollectionConverters._

class PortfolioManager(algoEngine: AlgoEngine,
                       tigerGateway: TigerGateway,
                       portfolioConf: RollingSellPutConfig) {
  import PortfolioManager._

  val logger = LoggerFactory[IO].getLogger

  def orderStatusEvent(order: Order): IO[Unit] = {
    for {
      _ <- logger.info(s"Receive order status: ${order.status}, symbol: ${order.symbol}")
    } yield ()
  }

  def getCalls(portfolioPos: SymbolPositionMap): IO[List[Position]] =
    getOptions(portfolioPos, TRight.CALL)

  def getPuts(portfolioPos: SymbolPositionMap): IO[List[Position]] =
    getOptions(portfolioPos, TRight.PUT)

  private def getOptions(portfolioPos: Map[String, List[Position]], right: TRight): IO[List[Position]] = {
    IO.pure {
      portfolioPos.values.toList.flatMap(_.filter(_.isOptionForRight(right.name())))
    }
  }

  private def getTickerForStock(contract: Contract): OptionT[IO, Tick] = {
    val result = for {
      resp <- tigerGateway.quoteApi.getRealTimeQuotes(NonEmptyList.one(contract.symbol)).value
      tickOpt <- resp match {
        case Left(err) => logger.error(s"realtime quote for stock: $err") *> IO.pure(None)
        case Right(value) =>
          val tickOpt = value.get(contract.symbol).flatMap(quotes => quotes.headOption.map(quote => Tick(quote, contract)))
          IO.pure(tickOpt)
      }
    } yield tickOpt

    OptionT(result)
  }

  private def getTickerForOption(contract: Contract): OptionT[IO, Tick] = {
    val result = for {
      resp <- tigerGateway.optionApi.getOptionBrief(
        contract.symbol, TRight.valueOf(contract.right), contract.strike.toString, contract.expiry).value
      tickOpt <- resp match {
        case Left(err) => logger.error(s"realtime quote for option: $err") *> IO.pure(None)
        case Right(value) =>
          val tickOpt = value.headOption.map(item => Tick(convertToRealtimeQuote(item), contract))
          IO.pure(tickOpt)
      }
    } yield tickOpt

    OptionT(result)
  }

  private def convertToRealtimeQuote(item: OptionBriefItem): RealtimeQuote = {
    RealtimeQuote(
      symbol = item.getSymbol,
      open = item.getOpen,
      high = item.getHigh,
      low = item.getLow,
      close = item.getLatestPrice,
      preClose = item.getPreClose,
      latestPrice = item.getLatestPrice,
      latestTime = item.getTimestamp,
      askPrice = item.getAskPrice,
      askSize = item.getAskSize.toLong,
      bidPrice = item.getBidPrice,
      bidSize = item.getBidSize.toLong,
      volume = item.getVolume.toLong,
      status = StockStatus.UNKNOWN,
      hourTrading = HourTrading.empty,
      openInterest = item.getOpenInterest
    )
  }

  private def getTickerFor(contract: Contract): OptionT[IO, Tick] = {
    if (contract.isOption) getTickerForOption(contract)
    else getTickerForStock(contract)
  }

  def isItmPut(contract: Contract): IO[Boolean] = {
    for {
      tickerOpt <- getTickerForStock(contract).value
      res = tickerOpt.exists(contract.strike >= _.latestPrice)
    } yield (res)
  }

  /**
   * If position profit > 50%, return true
   * @param position
   * @return
   */
  def positionCanBeClosed(position: Position): Boolean = {
    portfolioConf.rollWhen.closeAtPnl > 0 && position.positionPnl > portfolioConf.rollWhen.closeAtPnl
  }

  def putCanBeClosed(put: Position): Boolean = positionCanBeClosed(put)

  def putCanBeRolled(put: Position): IO[Boolean] = {
    // Ignore long positions, we only roll shorts
    if (put.isOptionPut) {
      for {
        isItm <- isItmPut(put.contract)
        res <- if (portfolioConf.rollWhen.put.isItm || isItm) { // FIXME: improve logic here
          val pnl = put.positionPnl
          val dte = optionDte(put.contract.expiry)
          if (dte <= portfolioConf.rollWhen.maxDte &&
            dte <= portfolioConf.rollWhen.dte &&
            pnl >= portfolioConf.rollWhen.minPnl) {
            logger.info(f"${put.symbol} can be rolled because DTE($dte) <= (${portfolioConf.rollWhen.dte}) and " +
              f"P&L(${pnl * 100}%1.1f) >= rollWhen.minPnl(${portfolioConf.rollWhen.minPnl * 100}%1.1f)") *> IO.pure(true)
          } else if (pnl >= portfolioConf.rollWhen.pnl) {
            logger.info(f"${put.symbol} can be rolled because P&L(${pnl * 100}%1.1f) >= " +
              f"rollWhen.pnl(${portfolioConf.rollWhen.pnl * 100}%1.1f)") *> IO.pure(true)
          } else {
            logger.info(f"${put.symbol} no need to be rolled") *> IO.pure(false)
          }
        } else {
          IO.pure(false)
        }
      } yield (res)
    } else {
      IO.pure(false)
    }
  }

  def isItmCall(contract: Contract): IO[Boolean] = {
    for {
      tickerOpt <- getTickerForStock(contract).value
      res = tickerOpt.exists(contract.strike <= _.latestPrice)
    } yield (res)
  }

  def callCanBeClosed(call: Position): Boolean = positionCanBeClosed(call)

  def callCanBeRolled(call: Position): IO[Boolean] = {
    // Ignore long positions, we only roll shorts
    if (call.isOptionCall && call.position < 0) {
      for {
        isItm <- isItmCall(call.contract)
        res <- if (portfolioConf.rollWhen.call.isItm || isItm) { // FIXME: improve logic here
          val pnl = call.positionPnl
          val dte = optionDte(call.contract.expiry)
          if (dte <= portfolioConf.rollWhen.maxDte &&
            dte <= portfolioConf.rollWhen.dte &&
            pnl >= portfolioConf.rollWhen.minPnl) {
            logger.info(f"${call.symbol} can be rolled because DTE($dte) <= (${portfolioConf.rollWhen.dte}) and " +
              f"P&L(${pnl * 100}%1.1f) >= rollWhen.minPnl(${portfolioConf.rollWhen.minPnl * 100}%1.1f)") *> IO.pure(true)
          } else if (pnl >= portfolioConf.rollWhen.pnl) {
            logger.info(f"${call.symbol} can be rolled because P&L(${pnl * 100}%1.1f) >= " +
              f"rollWhen.pnl(${portfolioConf.rollWhen.pnl * 100}%1.1f)") *> IO.pure(true)
          } else {
            logger.info(f"${call.symbol} no need to be rolled") *> IO.pure(false)
          }
        } else {
          IO.pure(false)
        }
      } yield (res)
    } else {
      IO.pure(false)
    }
  }

  def getSymbols: List[String] = {
    portfolioConf.symbols.keys.toList
  }

  def filterPositions(positions: List[Position]): List[Position] = {
    positions.filter(pos => pos.account.equalsIgnoreCase(portfolioConf.account.accountId) &&
      pos.position != 0 &&
      pos.averageCost != 0.0 &&
      portfolioConf.symbols.contains(pos.contract.symbol))
  }

  def getPortfolioPositions(): IO[SymbolPositionMap] = {
    for {
      resp <- tigerGateway.tradeApi.getPositions.value
      res <- resp match {
        case Left(err) => logger.error(s"tradeApi get positions: $err") *>
          IO.pure(Map.empty[String, List[Position]])
        case Right(value) => IO.pure {
          value.map{ case (symbol, positions) => (symbol, filterPositions(positions))}
        }
      }
    } yield res
  }

  def initializeAccount(): IO[Unit] = {
    if (portfolioConf.account.cancelOrder) {
      for {
        resp <- tigerGateway.tradeApi.getOpenOrders.value
        openTrades <- resp match {
          case Left(err) => logger.error(s"tradeApi getOpenOrders: $err") *>
            IO.pure(List.empty[Order])
          case Right(value) => IO.pure(value)
        }
        cancelTrades = openTrades
          .filterNot(_.status.equalsIgnoreCase(OrderStatus.Filled.name()))
          .filter(trade => getSymbols.contains(trade.symbol))
        results <- cancelTrades.map(trade => {
          logger.info(s"cancelling order ${trade.id}") *>
          tigerGateway.tradeApi.cancelOrder(trade.id).value
        }).sequence
        _ <- results.map {
          case Left(err) => logger.error(s"cancelling order $err") *> IO.unit
          case Right(value) => logger.info(s"cancelling order $value") *> IO.unit
        }.sequence_
      } yield ()
    } else {
      IO.unit
    }
  }

  def isItm(pos: Position): IO[Boolean] = {
    if (pos.contract.right.equalsIgnoreCase(TRight.CALL.name())) {
      isItmCall(pos.contract)
    } else if (pos.contract.right.equalsIgnoreCase(TRight.PUT.name())) {
      isItmPut(pos.contract)
    } else {
      IO.pure(false)
    }
  }

  def manage(): IO[Unit] = {
    for {
      _ <- initializeAccount()
      resp <- tigerGateway.tradeApi.getAsset("").value
      _ <- resp match {
        case Left(err) => logger.error(s"tradeApi getAsset: $err")
        case Right(asset) => mainLogic(asset)
      }
    } yield ()
  }

  private def mainLogic(asset: Asset): IO[Unit] = {
    for {
      pos <- getPortfolioPositions()
      _ <- checkPuts(asset, pos)
      _ <- checkCalls(asset, pos)
      _ <- checkFroUncoveredPositions(asset, pos)
      refreshPos <- getPortfolioPositions()
      _ <- checkIfCanWritePuts(asset, refreshPos)
      _ <- logger.info("ThetaGang is done, shutting down! Cya next time.")
    } yield ()
  }

  private def checkPuts(asset: Asset, portfolioPos: SymbolPositionMap): IO[Unit] = {
    for {
      puts <- getPuts(portfolioPos)
      (closeablePuts, remainingPuts) = puts.partition(putCanBeClosed)
      canBeRolled <- remainingPuts.map(putCanBeRolled).sequence
      rollablePuts = remainingPuts.zip(canBeRolled).filter(_._2).map(_._1)
      _ <- rollPuts(asset, rollablePuts, portfolioPos)
      _ <- closePuts(closeablePuts)
    } yield ()
  }

  private def rollPuts(asset: Asset, puts: List[Position], portfolioPos: SymbolPositionMap): IO[Unit] =
    rollPositions(asset, puts, TRight.PUT, portfolioPos)

  private def closePuts(puts: List[Position]): IO[Unit] = closePositions(puts)

  private def checkCalls(asset: Asset, portfolioPos: SymbolPositionMap): IO[Unit] = {
    for {
      calls <- getCalls(portfolioPos)
      (closeableCalls, remainingCalls) = calls.partition(callCanBeClosed)
      canBeRolled <- remainingCalls.map(callCanBeRolled).sequence
      rollableCalls = remainingCalls.zip(canBeRolled).filter(_._2).map(_._1)
      _ <- rollCalls(asset, rollableCalls, portfolioPos)
      _ <- closeCalls(closeableCalls)
    } yield ()
  }

  private def rollCalls(asset: Asset, calls: List[Position],
                        portfolioPos: SymbolPositionMap): IO[Unit] =
    rollPositions(asset, calls, TRight.CALL, portfolioPos)

  private def closeCalls(calls: List[Position]): IO[Unit] = closePositions(calls)

  def rollPositions(asset: Asset, positions: List[Position], right: TRight,
                    portfolioPos: SymbolPositionMap): IO[Unit] = {
    val symbolMap = positions.map(pos => pos.symbol -> pos).toMap

    for {
      res <- positions
        .map(pos => getTickerFor(pos.contract).value)
        .sequence
      _ <- res.filter(_.isDefined).map(_.get).map(tick => {
        symbolMap.get(tick.symbol).map(pos => {
          val tempLimit = getStrikeLimit(portfolioConf, pos.symbol, right)
          val strikeLimit = right match {
            case TRight.PUT => Math.round(
              tempLimit + pos.contract.strike + pos.averageCost - midPointOrMarketPrice(tick)
            ) * 100.0 / 100.0
            case TRight.CALL => Math.round(
              Math.max(
                tempLimit,
                portfolioPos.get(pos.symbol).map(_.filter(_.isStock).map(_.averageCost).max).getOrElse(tempLimit)
              )
            ) * 100.0 / 100.0
          }
          val isCreditOnly = if (right == TRight.CALL) {
            portfolioConf.rollWhen.call.creditOnly
          } else {
            portfolioConf.rollWhen.put.creditOnly
          }
          val minimumPrice = if (isCreditOnly) midPointOrMarketPrice(tick) else 0.0

          for {
            maximumContracts <- getMaximumNewContractsFor(
              Contract(pos.symbol, SecType.STK), asset
            )
            quantityToRoll = if (optionDte(pos.contract.expiry) > portfolioConf.rollWhen.dte) {
              Math.min(pos.position, maximumContracts)
            } else {
              pos.position
            }

            sellTickOpt <- findEligibleContracts(pos.symbol, right, strikeLimit,
              pos.contract.expiry, pos.contract.strike, minimumPrice).value
            _ <- sellTickOpt.map(sellTick => {
              val price = midPointOrMarketPrice(tick) - midPointOrMarketPrice(sellTick)
              tigerGateway.tradeApi.placeLimitOrder(
                pos.contract, Direction.BUY.entryName, price, quantityToRoll
              ).value *> tigerGateway.tradeApi.placeLimitOrder(
                Contract(pos.symbol, SecType.OPT, right.name(), sellTick.contract.expiry, strikeLimit),
                Direction.SELL.entryName, price, quantityToRoll
              ).value
            }).getOrElse(IO.unit)
          } yield ()
        }).getOrElse(IO.unit)
      }).sequence
    } yield ()
  }

  def closePositions(positions: List[Position]): IO[Unit] = {
    val res = positions.map(pos => {
      for {
        bidPrice <- getTickerFor(pos.contract).value.map(_.map(_.bidPrice).getOrElse(0.0))
        _ <- if (bidPrice <= 0.0) {
          logger.error(s"unable to close ${pos.contract.symbol}, price data unavailable")
        } else {
          for {
            resp <- tigerGateway.tradeApi.placeLimitOrder(
              pos.contract, Direction.BUY.entryName, bidPrice, Math.abs(pos.position)
            ).value
            _ <- resp match {
              case Left(err) => logger.error(s"$err")
              case Right(orderId) => for {
                resp <- tigerGateway.tradeApi.getOrder(orderId).value
                _ <- resp match {
                  case Left(err) => logger.error(s"err")
                  case Right(order) => logger.info(s"order submitted, position=${Math.abs(pos.position)}," +
                    s"price=${Math.round(bidPrice)}, order=$order")
                }
              } yield ()
            }
          } yield ()
        }
      } yield ()
    })
    res.sequence_
  }

  def checkFroUncoveredPositions(asset: Asset, portfolioPos: SymbolPositionMap): IO[Unit] = {
    val res = portfolioPos.map {
      case (symbol, positions) =>
        val sellCallCount = Math.max(0, countShortOptionPositions(symbol, TRight.CALL, portfolioPos))
        val stockCount = positions.filter(_.isStock).map(_.position).sum
        val strikeLimit = Math.max(
          getStrikeLimit(portfolioConf, symbol, TRight.CALL),
          positions.filter(_.isStock).map(_.averageCost).max
        )
        val targetCalls = Math.max(0, Math.floor((stockCount * getCallCap(portfolioConf)) / 100.0)).toInt
        val newContractNeeded = targetCalls - sellCallCount

        val contract = Contract(symbol, SecType.STK)
        val callsToWriteIO = getMaximumNewContractsFor(contract, asset)
          .map(maximumNewContracts => Math.max(0, Math.min(newContractNeeded, maximumNewContracts)))

        val okToWriteIO = if (!portfolioConf.writeWhen.call.green) {
          IO.pure(true)
        } else {
          for {
            tickOpt <- getTickerForStock(contract).value
            res = tickOpt.exists(tick => tick.latestPrice > tick.close)
          } yield res
        }

        for {
          callsToWrite <- callsToWriteIO
          okToWrite <- okToWriteIO
          _ <- if (callsToWrite > 0) {
            if (!okToWrite) {
              logger.info(s"Need to write $callsToWrite calls for $symbol, but skipping because underlying is red")
            } else {
              logger.info(s"Write $callsToWrite calls, $newContractNeeded needed for $symbol " +
                s"at or above strike $strikeLimit, target calls = $targetCalls," +
                s"call count = $sellCallCount") *> writeCalls(symbol, callsToWrite, strikeLimit)
            }
          } else { IO.unit }
        } yield ()
    }

    res.toList.sequence_
  }

  def writeCalls(symbol: String, quantity: Int, strikeLimit: Double): IO[Unit] = {
    for {
      tickOpt <- findEligibleContracts(symbol, TRight.CALL, strikeLimit, "", 0.0, 0.0).value
      _ <- tickOpt match {
        case None => logger.error(s"writeCalls cannot find suitable contract for $symbol")
        case Some(tick) => for {
          resp <- tigerGateway.tradeApi
            .placeLimitOrder(tick.contract, Direction.SELL.entryName, midPointOrMarketPrice(tick), quantity)
            .value
          _ <- resp match {
            case Left(err) => logger.error(s"writeCalls placeLimitOrder: $err") *> IO.unit
            case Right(orderId) if orderId == 0 => logger.error(s"no valid order id") *> IO.unit
            case Right(orderId) => for {
              _ <- tigerGateway.tradeApi.getOrder(orderId).value
              _ <- logger.info("writeCalls done...")
            } yield ()
          }
        } yield ()
      }
    } yield ()
  }

  def writePuts(symbol: String, quantity: Int, strikeLimit: Double): IO[Unit] = {
    for {
      tickOpt <- findEligibleContracts(symbol, TRight.PUT, strikeLimit, "", 0.0, 0.0).value
      resp <- tickOpt match {
        case None => logger.error(s"writePuts cannot find suitable contract for $symbol")
        case Some(tick) =>
          val contract = Contract(symbol, SecType.OPT, TRight.PUT.name, tick.contract.expiry, tick.contract.strike)
          for {
            resp <- tigerGateway.tradeApi.placeLimitOrder(contract, Direction.SELL.entryName, midPointOrMarketPrice(tick), quantity).value
            _ <- resp match {
              case Left(err) => logger.error(s"writePuts placeLimitOrder: $err") *> IO.unit
              case Right(orderId) if orderId == 0 => logger.error(s"no valid order id") *> IO.unit
              case Right(orderId) => for {
                _ <- tigerGateway.tradeApi.getOrder(orderId).value
                _ <- logger.info("writePuts done...")
              } yield ()
            }
          } yield ()
      }
    } yield ()
  }

  def findEligibleContracts(symbol: String, right: TRight, strikeLimit: Double,
                            excludeExpirationsBefore: String, excludeFirstExpStrike: Double,
                            minimumPrice: Double): OptionT[IO, Tick] = {
    val contract = Contract(symbol, SecType.STK, right.name())
    val minDte = optionDte(excludeExpirationsBefore)

    val result = for {
      tickOpt <- getTickerForStock(contract).value
      resp <- tigerGateway.optionApi.getOptionExpiration(symbol).value
      allExpirations <- resp match {
        case Left(err) => logger.error(s"getOptionExpiration error: $err") *> IO.pure(List.empty)
        case Right(value) => IO.pure(value.getDates.asScala)
      }
      expirations = allExpirations
        .filter(exp => {
          val dte = optionDte(exp)
          dte >= minDte && dte >= portfolioConf.target.dte
        })
        .sorted

      result <- tickOpt match {
        case None => IO.pure(None)
        case Some(tick) => if (expirations.isEmpty) {
          logger.warn(s"no valid contracts found for $symbol, abort...") *> IO.pure(None)
        } else {
          val chainExpirations = expirations
            .take(Math.min(portfolioConf.optionChain.expiration, expirations.length))
            .toList
          for {
            chains <- getChainsForStock(contract.copy(expiry = chainExpirations.head))
            strikes = chains
              .filterNot(group => (right == TRight.CALL && group.getCall == null) ||
                (right == TRight.PUT && group.getPut == null))
              .map(group =>
                if (right == TRight.CALL) group.getCall.getStrike.toDouble else group.getPut.getStrike.toDouble)
              .filter(chainStrike => isValidStrike(right, chainStrike, tick.latestPrice, strikeLimit))
              .sorted
            res <- if (strikes.isEmpty) {
              logger.warn(s"no strikes found for $symbol, abort...") *> IO.pure(None)
            } else {
              val nearestStrikes = getNearestStrikes(portfolioConf, strikes, right.name())
              val contracts = expirations
                .flatMap(exp => nearestStrikes
                  .map(strike => Contract(symbol, SecType.OPT, right.name(), exp, strike)))
                .filterNot(contract => if (excludeFirstExpStrike == 0) false
                  else contract.expiry == expirations.head && contract.strike == excludeFirstExpStrike)
                .toList

              // FIXME: collect first
              val compute = contracts
                .map(getTickerForOption(_).value)
                .sequence
                .map(_.filter(_.isDefined).map(_.get))
              for {
                ticks <- compute
                validTicks = ticks.filter(tick => midPointOrMarketPrice(tick) > minimumPrice)
                tick <- if (validTicks.isEmpty) {
                  logger.warn(s"no contract found for $symbol, abort...") *> IO.pure(None)
                } else {
                  IO.pure(Some(validTicks.head))
                }
              } yield tick
            }
          } yield res
        }
      }
    } yield result

    OptionT(result)
  }

  def getChainsForStock(contract: Contract): IO[List[OptionRealTimeQuoteGroup]] = {
    val filter = OptionChainFilter(
      portfolioConf.target.minOpenInterest,
      getTargetDelta(portfolioConf, contract.symbol, contract.right)
    )
    for {
      resp <- tigerGateway.optionApi.getOptionChain(contract.symbol, contract.expiry, filter).value
      res <- resp match {
        case Left(err) => logger.error(s"getOptionChain error: $err") *> IO.pure(List.empty)
        case Right(groups) => IO.pure(groups)
      }
    } yield res
  }

  def getMaximumNewContractsFor(contract: Contract, asset: Asset): IO[Int] = {
    val totalBuyingPower = Math.floor(asset.buyingPower * portfolioConf.account.marginUsage)
    val maxBuyingPower = portfolioConf.target.maxNewContractPercent * totalBuyingPower

    val res = for {
      tick <- getTickerForStock(contract)
      price = midPointOrMarketPrice(tick)
    } yield Math.max(1, Math.round(maxBuyingPower /  price / 100).toInt)

    // FIXME: add logging
    res.value.map(_.getOrElse(1))
  }

  def checkIfCanWritePuts(asset: Asset, portfolioPos: SymbolPositionMap): IO[Unit] = {
    val stockPos = portfolioPos.values.flatten.filter(_.isStock).toList
    val totalBuyingPower = Math.floor(asset.buyingPower * portfolioConf.account.marginUsage).toDouble

    val stockSymbols = stockPos.map(pos => pos.symbol -> pos).toMap

    for {
      resp <- portfolioConf.symbols.keys.toList
        .map(symbol => getTickerForStock(Contract(symbol, SecType.STK)).value)
        .sequence
      _ <- resp.filter(_.isDefined).map(_.get).map { tick =>
        portfolioConf.symbols.get(tick.symbol).map(conf => {
          val currentPosition = Math.floor(
            stockSymbols.get(tick.symbol).map(_.position).getOrElse(0).toDouble
          ).toInt
          val targetVal = Math.round(conf.weight * totalBuyingPower * 100.0) / 100.0
          val targetQuantity = Math.floor(targetVal / tick.latestPrice).toInt
          val putCount = countShortOptionPositions(tick.symbol, TRight.PUT, portfolioPos)
          val additionalQuantity = Math.floor(targetQuantity - currentPosition - 100 * putCount).toInt / 100
          val okToWrite = !portfolioConf.writeWhen.put.red || tick.latestPrice < tick.close

          if (additionalQuantity > 0 && okToWrite) {
            for {
              maximumContracts <- getMaximumNewContractsFor(
                Contract(tick.symbol, SecType.STK), asset
              )
              putsToWrite = Math.min(maximumContracts, additionalQuantity)
              _ <- if (putsToWrite > 0) {
                val strikeLimit = getStrikeLimit(portfolioConf, tick.symbol, TRight.PUT)
                writePuts(tick.symbol, putsToWrite, strikeLimit)
              } else { IO.unit }
            } yield ()
          } else { IO.unit }
        }).getOrElse(IO.unit)
      }.sequence
    } yield ()
  }
}

object PortfolioManager {

  def getStrikeLimit(config: RollingSellPutConfig, symbol: String, right: TRight): Double = {
    right match {
      case TRight.PUT =>
        config.symbols.get(symbol).map(_.put.strikeLimit).getOrElse(0.0)
      case TRight.CALL =>
        config.symbols.get(symbol).map(_.call.strikeLimit).getOrElse(0.0)
    }
  }

  def countShortOptionPositions(symbol: String, right: TRight, portfolioPos: SymbolPositionMap): Int = {
    val res = portfolioPos
      .getOrElse(symbol, List.empty)
      .filter(pos => pos.isOptionForRight(right.name) && pos.position < 0)
      .map(_.position)
      .sum
    Math.floor(-res.toDouble).toInt
  }

  def getCallCap(config: RollingSellPutConfig): Double = {
    Math.max(0, Math.min(1.0, config.writeWhen.call.capFactor))
  }

  def midPointOrMarketPrice(tick: Tick): Double = {
    if (tick.midpoint <= 0) tick.latestPrice
    else tick.midpoint
  }

  def getTargetDelta(config: RollingSellPutConfig, symbol: String, right: String): Double = {
    config.symbols.get(symbol) match {
      case None => 0D
      case Some(sym) if right.equalsIgnoreCase(TRight.CALL.name()) =>
        if (sym.call.delta > 0) { sym.call.delta }
        else if (sym.delta > 0) { sym.delta }
        else { config.target.delta }
      case Some(sym) if right.equalsIgnoreCase(TRight.PUT.name()) =>
        if (sym.put.delta > 0) { sym.put.delta }
        else if (sym.delta > 0) { sym.delta }
        else { config.target.delta }
    }
  }

  def getNearestStrikes(config: RollingSellPutConfig, strikes: List[Double], right: String): List[Double] = {
    val chainStrike = config.optionChain.strike
    if (right.equalsIgnoreCase(TRight.PUT.name())) {
      if (strikes.size < chainStrike) { strikes }
      else { strikes.takeRight(chainStrike) }
    } else {
      strikes.take(Math.min(chainStrike, strikes.size))
    }
  }

  def isValidStrike(right: TRight, strike: Double, marketPrice: Double, strikeLimit: Double): Boolean = {
    right match {
      case TRight.CALL =>
        if (strikeLimit > 0) strike <= marketPrice && strike <= strikeLimit
        else strike <= marketPrice
      case TRight.PUT =>
        if (strikeLimit > 0) strike >= marketPrice && strike >= strikeLimit
        else strike >= marketPrice
      case _ => false
    }
  }
}
