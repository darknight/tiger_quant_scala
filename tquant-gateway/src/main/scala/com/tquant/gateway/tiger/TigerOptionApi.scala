package com.tquant.gateway.tiger

import cats.effect.kernel.Sync
import com.tigerbrokers.stock.openapi.client.https.client.TigerHttpClient

class TigerOptionApi[F[_]](private val client: TigerHttpClient)(implicit f: Sync[F]) {

}
