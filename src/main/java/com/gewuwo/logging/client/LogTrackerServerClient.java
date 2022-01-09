package com.gewuwo.logging.client;

import com.alibaba.fastjson.JSONObject;
import com.gewuwo.logging.model.LogTrackerRecord;
import com.gewuwo.logging.util.HttpClientUtils;

import java.util.List;

/**
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/9 14:40
 */
public class LogTrackerServerClient implements Client {

	private final String url;

	public LogTrackerServerClient(String url) {
		this.url = url;
	}

	@Override
	public void sendRequest(List<LogTrackerRecord> logTrackerRecordList) {
//		HttpClientUtils.post(url, logTrackerRecordList);
	}

}
