package com.gewuwo.logging.tracker.collect;

import com.gewuwo.logging.tracker.client.Client;
import com.gewuwo.logging.tracker.errors.AppendTimeoutException;
import com.gewuwo.logging.tracker.errors.LogSizeTooLargeException;
import com.gewuwo.logging.tracker.errors.ProducerException;
import com.gewuwo.logging.tracker.model.LogTrackerRecord;
import com.gewuwo.logging.tracker.util.LogSizeCalculator;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志收集
 *
 * @author gewuwo
 * @version 1.0
 * @since 2022/1/7 4:44 下午
 */
public class LogAccumulator {

    private static final Logger LOGGER = LogManager.getLogger(LogAccumulator.class);

    /**
     * 配置信息
     */
    private final ProducerConfig producerConfig;

    /**
     * http客户端
     */
    private final Map<String, Client> clientPool;

    /**
     * 信号量流控
     */
    private final Semaphore memoryController;

    /**
     * 重试队列
     */
    private final RetryQueue retryQueue;

    /**
     * 发送线程
     */
    private final IoThreadPool ioThreadPool;

    /**
     * batches
     */
    private final ConcurrentMap<GroupKey, ProducerBatchHolder> batches;

    /**
     * 还在写入的日志
     */
    private final AtomicInteger appendsInProgress;

    /**
     * 关闭标识
     */
    private volatile boolean closed;

    public LogAccumulator(
        ProducerConfig producerConfig,
        Map<String, Client> clientPool,
        Semaphore memoryController,
        RetryQueue retryQueue,
        IoThreadPool ioThreadPool) {
        this.producerConfig = producerConfig;
        this.clientPool = clientPool;
        this.memoryController = memoryController;
        this.retryQueue = retryQueue;
        this.ioThreadPool = ioThreadPool;
        this.batches = new ConcurrentHashMap<>();
        this.appendsInProgress = new AtomicInteger(0);
        this.closed = false;
    }

    /**
     * 追加日志
     *
     * @param project       项目名
     * @param logRecordList 日志数据
     * @return {@link ListenableFuture<Result>}
     * @throws InterruptedException 线程中断
     * @throws ProducerException    Producer自定义异常
     */
    public ListenableFuture<Result> append(
        String project,
        List<LogTrackerRecord> logRecordList)
        throws InterruptedException, ProducerException {
        appendsInProgress.incrementAndGet();
        try {
            return doAppend(project, logRecordList);
        } finally {
            appendsInProgress.decrementAndGet();
        }
    }

    private ListenableFuture<Result> doAppend(
        String project,
        List<LogTrackerRecord> logRecordList)
        throws InterruptedException, ProducerException {

        int sizeInBytes = LogSizeCalculator.calculate(logRecordList);
        ensureValidLogSize(sizeInBytes);
        long maxBlockMs = producerConfig.getMaxBlockMs();
        LOGGER.trace(
            "Prepare to acquire bytes, sizeInBytes={}, maxBlockMs={}, project={}",
            sizeInBytes,
            maxBlockMs,
            project);
        if (maxBlockMs >= 0) {
            boolean acquired =
                memoryController.tryAcquire(sizeInBytes, maxBlockMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                LOGGER.warn(
                    "Failed to acquire memory within the configured max blocking time {} ms, "
                        + "requiredSizeInBytes={}, availableSizeInBytes={}",
                    producerConfig.getMaxBlockMs(),
                    sizeInBytes,
                    memoryController.availablePermits());
                throw new AppendTimeoutException(
                    "failed to acquire memory within the configured max blocking time "
                        + producerConfig.getMaxBlockMs()
                        + " ms");
            }
        } else {
            memoryController.acquire(sizeInBytes);
        }
        try {
            GroupKey groupKey = new GroupKey(project);
            ProducerBatchHolder holder = getOrCreateProducerBatchHolder(groupKey);
            synchronized (holder) {
                return appendToHolder(groupKey, logRecordList, sizeInBytes, holder);
            }
        } catch (Exception e) {
            memoryController.release(sizeInBytes);
            throw new ProducerException(e);
        }
    }

