package com.gewuwo.logging.tracker.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * LogTrackerRecord
 *
 * @author gewuwo
 * @version 1.0
 * @since 2022/1/4 3:27 下午
 */
public class LogTrackerRecord implements Serializable {

    private static final long serialVersionUID = 6303937420891223645L;

    /**
     * 日志服务的 project 名
     */
    private String project;

    /**
     * 类全路径
     */
    private String className;

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

    /**
     * 错误行数
     */
    private Integer lineNum;


    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getLineNum() {
        return lineNum;
    }

    public void setLineNum(Integer lineNum) {
        this.lineNum = lineNum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogTrackerRecord that = (LogTrackerRecord) o;
        return Objects.equals(project, that.project) && Objects.equals(className, that.className) && Objects.equals(resourceName,
            that.resourceName) && Objects.equals(message, that.message) && Objects.equals(threadName, that.threadName) && Objects.equals(traceId,
            that.traceId) && Objects.equals(logLevel, that.logLevel) && Objects.equals(hostIp, that.hostIp) && Objects.equals(time, that.time) && Objects.equals(lineNum, that.lineNum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, className, resourceName, message, threadName, traceId, logLevel, hostIp, time, lineNum);
    }

    @Override
    public String toString() {
        return "LogTrackerRecord{" +
            "project='" + project + '\'' +
            ", className='" + className + '\'' +
            ", resourceName='" + resourceName + '\'' +
            ", message='" + message + '\'' +
            ", threadName='" + threadName + '\'' +
            ", traceId='" + traceId + '\'' +
            ", logLevel='" + logLevel + '\'' +
            ", hostIp='" + hostIp + '\'' +
            ", time='" + time + '\'' +
            ", lineNum=" + lineNum +
            '}';
    }
}
