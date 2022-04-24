package com.gewuwo.logging.tracker.collect;


import com.gewuwo.logging.tracker.appender.LogTrackerAppenderBuilder;

/**
 * Configuration for {@link LogSender}.
 *
 * @author gewuwo
 * @version 1.0
 * @since 2022/1/12 10:51 上午
 */
public class ProducerConfig {

    public static final int DEFAULT_TOTAL_SIZE_IN_BYTES = 100 * 1024 * 1024;

    public static final long DEFAULT_MAX_BLOCK_MS = 60 * 1000L;

    public static final int DEFAULT_IO_THREAD_COUNT =
        Math.max(Runtime.getRuntime().availableProcessors(), 1);

    public static final int DEFAULT_BATCH_SIZE_THRESHOLD_IN_BYTES = 512 * 1024;

    public static final int MAX_BATCH_SIZE_IN_BYTES = 8 * 1024 * 1024;

    public static final int DEFAULT_BATCH_COUNT_THRESHOLD = 4096;

    public static final int MAX_BATCH_COUNT = 40960;

    public static final int DEFAULT_LINGER_MS = 2000;

    public static final int LINGER_MS_LOWER_LIMIT = 100;

    public static final int DEFAULT_RETRIES = 10;

    public static final long DEFAULT_BASE_RETRY_BACKOFF_MS = 100L;

    public static final long DEFAULT_MAX_RETRY_BACKOFF_MS = 50 * 1000L;

    public static final int DEFAULT_ERROR_MESSAGE_LINE = 10;


    /**
     * 单个 producer 实例能缓存的日志大小上限，默认为 100MB。
     */
    private int totalSizeInBytes = DEFAULT_TOTAL_SIZE_IN_BYTES;

    /**
     * 如果 producer 可用空间不足，调用者在 send 方法上的最大阻塞时间，默认为 60 秒。
     */
    private long maxBlockMs = DEFAULT_MAX_BLOCK_MS;

    /**
     * 执行日志发送任务的线程池大小，默认为可用处理器个数。
     */
    private int ioThreadCount = DEFAULT_IO_THREAD_COUNT;

    /**
     * 当一个 ProducerBatch 中缓存的日志大小大于等于 batchSizeThresholdInBytes 时，该 batch 将被发送，默认为 512 KB，最大可设置成 5MB。
     */
    private int batchSizeThresholdInBytes = DEFAULT_BATCH_SIZE_THRESHOLD_IN_BYTES;

    /**
     * 当一个 ProducerBatch 中缓存的日志条数大于等于 batchCountThreshold 时，该 batch 将被发送，默认为 4096，最大可设置成 40960。
     */
    private int batchCountThreshold = DEFAULT_BATCH_COUNT_THRESHOLD;

    /**
     * 一个 ProducerBatch 从创建到可发送的逗留时间，默认为 2 秒，最小可设置成 100 毫秒。
     */
    private int lingerMs = DEFAULT_LINGER_MS;

    /**
     * 如果某个 ProducerBatch 首次发送失败，能够对其重试的次数，默认为 10 次。
     */
    private int retries = DEFAULT_RETRIES;

    /**
     * 首次重试的退避时间，默认为 100 毫秒。<br/>
     * Producer 采样指数退避算法，第 N 次重试的计划等待时间为 baseRetryBackoffMs * 2^(N-1)。
     */
    private long baseRetryBackoffMs = DEFAULT_BASE_RETRY_BACKOFF_MS;

    /**
     * 重试的最大退避时间，默认为 50 秒。
     */
    private long maxRetryBackoffMs = DEFAULT_MAX_RETRY_BACKOFF_MS;

    /**
     * 最大错误消息行数，默认为{@link #DEFAULT_ERROR_MESSAGE_LINE}<br/>
     * 检测到某个关键字之后再取 {maxErrorMessageLine} 行
     */
    private int maxErrorMessageLine = DEFAULT_ERROR_MESSAGE_LINE;

    /**
     * 最大错误消息行数拦截的包名，配合 {maxErrorMessageLine} 字段一起使用
     */
    private String basePackageName = LogTrackerAppenderBuilder.TRACKER;


    /**
     * @return The total bytes of memory the producer can use to buffer logs waiting to be sent to the
     * server.
     */
    public int getTotalSizeInBytes() {
        return totalSizeInBytes;
    }

    /**
     * Set the total bytes of memory the producer can use to buffer logs waiting to be sent to the
     * server.
     */
    public void setTotalSizeInBytes(int totalSizeInBytes) {
        if (totalSizeInBytes <= 0) {
            throw new IllegalArgumentException(
                "totalSizeInBytes must be greater than 0, got " + totalSizeInBytes);
        }
        this.totalSizeInBytes = totalSizeInBytes;
    }

