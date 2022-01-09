package com.gewuwo.logging.appender;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.gewuwo.logging.collect.LogSender;
import com.gewuwo.logging.collect.ProducerConfig;
import com.gewuwo.logging.errors.ProducerException;
import com.gewuwo.logging.model.LogTrackerRecord;
import com.gewuwo.logging.util.MachineIpUtils;
import com.gewuwo.logging.util.SendFeiShuUtil;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.util.Strings;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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

	private final String sendUrl;

	private final String hostIp;

	/**
	 * 重试次数
	 */
	private final Integer retries;

	/**
	 * 是否开启
	 */
	private final boolean enabled;

	protected LogSender sender;


	protected int totalSizeInBytes;
	protected int maxBlockMs;
	protected int ioThreadCount;
	protected int batchSizeThresholdInBytes;
	protected int batchCountThreshold;
	protected int lingerMs;
	protected int baseRetryBackoffMs;
	protected int maxRetryBackoffMs;


	private ProducerConfig producerConfig = new ProducerConfig();


	protected LogTrackerAppender(String name, Filter filter, Layout<? extends Serializable> layout, String project, String hostIp
		, Integer retries, String sendUrl, boolean enabled) {
		super(name, filter, layout);
		this.project = project;
		this.hostIp = hostIp;
		this.retries = retries;
		this.sendUrl = sendUrl;
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

			LogTrackerRecord trackerRecord = LogTrackerRecord.builder()
				.logLevel(event.getLevel().toString())
				.hostIp(MachineIpUtils.getIp())
				.project(project)
				.hostIp(hostIp)
				.traceId(traceId)
				.resourceName(event.getSource().toString())
				.message(throwableStr)
				.time(time)
				.build();

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

//		producerConfig.setBatchCountThreshold(batchCountThreshold);
//		producerConfig.setBatchSizeThresholdInBytes(batchSizeThresholdInBytes);
//		producerConfig.setIoThreadCount(ioThreadCount);
//		producerConfig.setRetries(retries);
//		producerConfig.setBaseRetryBackoffMs(baseRetryBackoffMs);
//		producerConfig.setLingerMs(lingerMs);
//		producerConfig.setMaxBlockMs(maxBlockMs);
//		producerConfig.setMaxRetryBackoffMs(maxRetryBackoffMs);

		sender = new LogSender(producerConfig);

		sender.putProjectConfig(project, sendUrl);
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
		private String sendUrl;

		@PluginBuilderAttribute
		private Integer retries;

		@PluginBuilderAttribute
		private boolean enabled;


		@SuppressWarnings("unchecked")
		@Override
		public B asBuilder() {
			return (B) this;
		}

		@Override
		public LogTrackerAppender build() {
			String hostIp = MachineIpUtils.getIp();
			if (Strings.isBlank(project)) {
				if (Strings.isBlank(projectId)) {
					project = "fittime_" + UUID.randomUUID();
				} else {
					project = projectId;
				}
			}
			return new LogTrackerAppender(getName(), getFilter(), getLayout(), project, hostIp, retries, sendUrl, enabled);
		}

	}

}
