package com.gewuwo.logging.tracker.client;

import com.alibaba.fastjson.JSON;
import com.gewuwo.logging.tracker.model.LogTrackerRecord;
import com.gewuwo.logging.tracker.util.HttpClientUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * @author gewuwo
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
    public void sendRequest(List<LogTrackerRecord> logTrackerRecordList) throws IOException {
        LOGGER.info("send TrackerServer list:{}", logTrackerRecordList);
        String jsonStr = JSON.toJSONString(logTrackerRecordList);
        String result = HttpClientUtils.post(url, jsonStr);
        LOGGER.info("send TrackerServer result:{}", result);
    }

}
