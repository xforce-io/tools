package io.xforce.tools.xpre.slave

import io.xforce.tools.xpre.public.HttpHelper

import com.alibaba.fastjson.JSON
import io.xforce.tools.xpre.{Resource, Master, ServiceConfig}

class SlaveSeSearch(
    config :ServiceConfig,
    master :Master,
    resource :Resource) extends Slave(config, master, resource) {
  override def sendReq(data :AnyRef) :AnyRef = {
    HttpHelper.sendPost(config.globalConfig.targetAddr, data.asInstanceOf[String])
  }

  override def checkResult(response :AnyRef) :Boolean = {
    val jsonObj = JSON.parseObject(response.asInstanceOf[String])
    jsonObj.getJSONObject("code").getString("errmsg") == "success"
  }
}


// vim: set ts=4 sw=4 et:
