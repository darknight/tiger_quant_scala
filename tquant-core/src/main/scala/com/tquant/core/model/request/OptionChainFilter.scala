package com.tquant.core.model.request

case class OptionChainFilter(iTheMoney: Boolean,
                             minIV: Double, maxIV: Double,
                             minOpenInterest: Int, maxOpenInterest: Int,
                             minDelta: Double, maxDelta: Double,
                             minGamma: Double, maxGamma: Double,
                             minVega: Double, maxVega: Double,
                             minTheta: Double, maxTheta: Double,
                             minRho: Double, maxRho: Double)

object OptionChainFilter {
  def empty: OptionChainFilter =
    OptionChainFilter(false, 0D, 0D, 0, 0, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D)

  def apply(minOpenInterest: Int, maxDelta: Double): OptionChainFilter = {
    val res = empty
    res.copy(
      minOpenInterest = minOpenInterest,
      maxDelta = maxDelta
    )
  }
}
