package com.gewuwo.logging.client;

import com.alibaba.fastjson.JSONObject;
import com.gewuwo.logging.collect.SendProducerBatchTask;
import com.gewuwo.logging.model.LogTrackerRecord;
import com.gewuwo.logging.util.HttpClientUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/9 14:40
 */
public class LogTrackerServerClient implements Client {

	private static final Logger LOGGER = LogManager.getLogger(LogTrackerServerClient.class);

	private final String url;

	public LogTrackerServerClient(String url) {
		this.url = url;
	}

	@Override
	public void sendRequest(List<LogTrackerRecord> logTrackerRecordList) {
		String s = JSONObject.toJSONString(logTrackerRecordList);

		try {
			String post = HttpClientUtils.post(url, s);
			LOGGER.info(post);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
