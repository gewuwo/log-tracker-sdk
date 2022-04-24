package com.gewuwo.logging.tracker.collect;

import com.gewuwo.logging.tracker.model.LogTrackerRecord;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 批量发送的日志
 *
 * @author gewuwo
 * @version 1.0
 * @since 2022/1/7 2:48 下午
 */
public class ProducerBatch implements Delayed {

    /**
     * 服务标识
     */
    private final GroupKey groupKey;

    /**
     * batch最大缓存size
     */
    private final int batchSizeThresholdInBytes;

    /**
     * batch最大日志条数
     */
    private final int batchCountThreshold;

    /**
     * 创建时间
     */
    private final long createdMs;

    /**
     * 下次重试时间
     */
    private long nextRetryMs;

    /**
     * 当前batch使用的size
     */
    private int curBatchSizeInBytes;

    /**
     * 当前batch的日志条数
     */
    private int curBatchCount;

    /**
     * 尝试次数
     */
    private int attemptCount;

    /**
     * 日志数据
     */
    private final List<LogTrackerRecord> batchLogRecordList = new ArrayList<>();


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
        List<LogTrackerRecord> logRecordList, int sizeInBytes) {
        if (!hasRoomFor(sizeInBytes, logRecordList.size())) {
            return null;
        } else {
            SettableFuture<Result> future = SettableFuture.create();
            batchLogRecordList.addAll(logRecordList);
            curBatchCount += logRecordList.size();
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

    public List<LogTrackerRecord> getBatchLogRecordList() {
        return batchLogRecordList;
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
            ", batchLogRecordList=" + batchLogRecordList +
            ", createdMs=" + createdMs +
            ", nextRetryMs=" + nextRetryMs +
            ", curBatchSizeInBytes=" + curBatchSizeInBytes +
            ", curBatchCount=" + curBatchCount +
            '}';
    }
}
