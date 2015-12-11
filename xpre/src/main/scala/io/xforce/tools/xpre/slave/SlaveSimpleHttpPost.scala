package io.xforce.tools.xpre.slave

import io.xforce.tools.xpre.public.HttpHelper

import io.xforce.tools.xpre.{Resource, Master, ServiceConfig}

class SlaveSimpleHttpPost(
    config :ServiceConfig,
    master :Master,
    resource :Resource) extends Slave(config, master, resource) {

  override def sendReq(data :AnyRef) :AnyRef = {
    val result = HttpHelper.sendPost(config.globalConfig.targetAddr, data.asInstanceOf[String])
    if (result._1 == 200) {
      result._2
    } else {
      null
    }
  }

}


// vim: set ts=4 sw=4 et:
