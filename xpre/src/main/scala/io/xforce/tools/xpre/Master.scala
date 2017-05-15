package io.xforce.tools.xpre

import java.util.Comparator
import java.util.concurrent.locks.ReentrantLock

import com.google.common.collect.TreeMultiset
import io.xforce.tools.xpre.public.{ConcurrentPipe, SysInfo}
import io.xforce.tools.xpre.slave.{Slave, SlaveSimpleHttpGet, SlaveSimpleHttpPost}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class Master(
              config :ServiceConfig,
              resource :Resource) extends Thread {

  private val pipe = new ConcurrentPipe[(Int,Int)](100000, config.globalConfig.concurrency)
  private val taskBatch = config.globalConfig.taskBatch

  private var curTasksAssigned = 0
  private var curOffset = 0

  private val statistics = new Statistics(config, this)

  private val slaves = createSlaves(config, this, resource)
  slaves.foreach(_.start)

  def getPipe() :ConcurrentPipe[(Int,Int)] = pipe

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
      pipe.push((-1, -1))
      -1
    }
  }

  private def assignTask : Unit = {
    val numTasksToAssign = Math.min(taskBatch, config.globalConfig.numTasks - curTasksAssigned)
    while (!pipe.push((curOffset, numTasksToAssign)))
      Thread.sleep(10)

    curOffset = (curOffset + numTasksToAssign) % resource.len
    curTasksAssigned += numTasksToAssign
  }

  private def createSlaves(
      config :ServiceConfig,
      master :Master,
      resource :Resource): Array[Slave] = {
    val slaves = new ArrayBuffer[Slave]()
    for (i <- 0 until config.globalConfig.concurrency) {
      config.globalConfig.category match {
        case "simple-http-post" => {
          slaves.append(new SlaveSimpleHttpPost(config, master, resource))
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

  private val lock = new ReentrantLock()

  private var succs = 0
  private var fails = 0
  private var failsAll = 0
  private var reqAll = 0
  private var maxMsAll = 0L
  private var timeMsTotal = 0L
  private var timeMsAll = 0L

  private var numSysInfo = 0L
  private var memUsedAll = 0D
  private var cpuUsedAll = 0D

  private val latenciesAll = TreeMultiset.create(new Comparator[Long] {
    override def compare(o1: Long, o2: Long): Int = {
      val ret = o1.asInstanceOf[Long] - o2.asInstanceOf[Long]
      if (ret>0) {
        -1
      } else if (ret<0) {
        1
      } else {
        0
      }
    }
  })

  private var lastReportTimeMs = 0L
  private val timeStartMs = Time.getCurrentMs

  def reportStatistics(timeMsRecords :Array[Long]): Unit = {
    lock.lock()

    timeMsRecords.foreach(timeMs => {
      if (timeMs >= 0) {
        succs += 1
        reqAll += 1
        if (timeMs > maxMsAll) {
          maxMsAll = timeMs
        }
        timeMsTotal += timeMs
        timeMsAll += timeMs
        latenciesAll.add(timeMs)
      } else {
        fails += 1
        failsAll += 1
        reqAll += 1
        if (-timeMs > maxMsAll) {
          maxMsAll = -timeMs
        }
        timeMsTotal += -timeMs
        timeMsAll += -timeMs
      }
    })

    val iter = latenciesAll.iterator
    while (latenciesAll.size >= Statistics.kMaxNumLatenciesAll && iter.hasNext) {
      val tmp = iter.next()
      if (Random.nextLong() % 1000 == 0) {
        latenciesAll.remove(tmp)
      }
    }

    lock.unlock()
  }

  def tasksShouldBeAssigned = {
    (Time.getCurrentMs - timeStartMs) * config.globalConfig.qps * 1.0 / 1000
  }

  def report(reportIntervalMs :Long) = {
    val timeMsElapse = Time.getCurrentMs - lastReportTimeMs
    if (timeMsElapse > reportIntervalMs) {
      lock.lock()

      val curMem = SysInfo.getMemPercent()
      val curCpu = SysInfo.getCpuPercent()

      if (Time.getCurrentMs - timeStartMs > 5000 && reportIntervalMs != 0) {
        numSysInfo += 1
        memUsedAll += curMem
        cpuUsedAll += curCpu
      }

      var latMax = -1L
      val iter = latenciesAll.iterator
      val position = (latenciesAll.size.toDouble * (1.0 - config.globalConfig.latMaxThreshold)).toInt
      for (i <- 0 until (if (position > 1) position else 1)) {
        if (iter.hasNext) {
          latMax = iter.next()
        }
      }

      val reqs = succs + fails
      println("numSpawned[%d] succ[%d] fail[%d] avgMs[%.2f] qps[%d] memUsed[%f] cpuUsed[%f] avgAll[%.2f] maxMsAll[%d] maxMsThresholdAll[%d] qpsAll[%d] failsAll[%d] memUsedAll[%f] cpuUsedAll[%f] all[%d]".format(
        config.globalConfig.numTasks,
        succs,
        fails,
        if (reqs!=0) 1.0 * timeMsTotal / reqs else 0D,
        (reqs * 1.0 / timeMsElapse * 1000).toInt,
        curMem,
        curCpu,
        timeMsAll * 1.0 / reqAll,
        maxMsAll.toInt,
        latMax,
        (reqAll * 1.0 / (Time.getCurrentMs - timeStartMs) * 1000).toInt,
        failsAll,
        if (numSysInfo != 0) memUsedAll/numSysInfo else -1D,
        if (numSysInfo != 0) cpuUsedAll/numSysInfo else -1D,
        reqAll
      ))

      succs = 0
      fails = 0
      timeMsTotal = 0
      lastReportTimeMs = Time.getCurrentMs

      lock.unlock()
    }
  }

}

object Statistics {

  protected val kMaxNumLatenciesAll = 1000*1000

}

// vim: set ts=4 sw=4 et:
