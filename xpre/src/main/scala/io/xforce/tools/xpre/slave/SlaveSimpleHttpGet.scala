package io.xforce.tools.xpre.slave

import io.xforce.tools.xpre.public.HttpHelper
import io.xforce.tools.xpre.{Resource, Master, ServiceConfig}

/**
 * Created by freeman on 15/12/4.
 */
class SlaveSimpleHttpGet(
    config :ServiceConfig,
    master :Master,
    resource :Resource) extends Slave(config, master, resource) {

  override def sendReq(data :AnyRef) :AnyRef = {
    val result = HttpHelper.sendGet(config.globalConfig.targetAddr, data.asInstanceOf[String])
    if (result._1 == 200) {
      result._2
    } else {
      null
    }
  }

  override def checkResult(response :AnyRef) :Boolean = {
    checkerObj.invokeMethod("checkResponse", response.asInstanceOf[String]).asInstanceOf[Boolean]
  }


}
