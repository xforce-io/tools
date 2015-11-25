package io.xforce.tools.xpre

import scala.collection.mutable.ArrayBuffer

object Xpre {
  def main(args :Array[String]) {
    val config = new ServiceConfig("conf/xpre.conf")
    val resource = new Resource(config)
    val end :Boolean = false
    val master = new Master(config, resource, end)
    val slaves = createSlaves(config, master, resource, end)
    master.start()
    slaves.foreach(_.start)
    slaves.foreach(_.join)
    master.join()
  }

  protected def createSlaves(
      config :ServiceConfig,
      master :Master,
      resource :Resource,
      end :Boolean): Array[Slave] = {
    val slaves = new ArrayBuffer[Slave]()
    for (i <- 0 until config.globalConfig.concurrency) {
      slaves.append(new Slave(config, master, resource, end))
    }
    slaves.toArray
  }

}
