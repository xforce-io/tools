package io.xforce.tools.xpre

import io.xforce.tools.xpre.public.ConcurrentPipe

class Master(
              config :ServiceConfig,
              resource :Resource,
              end :Boolean) extends Thread {
  def getPipe() :ConcurrentPipe[Int] = pipe_

  override def run(): Unit = {
    while (!end) {
      if (!process_) {
        Thread.sleep(10)
      }
    }
  }

  def process_ :Boolean = {
    if (shouldGenNewTasks) {
      pipe_.push(curOffset)
    } else {
      false
    }
  }

  private def shouldGenNewTasks :Boolean = {
    if (curTasksAssigned < numTasks) {
      (Time.getCurrentMs - timeStartMs) * qps * 1.0 / 1000 > curTasksAssigned
    } else {
      return false
    }
  }

  private def setNextOffset: Unit = {
    curOffset = (curOffset + taskBatch) % resource.len
  }

  private val pipe_ = new ConcurrentPipe[Int]()
  private val timeStartMs = Time.getCurrentMs
  private val numTasks = config.globalConfig.numTasks
  private val qps = config.globalConfig.qps
  private val taskBatch = config.globalConfig.taskBatch

  private val curTasksAssigned = 0
  private var curOffset = 0
}


// vim: set ts=4 sw=4 et:
