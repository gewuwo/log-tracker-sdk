package com.gewuwo.logging.tracker.util;

import com.gewuwo.logging.tracker.model.LogTrackerRecord;

import java.util.List;

/**
 * 日志大小计算器
 *
 * @author gewuwo
 * @version 1.0
 * @since 2022/1/8 23:26
 */
public class LogSizeCalculator {


    /**
     * 日志大小计算
     *
     * @param logList 日志列表
     * @return 日志大小
     */
    public static int calculate(List<LogTrackerRecord> logList) {
        int sizeInBytes = 0;
        for (LogTrackerRecord logTrackerRecord : logList) {
            if (logTrackerRecord.getMessage() != null) {
                sizeInBytes += logTrackerRecord.getMessage().length();
            }
        }
        return sizeInBytes;
    }
}
