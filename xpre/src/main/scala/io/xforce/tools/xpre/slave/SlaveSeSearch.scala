package io.xforce.tools.xpre.slave

import java.io.{InputStreamReader, BufferedReader, PrintWriter}
import java.net.{URL, HttpURLConnection}
import scala.util.control.Breaks._

import com.alibaba.fastjson.JSON
import io.xforce.tools.xpre.{Resource, Master, ServiceConfig}

class SlaveSeSearch(
             config :ServiceConfig,
             master :Master,
             resource :Resource,
             end :Boolean) extends Slave {
  override def run(): Unit = {
    while (!end) {
      if (!process) {
        Thread.sleep(10)
      }
    }
  }

  private def process :Boolean = {
    val result = master.getPipe().pop()
    if (result == None) {
      return false
    }

    val offset = result.get
    for (i <- 0 until taskBatch) {
      doTask(resource.getData((offset+i) % resource.len))
    }
    true
  }

  private def doTask(data :String): Unit = {
    val result = SlaveSeSearch.sendPost(config.globalConfig.targetAddr, data)
    if (SlaveSeSearch.checkResult(result)) {
      master.reportSuccs()
    } else {
      master.reportFails()
    }
  }

  private val taskBatch = config.globalConfig.taskBatch
}

object SlaveSeSearch {
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
      conn.setRequestMethod("POST")

      out = new PrintWriter(conn.getOutputStream())
      out.print(param)
      out.flush()
      in = new BufferedReader(new InputStreamReader(conn.getInputStream()))
      var line = ""
      breakable {
          while (true) {
            line = in.readLine()
            if (line == null) break
            result += line
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
