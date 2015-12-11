package io.xforce.tools.xpre.public

import java.io.{InputStreamReader, BufferedReader, PrintWriter}
import java.net.{HttpURLConnection, URL}

import scala.util.control.Breaks._

/**
 * Created by freeman on 15/12/4.
 */
object HttpHelper {
  private val kReadBlockSize = 2<<20

  def sendGet(url :String, param :String) :(Int, String) = {
    var in :BufferedReader = null
    var result = ""
    var conn :HttpURLConnection = null
    try {
      conn = new URL(url + "?" + param).openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("GET")
      conn.setRequestProperty("accept", "*/*")
      conn.setRequestProperty("connection", "Keep-Alive")
      conn.setRequestProperty("user-agent",
        "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)")
      conn.connect()

      in = new BufferedReader(new InputStreamReader(conn.getInputStream))

      val buf = new Array[Char](kReadBlockSize)
      breakable {
        while (true) {
          val ret = in.read(buf, 0, buf.length)
          if (ret == -1) break
          result += buf.take(ret).mkString
        }
      }
    } catch {
      case ex :Exception => {
        println("fail_send_post[%s] stack[".format(url) + ex + "]")
        ex.printStackTrace()
      }
    } finally {
      if(in!=null) in.close()
    }
    (conn.getResponseCode, result)
  }

  def sendPost(url :String, param :String) :(Int, String) = {
    var out :PrintWriter = null
    var in :BufferedReader = null
    var result = ""
    var conn :HttpURLConnection = null
    try {
      conn = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("POST")
      conn.setRequestProperty("accept", "*/*")
      conn.setRequestProperty("connection", "Keep-Alive")
      conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)")
      conn.setDoOutput(true)
      conn.setDoInput(true)

      out = new PrintWriter(conn.getOutputStream)
      out.print(param)
      out.flush()

      in = new BufferedReader(new InputStreamReader(conn.getInputStream))

      val buf = new Array[Char](kReadBlockSize)
      breakable {
        while (true) {
          val ret = in.read(buf, 0, buf.length)
          if (ret == -1) break
          result += buf.take(ret).mkString
        }
      }
    } catch {
      case ex :Exception => {
        println("fail_send_post[%s] stack[".format(url) + ex + "]")
      }
    } finally {
      if(out!=null) out.close()
      if(in!=null) in.close()
    }
    (conn.getResponseCode, result)
  }
}
