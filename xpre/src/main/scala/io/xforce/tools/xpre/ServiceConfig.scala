package io.xforce.tools.xpre

import java.io.File

import com.typesafe.config.{ConfigFactory, Config}

object ServiceConfig {
  protected def createGlobalConfig(config :Config) :GlobalConfig = {
    new GlobalConfig(config.getConfig("global"))
  }
}

class ServiceConfig(val confPath :String) {
  val rawConfig = ConfigFactory.parseFile(new File(confPath))

  val globalConfig = ServiceConfig.createGlobalConfig(rawConfig)
}

class GlobalConfig(val config :Config) {
  val category = config.getString("category")
  val targetAddr = config.getString("targetAddr")
  val resourceFilepath = config.getString("resourceFilepath")
  val concurrency = config.getInt("concurrency")
  val numTasks = config.getInt("numTasks")
  val qps = config.getInt("qps")
  val latMaxThreshold = config.getDouble("latMaxThreshold")
  val taskBatch = config.getInt("taskBatch")
  val preprocessorFilepath = config.getString("preprocessorFilepath")
  val checkerFilepath = config.getString("checkerFilepath")
}

