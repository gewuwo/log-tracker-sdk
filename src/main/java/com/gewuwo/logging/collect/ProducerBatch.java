package com.gewuwo.logging.collect;

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

    private static final Logger LOGGER = LogManager.getLogger(ProducerBatch.class);

    /**
     * 服务标识
     */
    private final GroupKey groupKey;

    private final String packageId;

    private final int batchSizeThresholdInBytes;

    private final int batchCountThreshold;

    private final List<LogItem> logItems = new ArrayList<LogItem>();

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

    private final EvictingQueue<Attempt> reservedAttempts;

    private int attemptCount;

    public ProducerBatch(
        GroupKey groupKey,
        String packageId,
        int batchSizeThresholdInBytes,
        int batchCountThreshold,
        int maxReservedAttempts,
        long nowMs) {
        this.groupKey = groupKey;
        this.packageId = packageId;
        this.createdMs = nowMs;
        this.batchSizeThresholdInBytes = batchSizeThresholdInBytes;
        this.batchCountThreshold = batchCountThreshold;
        this.curBatchCount = 0;
        this.curBatchSizeInBytes = 0;
        this.reservedAttempts = EvictingQueue.create(maxReservedAttempts);
        this.attemptCount = 0;
    }

    public ListenableFuture<Result> tryAppend(LogItem item, int sizeInBytes, Callback callback) {
        if (!hasRoomFor(sizeInBytes, 1)) {
            return null;
        } else {
            SettableFuture<Result> future = SettableFuture.create();
            logItems.add(item);
            thunks.add(new Thunk(callback, future));
            curBatchCount++;
            curBatchSizeInBytes += sizeInBytes;
            return future;
        }
    }

    public ListenableFuture<Result> tryAppend(
        List<LogItem> items, int sizeInBytes, Callback callback) {
        if (!hasRoomFor(sizeInBytes, items.size())) {
            return null;
        } else {
            SettableFuture<Result> future = SettableFuture.create();
            logItems.addAll(items);
            thunks.add(new Thunk(callback, future));
            curBatchCount += items.size();
            curBatchSizeInBytes += sizeInBytes;
            return future;
        }
    }

    public void appendAttempt(Attempt attempt) {
        reservedAttempts.add(attempt);
        this.attemptCount++;
    }

    public boolean isMeetSendCondition() {
        return curBatchSizeInBytes >= batchSizeThresholdInBytes || curBatchCount >= batchCountThreshold;
    }

    public long remainingMs(long nowMs, long lingerMs) {
        return lingerMs - createdTimeMs(nowMs);
    }

    public void fireCallbacksAndSetFutures() {
        List<Attempt> attempts = new ArrayList<Attempt>(reservedAttempts);
        Attempt attempt = Iterables.getLast(attempts);
        Result result = new Result(attempt.isSuccess(), attempts, attemptCount);
        fireCallbacks(result);
        setFutures(result);
    }

    public GroupKey getGroupKey() {
        return groupKey;
    }

    public String getPackageId() {
        return packageId;
    }

    public List<LogItem> getLogItems() {
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

    public String getLogStore() {
        return groupKey.getLogStore();
    }

    public String getTopic() {
        return groupKey.getTopic();
    }

    public String getSource() {
        return groupKey.getSource();
    }

    public String getShardHash() {
        return groupKey.getShardHash();
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
        return "ProducerBatch{"
            + "groupKey="
            + groupKey
            + ", packageId='"
            + packageId
            + '\''
            + ", batchSizeThresholdInBytes="
            + batchSizeThresholdInBytes
            + ", batchCountThreshold="
            + batchCountThreshold
            + ", logItems="
            + logItems
            + ", thunks="
            + thunks
            + ", createdMs="
            + createdMs
            + ", nextRetryMs="
            + nextRetryMs
            + ", curBatchSizeInBytes="
            + curBatchSizeInBytes
            + ", curBatchCount="
            + curBatchCount
            + ", reservedAttempts="
            + reservedAttempts
            + ", attemptCount="
            + attemptCount
            + '}';
    }
}
