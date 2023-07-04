package com.tquant.storage

import cats.effect.IO
import cats.effect.kernel.Resource
import com.tquant.core.config.ServerConf
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor

// TODO: respect `"storage.enable"` settings from ConfigLoader
object DAOInstance {
  def createXaRes(conf: ServerConf): Resource[IO, HikariTransactor[IO]] = {
    for {
      hikariConfig <- Resource.pure {
        val config = new HikariConfig()
        config.setDriverClassName("org.postgresql.Driver")
        config.setJdbcUrl(conf.jdbc.url)
        config.setUsername(conf.jdbc.user)
        config.setPassword(conf.jdbc.password)
        config
      }
      xa <- HikariTransactor.fromHikariConfig[IO](hikariConfig)
    } yield xa
  }
}
