package io.xforce.tools.xpre.slave

import java.io.File

import groovy.lang.{GroovyClassLoader, GroovyObject}
import io.xforce.tools.xpre.{Master, Resource, ServiceConfig, Timer}

import scala.collection.mutable.ArrayBuffer

abstract class Slave(
    config :ServiceConfig,
    master :Master,
    resource :Resource) extends Thread {

  protected val taskBatch = config.globalConfig.taskBatch
  protected val statisticsReportBatch = if (config.globalConfig.qps/100 > 1) config.globalConfig.qps/100 else 1
  protected val preprocessorObj = getPreprocessorObject
  protected val checkerObj = getCheckerObject
  protected val statisticsReports = new ArrayBuffer[Long]()

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
    if (result.isEmpty) {
      return 1
    } else if (result.get._1 == -1) {
      master.getPipe().push((-1, -1))
      return -1
    }

    val offset = result.get._1
    for (i <- 0 until result.get._2) {
      doTask(resource.getData((offset+i) % resource.len))
    }
    0
  }

  protected def doTask(data :String): Unit = {
    val timer = new Timer
    var processedData :String = null
    if (preprocessorObj != null) {
      processedData = preprocess(data)
    } else {
      processedData = data
    }

    var result :String = null
    try {
      result = sendReq(processedData)
      timer.stop
    } catch {
      case e :Exception => {
        reportStatistics(timer.timeMs, false)
        return
      }
    }

    reportStatistics(
        timer.timeMs,
        if (checkerObj != null) checkResult(result) else true
    )
  }

  protected def reportStatistics(timeMs :Long, succ :Boolean): Unit = {
    statisticsReports.append(if (succ) timeMs else -timeMs)
    if (statisticsReports.length >= statisticsReportBatch) {
      master.getStatistics.reportStatistics(statisticsReports.toArray)
      statisticsReports.clear()
    }
  }

  protected def preprocess(data :String): String = data
  protected def sendReq(data :String): String
  protected def checkResult(response :String) :Boolean = true

  protected def getPreprocessorObject :GroovyObject = getGroovyObject(config.globalConfig.preprocessorFilepath)

  protected def getCheckerObject :GroovyObject = getGroovyObject(config.globalConfig.checkerFilepath)

  private def getGroovyObject(filepath :String) = {
    val classFile = new File(filepath)
    if (classFile.exists()) {
      val loader = new GroovyClassLoader(getClass.getClassLoader)
      val checkerClass = loader.parseClass(classFile)
      checkerClass.newInstance.asInstanceOf[GroovyObject]
    } else {
      null
    }
  }
}

// vim: set ts=4 sw=4 et:
