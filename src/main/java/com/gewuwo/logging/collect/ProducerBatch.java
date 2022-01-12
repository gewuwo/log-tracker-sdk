package com.gewuwo.logging.collect;

import com.gewuwo.logging.model.LogTrackerRecord;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 批量发送的日志
 *
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/7 2:48 下午
 */
public class ProducerBatch implements Delayed {

    /**
     * 服务标识
     */
    private final GroupKey groupKey;

    private final int batchSizeThresholdInBytes;

    private final int batchCountThreshold;

    private final List<LogTrackerRecord> logItems = new ArrayList<>();

    /**
     * 创建时间
     */
    private final long createdMs;

    /**
     * 下次重试时间
     */
    private long nextRetryMs;

    private int curBatchSizeInBytes;

    private int curBatchCount;

    /**
     * 尝试次数
     */
    private int attemptCount;


    public ProducerBatch(
        GroupKey groupKey,
        int batchSizeThresholdInBytes,
        int batchCountThreshold,
        long nowMs) {
        this.groupKey = groupKey;
        this.createdMs = nowMs;
        this.batchSizeThresholdInBytes = batchSizeThresholdInBytes;
        this.batchCountThreshold = batchCountThreshold;
        this.curBatchCount = 0;
        this.curBatchSizeInBytes = 0;
        this.attemptCount = 0;
    }


    public ListenableFuture<Result> tryAppend(
        List<LogTrackerRecord> items, int sizeInBytes) {
        if (!hasRoomFor(sizeInBytes, items.size())) {
            return null;
        } else {
            SettableFuture<Result> future = SettableFuture.create();
            logItems.addAll(items);
            curBatchCount += items.size();
            curBatchSizeInBytes += sizeInBytes;
            return future;
        }
    }

    public boolean isMeetSendCondition() {
        return curBatchSizeInBytes >= batchSizeThresholdInBytes || curBatchCount >= batchCountThreshold;
    }

    public long remainingMs(long nowMs, long lingerMs) {
        return lingerMs - createdTimeMs(nowMs);
    }

    public GroupKey getGroupKey() {
        return groupKey;
    }

    public List<LogTrackerRecord> getLogItems() {
        return logItems;
    }

    public long getNextRetryMs() {
        return nextRetryMs;
    }

    public void setNextRetryMs(long nextRetryMs) {
        this.nextRetryMs = nextRetryMs;
    }

    public String getProject() {
        return groupKey.getProject();
    }

    public int getCurBatchSizeInBytes() {
        return curBatchSizeInBytes;
    }

    public int getRetries() {
        return Math.max(0, attemptCount - 1);
    }

    private boolean hasRoomFor(int sizeInBytes, int count) {
        return curBatchSizeInBytes + sizeInBytes <= ProducerConfig.MAX_BATCH_SIZE_IN_BYTES
            && curBatchCount + count <= ProducerConfig.MAX_BATCH_COUNT;
    }

    private long createdTimeMs(long nowMs) {
        return Math.max(0, nowMs - createdMs);
    }


    public void appendAttempt() {
        this.attemptCount++;
    }


    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(nextRetryMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return (int) (nextRetryMs - ((ProducerBatch) o).getNextRetryMs());
    }

    @Override
    public String toString() {
        return "ProducerBatch{" +
            "groupKey=" + groupKey +
            ", batchSizeThresholdInBytes=" + batchSizeThresholdInBytes +
            ", batchCountThreshold=" + batchCountThreshold +
            ", logItems=" + logItems +
            ", createdMs=" + createdMs +
            ", nextRetryMs=" + nextRetryMs +
            ", curBatchSizeInBytes=" + curBatchSizeInBytes +
            ", curBatchCount=" + curBatchCount +
            '}';
    }
}
