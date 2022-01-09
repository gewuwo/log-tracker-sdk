package com.gewuwo.logging.util;

import com.gewuwo.logging.model.LogTrackerRecord;

import java.util.List;

/**
 * 日志大小计算器
 *
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/8 23:26
 */
public class LogSizeCalculator {


	/**
	 * 日志大小计算
	 *
	 * @param logList
	 * @return
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
