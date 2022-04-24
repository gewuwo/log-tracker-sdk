package com.gewuwo.logging.tracker.collect;

import com.gewuwo.logging.tracker.client.Client;
import com.gewuwo.logging.tracker.client.ClientEnum;
import com.gewuwo.logging.tracker.errors.MaxBatchCountExceedException;
import com.gewuwo.logging.tracker.errors.ProducerException;
import com.gewuwo.logging.tracker.model.LogTrackerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志发送者
 *
 * @author gewuwo
 * @version 1.0
 * @since 2022/1/7 4:35 下午
 */
public class LogSender {

    private static final Logger LOGGER = LogManager.getLogger(LogSender.class);

    private static final String LOG_PRODUCER_PREFIX = "tracker-log-sender-";

    private static final String MOVER_SUFFIX = "-mover";

    private static final AtomicInteger INSTANCE_ID_GENERATOR = new AtomicInteger(0);

    private final RetryQueue retryQueue;

    private final IoThreadPool ioThreadPool;

    private final LogAccumulator accumulator;

    private final Mover mover;

    private final Map<String, Client> clientPool = new ConcurrentHashMap<>();


    public LogSender(ProducerConfig producerConfig) {
        int instanceId = INSTANCE_ID_GENERATOR.getAndIncrement();
        String name = LOG_PRODUCER_PREFIX + instanceId;
        Semaphore memoryController = new Semaphore(producerConfig.getTotalSizeInBytes());
        this.retryQueue = new RetryQueue();
        this.ioThreadPool = new IoThreadPool(producerConfig.getIoThreadCount(), name);
        this.accumulator =
            new LogAccumulator(
                producerConfig,
                this.clientPool,
                memoryController,
                this.retryQueue,
                this.ioThreadPool);
        this.mover =
            new Mover(
                name + MOVER_SUFFIX,
                producerConfig,
                this.clientPool,
                this.accumulator,
                this.retryQueue,
                this.ioThreadPool);
        this.mover.start();
    }


    /**
     * 构建配置信息
     *
     * @param project    项目名
     * @param sendClient 客户端类型
     * @param url        发送的url
     */
    public void putProjectConfig(String project, String sendClient, String url) {
        ClientEnum clientEnum = ClientEnum.CLIENT_MAP.get(sendClient);
        Client client = null;

        if (clientEnum != null) {
            try {
                Class<?> clientClass = Class.forName(clientEnum.getClientClassName());
                Constructor<?> constructor = clientClass.getConstructor(String.class);
                client = (Client) constructor.newInstance(url);
            } catch (Exception e) {
                LOGGER.info("logTracker 获取发送客户端失败:{}", e.getMessage());
            }
        }

        if (client != null) {
            clientPool.put(project, client);
        }
    }

    /**
     * 发送日志
     *
     * @param project       项目名
     * @param logRecordList 日志数据
     * @throws IllegalArgumentException 没有日志数据
     * @throws InterruptedException     线程中断
     * @throws ProducerException        Producer 自定义异常
     */
    public void send(String project, List<LogTrackerRecord> logRecordList) throws InterruptedException, ProducerException {
        if (logRecordList == null || logRecordList.size() == 0) {
            throw new IllegalArgumentException("logRecordList cannot be empty");
        }

        int count = logRecordList.size();
        if (count > ProducerConfig.MAX_BATCH_COUNT) {
            throw new MaxBatchCountExceedException(
                "the log list size is "
                    + count
                    + " which exceeds the MAX_BATCH_COUNT "
                    + ProducerConfig.MAX_BATCH_COUNT);
        }
        accumulator.append(project, logRecordList);
    }


    /**
     * 关闭sender
     *
     * @throws InterruptedException 线程中断
     * @throws ProducerException    Producer自定义异常
     */
    public void close() throws InterruptedException, ProducerException {
        close(Long.MAX_VALUE);
    }


    /**
     * 关闭 sender
     *
     * @param timeoutMs timeout 毫秒数
     * @throws InterruptedException 线程中断
     * @throws ProducerException    Producer自定义异常
     */
    public void close(long timeoutMs) throws InterruptedException, ProducerException {
        if (timeoutMs < 0) {
            throw new IllegalArgumentException(
                "timeoutMs must be greater than or equal to 0, got " + timeoutMs);
        }
        ProducerException firstException = null;
        LOGGER.info("Closing the log producer, timeoutMs={}", timeoutMs);
        try {
            timeoutMs = closeMover(timeoutMs);
        } catch (ProducerException e) {
            firstException = e;
        }
        LOGGER.debug("After close mover, timeoutMs={}", timeoutMs);
        try {
            timeoutMs = closeIoThreadPool(timeoutMs);
        } catch (ProducerException e) {
            if (firstException == null) {
                firstException = e;
            }
        }
        LOGGER.debug("After close ioThreadPool, timeoutMs={}", timeoutMs);

        if (firstException != null) {
            throw firstException;
        }
        LOGGER.info("The log producer has been closed");
    }

    /**
     * 关闭mover
     *
     * @param timeoutMs timeout 毫秒数
     * @return timeoutMs
     * @throws InterruptedException 线程终端
     * @throws ProducerException    Producer自定义异常
     */
    private long closeMover(long timeoutMs) throws InterruptedException, ProducerException {
        long startMs = System.currentTimeMillis();
        accumulator.close();
        retryQueue.close();
        mover.close();
        mover.join(timeoutMs);
        if (mover.isAlive()) {
            LOGGER.warn("The mover thread is still alive");
            throw new ProducerException("the mover thread is still alive");
        }
        long nowMs = System.currentTimeMillis();
        return Math.max(0, timeoutMs - nowMs + startMs);
    }


    /**
     * 关闭IOThreadPool
     *
     * @param timeoutMs timeout 毫秒数
     * @return timeout
     * @throws InterruptedException 线程终端
     * @throws ProducerException    Producer自定义异常
     */
    private long closeIoThreadPool(long timeoutMs) throws InterruptedException, ProducerException {
        long startMs = System.currentTimeMillis();
        ioThreadPool.shutdown();
        if (ioThreadPool.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
            LOGGER.debug("The ioThreadPool is terminated");
        } else {
            LOGGER.warn("The ioThreadPool is not fully terminated");
            throw new ProducerException("the ioThreadPool is not fully terminated");
        }
        long nowMs = System.currentTimeMillis();
        return Math.max(0, timeoutMs - nowMs + startMs);
    }


}