    private ListenableFuture<Result> appendToHolder(
        GroupKey groupKey,
        List<LogTrackerRecord> logRecordList,
        int sizeInBytes,
        ProducerBatchHolder holder) {
        if (holder.producerBatch != null) {
            ListenableFuture<Result> f = holder.producerBatch.tryAppend(logRecordList, sizeInBytes);
            if (f != null) {
                if (holder.producerBatch.isMeetSendCondition()) {
                    holder.transferProducerBatch(
                        ioThreadPool,
                        producerConfig,
                        clientPool,
                        retryQueue);
                }
                return f;
            } else {
                holder.transferProducerBatch(
                    ioThreadPool,
                    producerConfig,
                    clientPool,
                    retryQueue);
            }
        }
        holder.producerBatch =
            new ProducerBatch(
                groupKey,
                producerConfig.getBatchSizeThresholdInBytes(),
                producerConfig.getBatchCountThreshold(),
                System.currentTimeMillis());
        ListenableFuture<Result> f = holder.producerBatch.tryAppend(logRecordList, sizeInBytes);

        if (holder.producerBatch.isMeetSendCondition()) {
            holder.transferProducerBatch(
                ioThreadPool,
                producerConfig,
                clientPool,
                retryQueue);
        }
        return f;
    }

    public ExpiredBatches expiredBatches() {
        long nowMs = System.currentTimeMillis();
        ExpiredBatches expiredBatches = new ExpiredBatches();
        long remainingMs = producerConfig.getLingerMs();
        for (Map.Entry<GroupKey, ProducerBatchHolder> entry : batches.entrySet()) {
            ProducerBatchHolder holder = entry.getValue();
            synchronized (holder) {
                if (holder.producerBatch == null) {
                    continue;
                }
                long curRemainingMs = holder.producerBatch.remainingMs(nowMs, producerConfig.getLingerMs());
                if (curRemainingMs <= 0) {
                    holder.transferProducerBatch(expiredBatches);
                } else {
                    remainingMs = Math.min(remainingMs, curRemainingMs);
                }
            }
        }
        expiredBatches.setRemainingMs(remainingMs);
        return expiredBatches;
    }

    public List<ProducerBatch> remainingBatches() {
        if (!closed) {
            throw new IllegalStateException(
                "cannot get the remaining batches before the log accumulator closed");
        }
        List<ProducerBatch> remainingBatches = new ArrayList<ProducerBatch>();
        while (appendsInProgress()) {
            drainTo(remainingBatches);
        }
        drainTo(remainingBatches);
        batches.clear();
        return remainingBatches;
    }

    private int drainTo(List<ProducerBatch> c) {
        int n = 0;
        for (Map.Entry<GroupKey, ProducerBatchHolder> entry : batches.entrySet()) {
            ProducerBatchHolder holder = entry.getValue();
            synchronized (holder) {
                if (holder.producerBatch == null) {
                    continue;
                }
                c.add(holder.producerBatch);
                ++n;
                holder.producerBatch = null;
            }
        }
        return n;
    }

    private void ensureValidLogSize(int sizeInBytes) throws LogSizeTooLargeException {
        if (sizeInBytes > ProducerConfig.MAX_BATCH_SIZE_IN_BYTES) {
            throw new LogSizeTooLargeException(
                "the logs is "
                    + sizeInBytes
                    + " bytes which is larger than MAX_BATCH_SIZE_IN_BYTES "
                    + ProducerConfig.MAX_BATCH_SIZE_IN_BYTES);
        }
        if (sizeInBytes > producerConfig.getTotalSizeInBytes()) {
            throw new LogSizeTooLargeException(
                "the logs is "
                    + sizeInBytes
                    + " bytes which is larger than the totalSizeInBytes you specified");
        }
    }

    private ProducerBatchHolder getOrCreateProducerBatchHolder(GroupKey groupKey) {
        ProducerBatchHolder holder = batches.get(groupKey);
        if (holder != null) {
            return holder;
        }
        holder = new ProducerBatchHolder();
        ProducerBatchHolder previous = batches.putIfAbsent(groupKey, holder);
        if (previous == null) {
            return holder;
        } else {
            return previous;
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        this.closed = true;
    }

    private boolean appendsInProgress() {
        return appendsInProgress.get() > 0;
    }

    private static final class ProducerBatchHolder {

        ProducerBatch producerBatch;

        void transferProducerBatch(
            IoThreadPool ioThreadPool,
            ProducerConfig producerConfig,
            Map<String, Client> clientPool,
            RetryQueue retryQueue) {
            if (producerBatch == null) {
                return;
            }
            ioThreadPool.submit(
                new SendProducerBatchTask(
                    producerBatch,
                    producerConfig,
                    clientPool,
                    retryQueue
                ));
            producerBatch = null;
        }

        void transferProducerBatch(ExpiredBatches expiredBatches) {
            if (producerBatch == null) {
                return;
            }
            expiredBatches.add(producerBatch);
            producerBatch = null;
        }
    }
}
