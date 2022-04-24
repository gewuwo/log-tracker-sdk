package com.gewuwo.logging.tracker.collect;

import com.gewuwo.logging.tracker.client.Client;
import com.gewuwo.logging.tracker.model.LogTrackerRecord;
import com.google.common.math.LongMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author gewuwo
 * @version 1.0
 * @since 2022/1/7 5:34 下午
 */
public class SendProducerBatchTask implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(SendProducerBatchTask.class);

    private final ProducerBatch batch;

    private final ProducerConfig producerConfig;

    private final Map<String, Client> clientPool;

    private final RetryQueue retryQueue;

    public SendProducerBatchTask(
        ProducerBatch batch,
        ProducerConfig producerConfig,
        Map<String, Client> clientPool,
        RetryQueue retryQueue) {
        this.batch = batch;
        this.producerConfig = producerConfig;
        this.clientPool = clientPool;
        this.retryQueue = retryQueue;
    }

    @Override
    public void run() {
        try {
            sendProducerBatch();
        } catch (Throwable t) {
            LOGGER.error(
                "Uncaught error in send producer batch task, project="
                    + batch.getProject()
                    + ", e=",
                t);
        }
    }

    /**
     * 批量发送日志
     */
    private void sendProducerBatch() {
        LOGGER.trace("Prepare to send producer batch, batch={}", batch);
        String project = batch.getProject();
        Client client = getClient(project);
        if (client == null) {
            LOGGER.error("Failed to get client, project={}", project);
        } else {
            try {
                List<LogTrackerRecord> logTrackerRecordList = buildPutLogsRequest(batch);
                client.sendRequest(logTrackerRecordList);
            } catch (Exception e) {
                LOGGER.error(
                    "Failed to put logs, project="
                        + batch.getProject()
                        + ", e=",
                    e);
                batch.appendAttempt();
                if (meetFailureCondition(e)) {
                    LOGGER.debug("Prepare to put batch to the failure queue");
                } else {
                    LOGGER.debug("Prepare to put batch to the retry queue");
                    long retryBackoffMs = calculateRetryBackoffMs();
                    LOGGER.debug(
                        "Calculate the retryBackoffMs successfully, retryBackoffMs=" + retryBackoffMs);
                    batch.setNextRetryMs(System.currentTimeMillis() + retryBackoffMs);
                    try {
                        retryQueue.put(batch);
                    } catch (IllegalStateException e1) {
                        LOGGER.error(
                            "Failed to put batch to the retry queue, project="
                                + batch.getProject()
                                + ", e=",
                            e);
                        if (retryQueue.isClosed()) {
                            LOGGER.info(
                                "Prepare to put batch to the failure queue since the retry queue was closed");
                        }
                    }
                }
                return;
            }
            batch.appendAttempt();
            LOGGER.trace("Send producer batch successfully, batch={}", batch);
        }
    }

    private Client getClient(String project) {
        return clientPool.get(project);
    }

    private List<LogTrackerRecord> buildPutLogsRequest(ProducerBatch batch) {

        return batch.getBatchLogRecordList();
    }

    /**
     * 是否满足失败条件
     *
     * @param e 异常信息
     * @return 是否满足
     */
    private boolean meetFailureCondition(Exception e) {
        if (!isRetryableException(e)) {
            return true;
        }
        if (retryQueue.isClosed()) {
            return true;
        }
        return (batch.getRetries() >= producerConfig.getRetries());
    }

    /**
     * 判断是否可重试异常
     *
     * @param e 异常信息
     * @return 是否可重试
     */
    private boolean isRetryableException(Exception e) {
        return Objects.nonNull(e.getCause()) && e.getCause() instanceof SocketTimeoutException;
    }


    private long calculateRetryBackoffMs() {
        int retry = batch.getRetries();
        long retryBackoffMs = producerConfig.getBaseRetryBackoffMs() * LongMath.pow(2, retry);
        if (retryBackoffMs <= 0) {
            retryBackoffMs = producerConfig.getMaxRetryBackoffMs();
        }
        return Math.min(retryBackoffMs, producerConfig.getMaxRetryBackoffMs());
    }
}
