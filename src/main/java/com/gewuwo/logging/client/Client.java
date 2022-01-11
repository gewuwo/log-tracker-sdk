package com.gewuwo.logging.client;

import com.gewuwo.logging.model.LogTrackerRecord;

import java.util.List;

/**
 * 客户端顶层接口
 *
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/9 14:05
 */
public interface Client {

    /**
     * 发送消息
     *
     * @param logTrackerRecordList 日志列表
     */
    void sendRequest(List<LogTrackerRecord> logTrackerRecordList);
}