    /**
     * @return How long <code>LogProducer.send()</code> will block.
     */
    public long getMaxBlockMs() {
        return maxBlockMs;
    }

    /**
     * Set how long <code>LogProducer.send()</code> will block.
     */
    public void setMaxBlockMs(long maxBlockMs) {
        this.maxBlockMs = maxBlockMs;
    }

    /**
     * @return The thread count of the background I/O thread pool.
     */
    public int getIoThreadCount() {
        return ioThreadCount;
    }

    /**
     * Set the thread count of the background I/O thread pool.
     */
    public void setIoThreadCount(int ioThreadCount) {
        if (ioThreadCount <= 0) {
            throw new IllegalArgumentException(
                "ioThreadCount must be greater than 0, got " + ioThreadCount);
        }
        this.ioThreadCount = ioThreadCount;
    }

    /**
     * @return The batch size threshold.
     */
    public int getBatchSizeThresholdInBytes() {
        return batchSizeThresholdInBytes;
    }

    /**
     * Set the batch size threshold.
     */
    public void setBatchSizeThresholdInBytes(int batchSizeThresholdInBytes) {
        if (batchSizeThresholdInBytes < 1 || batchSizeThresholdInBytes > MAX_BATCH_SIZE_IN_BYTES) {
            throw new IllegalArgumentException(
                String.format(
                    "batchSizeThresholdInBytes must be between 1 and %d, got %d",
                    MAX_BATCH_SIZE_IN_BYTES, batchSizeThresholdInBytes));
        }
        this.batchSizeThresholdInBytes = batchSizeThresholdInBytes;
    }

    /**
     * @return The batch count threshold.
     */
    public int getBatchCountThreshold() {
        return batchCountThreshold;
    }

    /**
     * Set the batch count threshold.
     */
    public void setBatchCountThreshold(int batchCountThreshold) {
        if (batchCountThreshold < 1 || batchCountThreshold > MAX_BATCH_COUNT) {
            throw new IllegalArgumentException(
                String.format(
                    "batchCountThreshold must be between 1 and %d, got %d",
                    MAX_BATCH_COUNT, batchCountThreshold));
        }
        this.batchCountThreshold = batchCountThreshold;
    }

    /**
     * @return The max linger time of a log.
     */
    public int getLingerMs() {
        return lingerMs;
    }

    /**
     * Set the max linger time of a log.
     */
    public void setLingerMs(int lingerMs) {
        if (lingerMs < LINGER_MS_LOWER_LIMIT) {
            throw new IllegalArgumentException(
                String.format(
                    "lingerMs must be greater than or equal to %d, got %d",
                    LINGER_MS_LOWER_LIMIT, lingerMs));
        }
        this.lingerMs = lingerMs;
    }

    /**
     * @return The retry times for transient error.
     */
    public int getRetries() {
        return retries;
    }

    /**
     * Set the retry times for transient error. Setting a value greater than zero will cause the
     * client to resend any log whose send fails with a potentially transient error.
     */
    public void setRetries(int retries) {
        this.retries = retries;
    }

    /**
     * @return The amount of time to wait before attempting to retry a failed request for the first
     * time.
     */
    public long getBaseRetryBackoffMs() {
        return baseRetryBackoffMs;
    }

    /**
     * Set the amount of time to wait before attempting to retry a failed request for the first time.
     */
    public void setBaseRetryBackoffMs(long baseRetryBackoffMs) {
        if (baseRetryBackoffMs <= 0) {
            throw new IllegalArgumentException(
                "baseRetryBackoffMs must be greater than 0, got " + baseRetryBackoffMs);
        }
        this.baseRetryBackoffMs = baseRetryBackoffMs;
    }

    /**
     * @return The upper limit of time to wait before attempting to retry a failed request.
     */
    public long getMaxRetryBackoffMs() {
        return maxRetryBackoffMs;
    }

    /**
     * Set the upper limit of time to wait before attempting to retry a failed request.
     */
    public void setMaxRetryBackoffMs(long maxRetryBackoffMs) {
        if (maxRetryBackoffMs <= 0) {
            throw new IllegalArgumentException(
                "maxRetryBackoffMs must be greater than 0, got " + maxRetryBackoffMs);
        }
        this.maxRetryBackoffMs = maxRetryBackoffMs;
    }

    public int getMaxErrorMessageLine() {
        return maxErrorMessageLine;
    }

    public void setMaxErrorMessageLine(int maxErrorMessageLine) {
        this.maxErrorMessageLine = maxErrorMessageLine;
    }

    public String getBasePackageName() {
        return basePackageName;
    }

    public void setBasePackageName(String basePackageName) {
        this.basePackageName = basePackageName;
    }
}
