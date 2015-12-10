package io.xforce.tools.xpre.slave

import java.io.File

import groovy.lang.{GroovyClassLoader, GroovyObject}
import io.xforce.tools.xpre.public.HttpHelper
import io.xforce.tools.xpre.{Resource, Master, ServiceConfig}

/**
 * Created by freeman on 15/12/4.
 */
class SlaveSimpleHttpGet(
    config :ServiceConfig,
    master :Master,
    resource :Resource) extends Slave(config, master, resource) {
  val checkerObj = getCheckerObject

  override def sendReq(data :AnyRef) :AnyRef = {
    HttpHelper.sendGet(config.globalConfig.targetAddr, data.asInstanceOf[String])
  }

  override def checkResult(response :AnyRef) :Boolean = {
    checkerObj.invokeMethod("checkResult", null).asInstanceOf[Boolean]
  }

  def getCheckerObject :GroovyObject = {
    val loader = new GroovyClassLoader(getClass.getClassLoader)
    val checkerClass = loader.parseClass(new File(config.globalConfig.checkerFilepath))
    checkerClass.newInstance.asInstanceOf[GroovyObject]
  }
}
