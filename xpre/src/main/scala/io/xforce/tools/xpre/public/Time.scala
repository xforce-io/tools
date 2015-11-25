package io.xforce.tools.xpre

/**
 * Created by freeman on 14/11/22.
 */
object Time {
  def getCurrentSec = System.currentTimeMillis / 1000
  def getCurrentMs = System.currentTimeMillis
}

class Timer {
  var timeMs = 0L
  start

  def start { timeMs = Time.getCurrentMs }
  def stop { timeMs = Time.getCurrentMs - timeMs }

  def timeSec = timeMs/1000
}
