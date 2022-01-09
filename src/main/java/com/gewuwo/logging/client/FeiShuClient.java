package com.gewuwo.logging.client;

import com.alibaba.fastjson.JSONObject;
import com.gewuwo.logging.model.LogTrackerRecord;
import com.gewuwo.logging.util.Content;
import com.gewuwo.logging.util.HttpClientUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/9 14:40
 */
public class FeiShuClient implements Client {

	private final String url;

	public FeiShuClient(String url) {
		this.url = url;
	}

	@Override
	public void sendRequest(List<LogTrackerRecord> logTrackerRecordList) {
		Content cc = new Content();
		cc.setText(JSONObject.toJSONString(logTrackerRecordList));
		Map<String,String> stringMap = new HashMap<>();
		stringMap.put("msg_type","text");
		stringMap.put("content", JSONObject.toJSONString(cc));

		try {
			String post = HttpClientUtils.post(url, stringMap);

			if(post != null && !"".equals(post)){
				JSONObject jsonObject = JSONObject.parseObject(post);
				if(jsonObject != null && jsonObject.size()>0){
					Integer statusCode = jsonObject.getInteger("StatusCode");
					if(statusCode.equals(0)){
					}
				}
			}
		}catch (Exception e){
		}
	}

}
