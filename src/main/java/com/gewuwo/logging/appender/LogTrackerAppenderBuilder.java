package com.gewuwo.logging.appender;

import com.gewuwo.logging.util.MachineIpUtils;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.util.Strings;

import java.util.UUID;

/**
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/12 11:31 上午
 */
public class LogTrackerAppenderBuilder<B extends LogTrackerAppenderBuilder<B>> extends AbstractAppender.Builder<B>
    implements org.apache.logging.log4j.core.util.Builder<LogTrackerAppender> {

    public static final String FIT_TIME = "fittime_";


    @PluginBuilderAttribute
    protected Boolean enabled;

    @PluginBuilderAttribute
    protected String project;

    @PluginBuilderAttribute
    @Required(message = "No sendUrl for LogTrackerAppender")
    protected String sendUrl;

    @PluginBuilderAttribute
    @Required(message = "No senderClient for LogTrackerAppender")
    protected String senderClient;

    @PluginBuilderAttribute
    protected Integer retries;

    @PluginBuilderAttribute
    protected Integer totalSizeInBytes;

    @PluginBuilderAttribute
    protected Integer maxBlockMs;

    @PluginBuilderAttribute
    protected Integer ioThreadCount;

    @PluginBuilderAttribute
    protected Integer batchSizeThresholdInBytes;

    @PluginBuilderAttribute
    protected Integer batchCountThreshold;

    @PluginBuilderAttribute
    protected Integer lingerMs;

    @PluginBuilderAttribute
    protected Integer baseRetryBackoffMs;

    @PluginBuilderAttribute
    protected Integer maxRetryBackoffMs;


    @SuppressWarnings("unchecked")
    @Override
    public B asBuilder() {
        return (B) this;
    }

    @Override
    public LogTrackerAppender build() {
        String hostIp = MachineIpUtils.getIp();

        if (Strings.isBlank(project)) {
            project = FIT_TIME + UUID.randomUUID();
        }


        enabled = enabled == null ? Boolean.FALSE : enabled;

        return new LogTrackerAppender(getName(), getFilter(), getLayout(),
            project,
            hostIp,
            retries,
            sendUrl,
            senderClient,
            totalSizeInBytes,
            maxBlockMs,
            ioThreadCount,
            batchSizeThresholdInBytes,
            batchCountThreshold,
            lingerMs,
            baseRetryBackoffMs,
            maxRetryBackoffMs,
            enabled);
    }

}
