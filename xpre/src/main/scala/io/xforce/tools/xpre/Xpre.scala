package io.xforce.tools.xpre

object Xpre {
  def main(args :Array[String]) {
    val config = new ServiceConfig("conf/xpre.conf")
    val resource = new Resource(config)
    val master = new Master(config, resource)
    master.start()
    master.join()
  }
}
