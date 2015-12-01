package io.xforce.tools.xpre

import io.xforce.tools.xpre.slave.{SlaveSeSearch, Slave}

import scala.collection.mutable.ArrayBuffer

object Xpre {
  def main(args :Array[String]) {
    val config = new ServiceConfig("conf/xpre.conf")
    val resource = new Resource(config)
    val master = new Master(config, resource)
    val slaves = createSlaves(config, master, resource)
    if (slaves == null) {
      println("fail_create_slaves")
      return
    }
    master.start()
    slaves.foreach(_.start)
    slaves.foreach(_.join)
    master.join()
  }

  protected def createSlaves(
      config :ServiceConfig,
      master :Master,
      resource :Resource): Array[Slave] = {
    val slaves = new ArrayBuffer[Slave]()
    for (i <- 0 until config.globalConfig.concurrency) {
      config.globalConfig.category match {
        case "se-search" => {
          slaves.append(new SlaveSeSearch(config, master, resource))
        }
        case _ => {
          println("unknow_category[%s]".format(config.globalConfig.category))
          return null
        }
      }
    }
    slaves.toArray
  }

}
