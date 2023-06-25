package com.tquant.core

class TigerQuantException(message: String, cause: Throwable = None.orNull)
  extends RuntimeException(message, cause)
