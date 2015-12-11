package io.xforce.tools.xpre.slave

import java.io.File

import groovy.lang.{GroovyClassLoader, GroovyObject}
import io.xforce.tools.xpre.{Resource, Master, ServiceConfig, Timer}

abstract class Slave(
    config :ServiceConfig,
    master :Master,
    resource :Resource) extends Thread {

  protected val checkerObj = getCheckerObject
  private val taskBatch = config.globalConfig.taskBatch

  override def run(): Unit = {
    while (true) {
      val ret = process
      if (ret>0) {
        Thread.sleep(10)
      } else if (ret<0) {
        return
      }
    }
  }

  protected def process :Int = {
    val result = master.getPipe().pop()
    if (result == None) {
      return 1
    } else if (result.get == -1) {
      master.getPipe().push(-1)
      return -1
    }

    val offset = result.get
    for (i <- 0 until taskBatch) {
      doTask(resource.getData((offset+i) % resource.len))
    }
    0
  }

  protected def doTask(data :AnyRef): Unit = {
    val timer = new Timer
    val result = sendReq(data)
    timer.stop

    if (checkResult(result)) {
      master.getStatistics.reportSuccs(timer.timeMs)
    } else {
      master.getStatistics.reportFails(timer.timeMs)
    }
  }

  protected def sendReq(data :AnyRef): AnyRef

  protected def checkResult(response :AnyRef) :Boolean = {
    checkerObj.invokeMethod("checkResponse", response.asInstanceOf[String]).asInstanceOf[Boolean]
  }

  protected def getCheckerObject :GroovyObject = {
    val loader = new GroovyClassLoader(getClass.getClassLoader)
    val checkerClass = loader.parseClass(new File(config.globalConfig.checkerFilepath))
    checkerClass.newInstance.asInstanceOf[GroovyObject]
  }
}

// vim: set ts=4 sw=4 et:
