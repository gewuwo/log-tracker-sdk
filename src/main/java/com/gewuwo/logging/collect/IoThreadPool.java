package com.gewuwo.logging.collect;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 发送日志线程
 *
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/7 5:14 下午
 */
public class IoThreadPool {

    private static final String IO_THREAD_SUFFIX_FORMAT = "-io-thread-%d";

    private final ExecutorService ioThreadPool;

    public IoThreadPool(int ioThreadCount, String prefix) {
        this.ioThreadPool =
            Executors.newFixedThreadPool(
                ioThreadCount,
                new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat(prefix + IO_THREAD_SUFFIX_FORMAT)
                    .build());
    }

    public void submit(SendProducerBatchTask task) {
        ioThreadPool.submit(task);
    }

    public void shutdown() {
        ioThreadPool.shutdown();
    }

    public boolean isTerminated() {
        return ioThreadPool.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return ioThreadPool.awaitTermination(timeout, unit);
    }
}
