import com.alibaba.fastjson.JSON

class Checker {
    def checkResponse(response) {
        jsonObj = JSON.parseObject(response.asInstanceOf[String])
        jsonObj.getJSONObject("code").getString("errmsg") == "success"
    }
}