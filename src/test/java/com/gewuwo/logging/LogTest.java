package com.gewuwo.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

/**
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/6 3:18 下午
 */
public class LogTest {

    private static final Logger LOGGER = LogManager.getLogger(LogTest.class);

    @Test
    public void testLogAppender() throws InterruptedException {
        LOGGER.info("testLogAppender start");
        try {
            String fit = getTestString("fit");
            if (fit.contains("f")) {
                throw new RuntimeException("抛出一个异常试试-gjs");
            }
        } catch (RuntimeException e) {
            LOGGER.error("testLogAppender error", e);
        }

        Thread.sleep(20000L);

        LOGGER.info("testLogAppender end");
    }


    public String getTestString(String msg) {
        return msg + "test";
    }

}
