package com.tquant.core.model.request

import cats.data.NonEmptySet

case class SubscribeRequest(symbols: NonEmptySet[String])
