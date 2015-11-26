package io.xforce.tools.xpre

import java.util.concurrent.atomic.AtomicInteger

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

  def reportSuccs(num :Int = 1) = succs.addAndGet(num)
  def reportFails(num :Int = 1) = fails.addAndGet(num)

  private def process :Boolean = {
    if (shouldGenNewTasks) {
      pipe_.push(curOffset)
    } else {
      false
    }
  }

  private def report = {
    val curTimeSec = Time.getCurrentSec
    if (curTimeSec != Master.lastReportTimeSec) {
      println("numSpawned[%d] succ[%d] fail[%d] qps[%d]".format(
        numTasks, succs.get(), fails.get(), ((succs.get() + fails.get()) * 1.0 / timeElapseMs * 1000).toInt
      ))
      Master.lastReportTimeSec = curTimeSec
    }
  }

  private def shouldGenNewTasks :Boolean = {
    if (curTasksAssigned < numTasks) {
      timeElapseMs * qps * 1.0 / 1000 > curTasksAssigned
    } else {
      return false
    }
  }

  private def setNextOffset: Unit = {
    curOffset = (curOffset + taskBatch) % resource.len
  }

  private def timeElapseMs = Time.getCurrentMs - timeStartMs

  private val pipe_ = new ConcurrentPipe[Int]()
  private val timeStartMs = Time.getCurrentMs
  private val numTasks = config.globalConfig.numTasks
  private val qps = config.globalConfig.qps
  private val taskBatch = config.globalConfig.taskBatch

  private val curTasksAssigned = 0
  private var curOffset = 0

  private val succs = new AtomicInteger(0)
  private val fails = new AtomicInteger(0)
}

object Master {
  protected var lastReportTimeSec = 0L
}

// vim: set ts=4 sw=4 et:
