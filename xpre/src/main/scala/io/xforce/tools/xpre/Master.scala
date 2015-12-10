package io.xforce.tools.xpre

import java.util.concurrent.atomic.AtomicLong
import io.xforce.tools.xpre.public.ConcurrentPipe
import io.xforce.tools.xpre.slave.{SlaveSimpleHttpGet, SlaveSeSearch, Slave}

import scala.collection.mutable.ArrayBuffer

class Master(
              config :ServiceConfig,
              resource :Resource) extends Thread {
  private val pipe_ = new ConcurrentPipe[Int](config.globalConfig.concurrency)
  private val taskBatch = config.globalConfig.taskBatch

  private var curTasksAssigned = 0
  private var curOffset = 0

  private val statistics = new Statistics(config, this)

  private val slaves = createSlaves(config, this, resource)
  slaves.foreach(_.start)

  def getPipe() :ConcurrentPipe[Int] = pipe_

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

  def getStatistics :Statistics = statistics

  private def process :Int = {
    statistics.report(1000)
    val ret = shouldGenNewTasks
    if (ret==0) {
      assignTask
    } else if (ret<0) {
      var allDead = true
      do {
        allDead = true
        slaves.foreach { slave =>
          if (slave.isAlive) allDead = false
        }
        statistics.report(0)
        Thread.sleep(1000)
      } while (!allDead)
    }
    ret
  }

  private def shouldGenNewTasks :Int = {
    if (curTasksAssigned < config.globalConfig.numTasks) {
       if (statistics.tasksShouldBeAssigned > curTasksAssigned) 0 else 1
    } else {
      pipe_.push(-1)
      -1
    }
  }

  private def assignTask : Unit = {
    while (!pipe_.push(curOffset))
      Thread.sleep(10)

    curOffset = (curOffset + taskBatch) % resource.len
    curTasksAssigned += taskBatch
  }

  private def createSlaves(
      config :ServiceConfig,
      master :Master,
      resource :Resource): Array[Slave] = {
    val slaves = new ArrayBuffer[Slave]()
    for (i <- 0 until config.globalConfig.concurrency) {
      config.globalConfig.category match {
        case "se-search" => {
          slaves.append(new SlaveSeSearch(config, master, resource))
        }
        case "simple-http-get" => {
          slaves.append(new SlaveSimpleHttpGet(config, master, resource))
        }
        case _ => {
          println("unknown_category[%s]".format(config.globalConfig.category))
          sys.exit(1)
        }
      }
    }
    slaves.toArray
  }
}

class Statistics(
               val config :ServiceConfig,
               val master :Master) {
  def reportSuccs(timeMs :Long) = {
    succs.addAndGet(1)
    reqAll.addAndGet(1)
    timeMsAll.addAndGet(timeMs)
  }
  def reportFails(timeMs :Long) = {
    fails.addAndGet(1)
    reqAll.addAndGet(1)
    timeMsAll.addAndGet(timeMs)
  }

  def tasksShouldBeAssigned = {
    (Time.getCurrentMs - timeStartMs) * config.globalConfig.qps * 1.0 / 1000
  }

  def report(reportIntervalMs :Long) = {
    val timeMsElapse = Time.getCurrentMs - lastReportTimeMs
    if (timeMsElapse > reportIntervalMs) {
      val reqs = succs.get() + fails.get()
      println("numSpawned[%d] succ[%d] fail[%d] avgMs[%d] qps[%d] qpsAll[%d] all[%d]".format(
        config.globalConfig.numTasks,
        succs.get(),
        fails.get(),
        if (reqs!=0) timeMsAll.get / reqs else 0,
        (reqs * 1.0 / timeMsElapse * 1000).toInt,
        (reqAll.get() * 1.0 / (Time.getCurrentMs - timeStartMs) * 1000).toInt,
        reqAll.get()
      ))

      succs.set(0)
      fails.set(0)
      timeMsAll.set(0)
      lastReportTimeMs = Time.getCurrentMs
    }
  }

  private val succs = new AtomicLong(0)
  private val fails = new AtomicLong(0)
  private val reqAll = new AtomicLong(0)
  private val timeMsAll = new AtomicLong(0)
  private var lastReportTimeMs = 0L
  private val timeStartMs = Time.getCurrentMs
}

// vim: set ts=4 sw=4 et:
