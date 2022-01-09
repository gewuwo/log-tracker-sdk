package com.gewuwo.logging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LogTrackerRecord
 *
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/4 3:27 下午
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogTrackerRecord implements Serializable {

    private static final long serialVersionUID = 6303937420891223645L;

    /**
     * 日志服务的 project 名
     */
    private String project;

    /**
     * 资源名（实际的报错位置）类名 + 方法名 + 某一行xxxx
     */
    private String resourceName;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 报错线程名
     */
    private String threadName;

    /**
     * traceId
     */
    private String traceId;

    /**
     * 日志级别
     */
    private String logLevel;

    /**
     * 机器地址
     */
    private String hostIp;

    /**
     * 报错时间
     */
    private String time;

}
