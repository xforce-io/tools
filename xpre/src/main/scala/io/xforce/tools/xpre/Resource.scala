package io.xforce.tools.xpre

import java.io.{FileOutputStream, DataOutputStream, File}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

class Resource(val config :ServiceConfig) {
  val dataStr = createDataStr()
  val len = dataStr.length

  def getData(offset :Int) = dataStr(offset)

  private def createDataStr(): Array[String] = {
    val data = new ArrayBuffer[String]
    Source.fromFile(new java.io.File(config.globalConfig.resourceFilepath)).getLines().foreach(data.append(_))
    data.toArray
  }
}


// vim: set ts=4 sw=4 et:
