import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.JSONArray

class Preprocesser {
    def process(response) {
        JSONObject json = JSON.parseObject(response);
        JSONArray jsonArray = json.getJSONArray("records");
        for (int i=0; i < jsonArray.size(); ++i) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            jsonObject.put("timestamp", System.currentTimeMillis());
        }
        return JSON.toJSONString(json);
    }
}