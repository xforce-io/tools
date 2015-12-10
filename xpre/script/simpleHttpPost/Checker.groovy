import com.alibaba.fastjson.JSON

class Checker {
    def checkResponse(response) {
        def jsonObj = JSON.parseObject(response)
        jsonObj.getJSONObject("code").getString("errmsg") == "success"
    }
}