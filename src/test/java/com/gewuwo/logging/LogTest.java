package com.gewuwo.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * @author gewuwo
 * @version 1.0
 * @since 2022/1/6 3:18 下午
 */
public class LogTest {

    private static final Logger LOGGER = LogManager.getLogger(LogTest.class);

    @Test
    public void testLogAppender() throws InterruptedException {
        LOGGER.info("testLogAppender start");
        try {
            doTest();
        } catch (RuntimeException e) {
            LOGGER.error("testLogAppender error", e);
        }

        Thread.sleep(20000L);

        LOGGER.info("testLogAppender end");
    }


    public String getTestString(String msg) {
        return msg + "test";
    }

    class Worker implements Runnable{

        CyclicBarrier cyclicBarrier;

        public Worker(CyclicBarrier cyclicBarrier){
            this.cyclicBarrier = cyclicBarrier;
        }

        @Override
        public void run() {
            try {
                cyclicBarrier.await(); // 等待其它线程
                System.out.println(Thread.currentThread().getName() + "启动@" + System.currentTimeMillis());
                testError();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    public void testError(){
        try {
            throw new RuntimeException("抛出一个异常试试-gjs");
        }catch (Exception e){
            LOGGER.error("打印一个异常信息:",e);
        }
    }

    public void doTest() throws InterruptedException {
        final int N = 200; // 线程数
        CyclicBarrier cyclicBarrier = new CyclicBarrier(N);
        for (int j = 0; j < 3000; j++) {
            for(int i=0;i<N;i++){
                new Thread(new Worker(cyclicBarrier)).start();
            }
        }

    }

}
