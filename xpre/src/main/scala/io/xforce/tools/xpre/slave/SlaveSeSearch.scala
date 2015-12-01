package io.xforce.tools.xpre.slave

import java.io.{InputStreamReader, BufferedReader, PrintWriter}
import java.net.{URL, HttpURLConnection}
import scala.util.control.Breaks._

import com.alibaba.fastjson.JSON
import io.xforce.tools.xpre.{Timer, Resource, Master, ServiceConfig}

class SlaveSeSearch(
             config :ServiceConfig,
             master :Master,
             resource :Resource) extends Slave {
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

  private def process :Int = {
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

  private def doTask(data :String): Unit = {
    val timer = new Timer
    val result = SlaveSeSearch.sendPost(config.globalConfig.targetAddr, data)
    timer.stop

    if (SlaveSeSearch.checkResult(result)) {
      master.getStatistics.reportSuccs(timer.timeMs)
    } else {
      master.getStatistics.reportFails(timer.timeMs)
    }
  }

  private val taskBatch = config.globalConfig.taskBatch
}

object SlaveSeSearch {
  private val kBlockSize = 4096

  def sendPost(url :String, param :String) :String = {
    var out :PrintWriter = null
    var in :BufferedReader = null
    var result = ""
    try {
      val realUrl = new URL(url)
      val conn = realUrl.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("POST")
      conn.setRequestProperty("accept", "*/*")
      conn.setRequestProperty("connection", "Keep-Alive")
      conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)")
      conn.setDoOutput(true)
      conn.setDoInput(true)

      out = new PrintWriter(conn.getOutputStream())
      out.print(param)
      out.flush()
      in = new BufferedReader(new InputStreamReader(conn.getInputStream()))
      val buf = new Array[Char](kBlockSize)
      breakable {
          while (true) {
            val ret = in.read(buf, 0, buf.length)
            if (ret == -1) break
            result += buf.take(ret).mkString
          }
      }
    } catch {
      case ex :Exception => {
        System.out.println("fail_send_post[%s] stack[".format(url) + ex + "]")
        ex.printStackTrace()
      }
    }
    finally{
      if(out!=null) out.close()
      if(in!=null) in.close()
    }
    result
  }

  def checkResult(postRes :String) :Boolean = {
    val jsonObj = JSON.parseObject(postRes)
    jsonObj.getJSONObject("code").getString("errmsg") == "success"
  }
}

// vim: set ts=4 sw=4 et:
