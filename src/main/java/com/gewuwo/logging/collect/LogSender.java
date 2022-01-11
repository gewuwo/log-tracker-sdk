package com.gewuwo.logging.collect;

import com.gewuwo.logging.client.Client;
import com.gewuwo.logging.client.ClientEnum;
import com.gewuwo.logging.errors.ProducerException;
import com.gewuwo.logging.model.LogTrackerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志发送者
 *
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/7 4:35 下午
 */
public class LogSender {

	private static final Logger LOGGER = LogManager.getLogger(LogSender.class);



	private static final String LOG_PRODUCER_PREFIX = "fittime-log-sender-";

	private static final String MOVER_SUFFIX = "-mover";

	private static final AtomicInteger INSTANCE_ID_GENERATOR = new AtomicInteger(0);

	private final int instanceId;

	private final String name;

	private final ProducerConfig producerConfig;

	private final Semaphore memoryController;

	private final RetryQueue retryQueue;

	private final IoThreadPool ioThreadPool;

	private final LogAccumulator accumulator;

	private final Mover mover;

	private final AtomicInteger batchCount = new AtomicInteger(0);

	private final Map<String, Client> clientPool = new ConcurrentHashMap<>();

	public LogSender(ProducerConfig producerConfig) {
		this.instanceId = INSTANCE_ID_GENERATOR.getAndIncrement();
		this.name = LOG_PRODUCER_PREFIX + this.instanceId;
		this.producerConfig = producerConfig;
		this.memoryController = new Semaphore(producerConfig.getTotalSizeInBytes());
		this.retryQueue = new RetryQueue();
		this.ioThreadPool = new IoThreadPool(producerConfig.getIoThreadCount(), this.name);
		this.accumulator =
			new LogAccumulator(
				producerConfig,
				this.clientPool,
				this.memoryController,
				this.retryQueue,
				this.ioThreadPool,
				this.batchCount);
		this.mover =
			new Mover(
				this.name + MOVER_SUFFIX,
				producerConfig,
				this.clientPool,
				this.accumulator,
				this.retryQueue,
				this.ioThreadPool,
				this.batchCount);
		this.mover.start();
	}


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

	public void send(String project, List<LogTrackerRecord> logRecordList) throws InterruptedException, ProducerException {
		accumulator.append(project, logRecordList);
	}

	public void close() throws InterruptedException, ProducerException {
		close(Long.MAX_VALUE);
	}


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
	 * @param timeoutMs
	 * @return
	 * @throws InterruptedException
	 * @throws ProducerException
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
	 * @param timeoutMs
	 * @return
	 * @throws InterruptedException
	 * @throws ProducerException
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
