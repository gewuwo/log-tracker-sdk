package com.gewuwo.logging.appender;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.gewuwo.logging.collect.LogSender;
import com.gewuwo.logging.collect.ProducerConfig;
import com.gewuwo.logging.model.LogTrackerRecord;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.util.Strings;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;

import static com.gewuwo.logging.collect.ProducerConfig.*;

/**
 * 错误追踪 Appender
 *
 * @author jishan.guo
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


    protected LogSender sender;


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
                                 Boolean enabled) {
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
    }

    @Override
    public void append(LogEvent event) {
        if (enabled) {
            // 获取错误信息
            String throwableStr = getThrowableStr(event.getThrown());

            // 获取traceId    如果没有traceId，生成一个uuid作为traceId
            String traceId = TraceContext.traceId();
            if (Strings.isBlank(traceId)) {
                traceId = UUID.randomUUID().toString();
            }

            long timeMillis = event.getTimeMillis();
            LocalDateTime localDateTime = Instant.ofEpochMilli(timeMillis).atZone(ZoneOffset.ofHours(CCT_HOURS)).toLocalDateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(NORM_DATETIME_PATTERN);
            String time = localDateTime.format(formatter);

            LogTrackerRecord trackerRecord = new LogTrackerRecord();
            trackerRecord.setLogLevel(event.getLevel().toString());
            trackerRecord.setProject(project);
            trackerRecord.setHostIp(hostIp);
            trackerRecord.setTraceId(traceId);
            trackerRecord.setResourceName(event.getSource().toString());
            trackerRecord.setMessage(throwableStr);
            trackerRecord.setTime(time);

            String s = JSONObject.toJSONString(trackerRecord, SerializerFeature.PrettyFormat);
            LOGGER.error("succ  trackerRecord:{}", s);

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

        if (batchCountThreshold != null && batchCountThreshold > 0 && batchCountThreshold <= MAX_BATCH_COUNT) {
            producerConfig.setBatchCountThreshold(batchCountThreshold);
        }

        if (batchSizeThresholdInBytes != null && batchSizeThresholdInBytes > 0 && batchSizeThresholdInBytes <= MAX_BATCH_SIZE_IN_BYTES) {
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

        if (lingerMs != null && lingerMs >= LINGER_MS_LOWER_LIMIT) {
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

        sender = new LogSender(producerConfig);

        sender.putProjectConfig(project, senderClient, sendUrl);
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
     * TODO 改为获取fittime包下的异常
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
        for (String s : Throwables.toStringList(throwable)) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(System.getProperty("line.separator"));
            }
            sb.append(s);
        }
        return sb.toString();
    }


    @PluginBuilderFactory
    public static <B extends LogTrackerAppenderBuilder<B>> B newBuilder() {
        return new LogTrackerAppenderBuilder<B>().asBuilder();
    }


}
