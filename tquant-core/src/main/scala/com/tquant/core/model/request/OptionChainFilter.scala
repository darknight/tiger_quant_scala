package com.tquant.core.model.request

case class OptionChainFilter(iTheMoney: Boolean,
                             minIV: Double, maxIV: Double,
                             minOpenInterest: Int, maxOpenInterest: Int,
                             minDelta: Double, maxDelta: Double,
                             minGamma: Double, maxGamma: Double,
                             minVega: Double, maxVega: Double,
                             minTheta: Double, maxTheta: Double,
                             minRho: Double, maxRho: Double)
