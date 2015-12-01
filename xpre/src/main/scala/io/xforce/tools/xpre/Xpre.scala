package io.xforce.tools.xpre

import io.xforce.tools.xpre.slave.{SlaveSeSearch, Slave}

import scala.collection.mutable.ArrayBuffer

object Xpre {
  def main(args :Array[String]) {
    val config = new ServiceConfig("conf/xpre.conf")
    val resource = new Resource(config)
    val master = new Master(config, resource)
    master.start()
    master.join()
  }



}
