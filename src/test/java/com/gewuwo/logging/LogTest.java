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
    public void testLogAppender() {
        LOGGER.info("testLogAppender start");
        try {
            if (1 == 1) {
                throw new RuntimeException("抛出一个异常试试");
            }
        } catch (RuntimeException e) {
            LOGGER.error("testLogAppender error", e);
        }

        LOGGER.info("testLogAppender end");
    }

}
