package io.xforce.tools.xpre.public

import org.hyperic.sigar._

/**
  * Created by freeman on 2017/5/10.
  */
object SysInfo {

  private val sigar = new Sigar

  def getMemPercent() = sigar.getMem.getUsedPercent

  def getCpuPercent() = {
    var cpuPercent = 0D
    sigar.getCpuPercList.foreach(cpuPerc => {
      cpuPercent += cpuPerc.getCombined
    })
    cpuPercent/sigar.getCpuPercList.length
  }

}
