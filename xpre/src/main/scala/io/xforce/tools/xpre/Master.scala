package io.xforce.tools.xpre

import java.util.concurrent.atomic.{AtomicLong, AtomicInteger}
import io.xforce.tools.xpre.public.ConcurrentPipe

class Master(
              config :ServiceConfig,
              resource :Resource,
              end :Boolean) extends Thread {
  def getPipe() :ConcurrentPipe[Int] = pipe_

  override def run(): Unit = {
    while (!end) {
      if (!process) {
        Thread.sleep(10)
      }
    }
  }

  def getStatistics :Statistics = statistics

  private def process :Boolean = {
    statistics.report
    if (shouldGenNewTasks) {
      pipe_.push(curOffset)
      setNextOffset
      true
    } else {
      false
    }
  }

  private def shouldGenNewTasks :Boolean = {
    if (curTasksAssigned < config.globalConfig.numTasks) {
       statistics.tasksShouldBeAssigned > curTasksAssigned
    } else {
      false
    }
  }

  private def setNextOffset: Unit = {
    curOffset = (curOffset + taskBatch) % resource.len
  }

  private val pipe_ = new ConcurrentPipe[Int]()
  private val taskBatch = config.globalConfig.taskBatch

  private val curTasksAssigned = 0
  private var curOffset = 0

  private val statistics = new Statistics(config, this)
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

  def report = {
    val timeMsElapse = Time.getCurrentMs - lastReportTimeMs
    if (timeMsElapse>1000) {
      val reqs = succs.get() + fails.get()
      println("numSpawned[%d] succ[%d] fail[%d] avgMs[%d] qps[%d] qpsAll[%d]".format(
        config.globalConfig.numTasks,
        succs.get(),
        fails.get(),
        if (reqs!=0) timeMsAll.get / reqs else 0,
        (reqs * 1.0 / timeMsElapse * 1000).toInt,
        (reqAll.get() * 1.0 / (Time.getCurrentMs - timeStartMs) * 1000).toInt
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
