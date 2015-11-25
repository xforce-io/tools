package io.xforce.tools.xpre

import java.io.{IOException, InputStreamReader, BufferedReader, PrintWriter}
import java.net.URL

class Slave(
             config :ServiceConfig,
             master :Master,
             resource :Resource,
             end :Boolean) extends Thread {
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
    Slave.sendPost(config.globalConfig.targetAddr, data)
  }

  private val taskBatch = config.globalConfig.taskBatch
}

object Slave {
  def sendPost(url :String, param :String) {
    var out :PrintWriter = null
    var in :BufferedReader = null
    var result = ""
    try {
      val realUrl = new URL(url)
      val conn = realUrl.openConnection()
      conn.setRequestProperty("accept", "*/*")
      conn.setRequestProperty("connection", "Keep-Alive")
      conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)")
      conn.setDoOutput(true)
      conn.setDoInput(true)
      out = new PrintWriter(conn.getOutputStream())
      out.print(param)
      out.flush()
      in = new BufferedReader(
        new InputStreamReader(conn.getInputStream()))
      var line = ""
      while (true) {
        line = in.readLine()
        if (line == null) break;
        result += line
      }
    } catch {
      case ex :Exception => {
        System.out.println("发送 POST 请求出现异常！" + ex)
        ex.printStackTrace()
      }
    }
    //使用finally块来关闭输出流、输入流
    finally{
      if(out!=null) out.close()
      if(in!=null) in.close()
    }
    result
  }
}

// vim: set ts=4 sw=4 et:
