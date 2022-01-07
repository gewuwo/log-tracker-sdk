package com.gewuwo.logging.appender;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.gewuwo.logging.model.LogTrackerRecord;
import com.gewuwo.logging.util.MachineIpUtils;
import com.gewuwo.logging.util.SendFeiShuUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

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

    private final String project;

    private final String projectId;

    private final String hostIp;

    /**
     * 重试次数
     */
    private final Integer retries;

    /**
     * 是否开启
     */
    private final boolean enabled;


    protected LogTrackerAppender(String name, Filter filter, Layout<? extends Serializable> layout, String project, String projectId, String hostIp
        , Integer retries, boolean enabled) {
        super(name, filter, layout);
        this.project = project;
        this.projectId = projectId;
        this.hostIp = hostIp;
        this.retries = retries;
        this.enabled = enabled;
    }

    @Override
    public void append(LogEvent event) {
        if (enabled) {

            // 获取错误信息
            String throwableStr = getThrowableStr(event.getThrown());

            // 获取traceId    如果没有traceId，生成一个uuid作为traceId
            String traceId = TraceContext.traceId();
            if (StringUtils.isBlank(traceId)) {
                traceId = UUID.randomUUID().toString();
            }

            long timeMillis = event.getTimeMillis();
            LocalDateTime localDateTime = Instant.ofEpochMilli(timeMillis).atZone(ZoneOffset.ofHours(CCT_HOURS)).toLocalDateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(NORM_DATETIME_PATTERN);
            String time = localDateTime.format(formatter);


            LogTrackerRecord trackerRecord = LogTrackerRecord.builder()
                .logLevel(event.getLevel().toString())
                .hostIp(MachineIpUtils.getIp())
                .project(project)
                .projectId(projectId)
                .hostIp(hostIp)
                .traceId(traceId)
                .resourceName(event.getSource().toString())
                .message(throwableStr)
                .time(time)
                .build();

            String s = JSONObject.toJSONString(trackerRecord, SerializerFeature.PrettyFormat);
            LOGGER.error("succ  trackerRecord:{}", s);

            String url = "https://open.feishu.cn/open-apis/bot/v2/hook/a95b8abe-7168-4d82-a7a7-68bd0b8b651d";
            int i = SendFeiShuUtil.sendFeishuMessage(url, s);
            LOGGER.info("send feishu result:{}", i);
        }
    }


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
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }


    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
        implements org.apache.logging.log4j.core.util.Builder<LogTrackerAppender> {

        @PluginBuilderAttribute
        private String project;

        @PluginBuilderAttribute
        private String projectId;

        @PluginBuilderAttribute
        private Integer retries;

        @PluginBuilderAttribute
        private boolean enabled;

        @PluginBuilderAttribute
        private boolean url;


        @SuppressWarnings("unchecked")
        @Override
        public B asBuilder() {
            return (B) this;
        }

        @Override
        public LogTrackerAppender build() {
            String hostIp = MachineIpUtils.getIp();

//            final HttpManager httpManager = new HttpURLConnectionManager(getConfiguration(),
//                getConfiguration().getLoggerContext(), getName(), url, method, connectTimeoutMillis,
//                readTimeoutMillis, headers, sslConfiguration, verifyHostname);


            return new LogTrackerAppender(getName(), getFilter(), getLayout(), project, projectId, hostIp, retries, enabled);
        }

    }

}
