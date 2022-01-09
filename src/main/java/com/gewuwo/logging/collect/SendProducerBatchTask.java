package com.gewuwo.logging.collect;

import com.gewuwo.logging.client.Client;
import com.gewuwo.logging.errors.RetriableErrors;
import com.gewuwo.logging.model.LogTrackerRecord;
import com.google.common.math.LongMath;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/7 5:34 下午
 */
public class SendProducerBatchTask implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(SendProducerBatchTask.class);

    private final ProducerBatch batch;

    private final ProducerConfig producerConfig;

    private final Map<String, Client> clientPool;

    private final RetryQueue retryQueue;

    private final AtomicInteger batchCount;

    public SendProducerBatchTask(
        ProducerBatch batch,
        ProducerConfig producerConfig,
        Map<String, Client> clientPool,
        RetryQueue retryQueue,
        AtomicInteger batchCount) {
        this.batch = batch;
        this.producerConfig = producerConfig;
        this.clientPool = clientPool;
        this.retryQueue = retryQueue;
        this.batchCount = batchCount;
    }

    @Override
    public void run() {
        try {
            sendProducerBatch(System.currentTimeMillis());
        } catch (Throwable t) {
            LOGGER.error(
                "Uncaught error in send producer batch task, project="
                    + batch.getProject()
                    + ", e=",
                t);
        }
    }

    private void sendProducerBatch(long nowMs) throws InterruptedException {
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

        return batch.getLogItems();
    }



    private boolean meetFailureCondition(Exception e) {
        if (!isRetriableException(e)) {
            return true;
        }
        if (retryQueue.isClosed()) {
            return true;
        }
        return (batch.getRetries() >= producerConfig.getRetries());
    }

    private boolean isRetriableException(Exception e) {
        if (e instanceof ConnectTimeoutException) {
            return true;
        }
        return false;
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
