package io.xforce.tools.xpre.slave

import io.xforce.tools.xpre.public.HttpHelper

import io.xforce.tools.xpre.{Resource, Master, ServiceConfig}

class SlaveSimpleHttpPost(
    config :ServiceConfig,
    master :Master,
    resource :Resource) extends Slave(config, master, resource) {

  override def preprocess(data :String): String = {
    preprocessorObj.invokeMethod("process", data).asInstanceOf[String]
  }

  override def sendReq(data :String) :String = {
    val result = HttpHelper.sendPost(config.globalConfig.targetAddr, data)
    if (result._1 == 200) {
      result._2
    } else {
      null
    }
  }

  override def checkResult(response :String) :Boolean = {
    checkerObj.invokeMethod("checkResponse", response).asInstanceOf[Boolean]
  }
}


// vim: set ts=4 sw=4 et:
