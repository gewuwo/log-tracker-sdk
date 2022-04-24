package com.gewuwo.logging.tracker.appender;

import com.gewuwo.logging.tracker.client.Client;
import com.gewuwo.logging.tracker.client.LogTrackerServerClient;
import com.gewuwo.logging.tracker.collect.LogSender;
import com.gewuwo.logging.tracker.collect.ProducerConfig;
import com.gewuwo.logging.tracker.model.LogTrackerRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;

/**
 * 错误追踪 Appender
 *
 * @author gewuwo
 * @version 1.0
 * @since 2022/1/4 3:43 下午
 */
@Plugin(name = "LogTracker", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class LogTrackerAppender extends AbstractAppender {

    public static final int CCT_HOURS = 8;

    public final String NORM_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    protected final Boolean enabled;

    protected final String project;

    protected final String senderClient;

    protected final String sendUrl;

    protected final String hostIp;

    protected final Integer retries;

    protected Integer totalSizeInBytes;

    protected Integer maxBlockMs;

    protected Integer ioThreadCount;

    protected Integer batchSizeThresholdInBytes;

    protected Integer batchCountThreshold;

    protected Integer lingerMs;

    protected Integer baseRetryBackoffMs;

    protected Integer maxRetryBackoffMs;

    protected Integer maxErrorMessageLine;

    protected String basePackageName;

    protected LogSender sender;

    private final static String SERVER_START_INFO = "服务启动";


    private final ProducerConfig producerConfig = new ProducerConfig();


    protected LogTrackerAppender(String name, Filter filter, Layout<? extends Serializable> layout,
                                 String project,
                                 String hostIp,
                                 Integer retries,
                                 String sendUrl,
                                 String senderClient,
                                 Integer totalSizeInBytes,
                                 Integer maxBlockMs,
                                 Integer ioThreadCount,
                                 Integer batchSizeThresholdInBytes,
                                 Integer batchCountThreshold,
                                 Integer lingerMs,
                                 Integer baseRetryBackoffMs,
                                 Integer maxRetryBackoffMs,
                                 Boolean enabled,
                                 Integer maxErrorMessageLine,
                                 String basePackageName) {
        super(name, filter, layout);
        this.project = project;
        this.hostIp = hostIp;
        this.retries = retries;
        this.sendUrl = sendUrl;
        this.senderClient = senderClient;
        this.totalSizeInBytes = totalSizeInBytes;
        this.maxBlockMs = maxBlockMs;
        this.ioThreadCount = ioThreadCount;
        this.batchSizeThresholdInBytes = batchSizeThresholdInBytes;
        this.batchCountThreshold = batchCountThreshold;
        this.lingerMs = lingerMs;
        this.baseRetryBackoffMs = baseRetryBackoffMs;
        this.maxRetryBackoffMs = maxRetryBackoffMs;
        this.enabled = enabled;
        this.maxErrorMessageLine = maxErrorMessageLine;
        this.basePackageName = basePackageName;
    }

    @Override
    public void append(LogEvent event) {
        if (enabled) {
            // 获取错误信息
            String throwableStr = event.getThrown() == null ? event.getMessage().getFormattedMessage() : getThrowableStr(event.getThrown());

            // 获取traceId
            String traceId = TraceContext.traceId();

            long timeMillis = event.getTimeMillis();
            LocalDateTime localDateTime = Instant.ofEpochMilli(timeMillis).atZone(ZoneOffset.ofHours(CCT_HOURS)).toLocalDateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(NORM_DATETIME_PATTERN);
            String time = localDateTime.format(formatter);

            LogTrackerRecord trackerRecord = new LogTrackerRecord();
            trackerRecord.setLogLevel(event.getLevel().toString());
            trackerRecord.setProject(project);
            trackerRecord.setHostIp(hostIp);
            trackerRecord.setTraceId(traceId);

            if (event.getThrown() != null) {
                StackTraceElement[] stackTraceList = event.getThrownProxy().getStackTrace();
                for (StackTraceElement stackTrace : stackTraceList) {
                    if (stackTrace.getClassName().contains(producerConfig.getBasePackageName())) {
                        trackerRecord.setClassName(stackTrace.getClassName());
                        trackerRecord.setLineNum(stackTrace.getLineNumber());
                        trackerRecord.setResourceName(stackTrace.toString());
                        break;
                    }
                }
            }

            if (trackerRecord.getClassName() == null) {
                trackerRecord.setClassName(event.getLoggerName());
            }

            trackerRecord.setMessage(throwableStr);
            trackerRecord.setThreadName(event.getThreadName());
            trackerRecord.setTime(time);

            try {
                sender.send(project, Collections.singletonList(trackerRecord));
            } catch (Exception e) {
                this.error(
                    "Failed to send log, project=" + project
                        + ", logItem=" + Collections.singletonList(trackerRecord), e);
            }
        }
    }


    @Override
    public void start() {
        super.start();

        if (batchCountThreshold != null && batchCountThreshold > 0 && batchCountThreshold <= ProducerConfig.MAX_BATCH_COUNT) {
            producerConfig.setBatchCountThreshold(batchCountThreshold);
        }

        if (batchSizeThresholdInBytes != null && batchSizeThresholdInBytes > 0 && batchSizeThresholdInBytes <= ProducerConfig.MAX_BATCH_SIZE_IN_BYTES) {
            producerConfig.setBatchSizeThresholdInBytes(batchSizeThresholdInBytes);
        }

        if (ioThreadCount != null && ioThreadCount > 0) {
            producerConfig.setIoThreadCount(ioThreadCount);
        }

        if (baseRetryBackoffMs != null && baseRetryBackoffMs > 0) {
            producerConfig.setBaseRetryBackoffMs(baseRetryBackoffMs);
        }

        if (retries != null) {
            producerConfig.setRetries(retries);
        }

        if (lingerMs != null && lingerMs >= ProducerConfig.LINGER_MS_LOWER_LIMIT) {
            producerConfig.setLingerMs(lingerMs);
        }

        if (maxBlockMs != null) {
            producerConfig.setMaxBlockMs(maxBlockMs);
        }

        if (maxRetryBackoffMs != null && maxRetryBackoffMs > 0) {
            producerConfig.setMaxRetryBackoffMs(maxRetryBackoffMs);
        }

        if (totalSizeInBytes != null && totalSizeInBytes > 0) {
            producerConfig.setTotalSizeInBytes(totalSizeInBytes);
        }

        if (maxErrorMessageLine != null && maxErrorMessageLine > 0) {
            producerConfig.setMaxErrorMessageLine(maxErrorMessageLine);
        }

        if (basePackageName != null && !"".equals(basePackageName)) {
            producerConfig.setBasePackageName(basePackageName);
        }

        sender = new LogSender(producerConfig);

        sender.putProjectConfig(project, senderClient, sendUrl);
        this.sendServerStartInfo();
    }

    /**
     * 服务启动标识，只在服务启动的时候调用一次
     */
    public void sendServerStartInfo() {

        try {
            LogTrackerRecord logTrackerRecord = new LogTrackerRecord();
            logTrackerRecord.setClassName(this.getClass().getName());
            logTrackerRecord.setThreadName(Thread.currentThread().getName());
            logTrackerRecord.setMessage(SERVER_START_INFO);
            SimpleDateFormat formatter = new SimpleDateFormat(NORM_DATETIME_PATTERN);
            String time = formatter.format(new Date());
            logTrackerRecord.setTime(time);
            logTrackerRecord.setLogLevel(Level.INFO.toString());
            logTrackerRecord.setHostIp(hostIp);
            logTrackerRecord.setProject(project);
            logTrackerRecord.setResourceName(SERVER_START_INFO);
            logTrackerRecord.setLineNum(0);

            Client client = new LogTrackerServerClient(sendUrl);
            client.sendRequest(Collections.singletonList(logTrackerRecord));
        } catch (Exception e) {
            LOGGER.error("启动功能异常:", e);
        }

    }


    @Override
    public void stop() {
        super.stop();
        if (sender != null) {
            try {
                sender.close();
            } catch (Exception e) {
                this.error("Failed to close LoghubAppender.", e);
            }
        }
    }


    /**
     * 获取异常信息
     *
     * @param throwable Throwable对象
     * @return 异常信息
     */
    private String getThrowableStr(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        Integer errorLine = null;
        for (String s : Throwables.toStringList(throwable)) {

            if (s.contains(producerConfig.getBasePackageName()) && errorLine == null) {
                errorLine = 0;
            }

            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(System.getProperty("line.separator"));
            }
            sb.append(s);

            if (errorLine != null && errorLine++ > producerConfig.getMaxErrorMessageLine()) {
                break;
            }
        }
        return sb.toString();
    }


    @PluginBuilderFactory
    public static <B extends LogTrackerAppenderBuilder<B>> B newBuilder() {
        return new LogTrackerAppenderBuilder<B>().asBuilder();
    }

}
