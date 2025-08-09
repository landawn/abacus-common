package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

public class LoggerTest {
    private static final Logger logger = LoggerFactory.getLogger(LoggerTest.class);

    @Test
    public void test() {
        logger.warn("**************");

        logger.warn("log4j v2", "a");

        logger.warn("log4j v2 %s", "a");

        logger.warn("log4j v2 {}", "a");

        logger.warn("log4j v2", "a", "b");

        logger.warn("log4j v2 %s", "a", "b");

        logger.warn("log4j v2 {}", "a", "b");

        logger.warn("log4j v2 %s, %s", "a", "b");

        logger.warn("log4j v2 {}, {}", "a", "b");

        logger.warn("log4j v2", "a", "b", "c");

        logger.warn("log4j v2 %s", "a", "b", "c");

        logger.warn("log4j v2 {}", "a", "b", "c");

        logger.warn("log4j v2 %s, %s", "a", "b", "c");

        logger.warn("log4j v2 {}, {}", "a", "b", "c");

        logger.warn("log4j v2 %s, %s, %s", "a", "b", "c");

        logger.warn("log4j v2 {}, {}, {}", "a", "b", "c");

        logger.warn("log4j v2 %s, %s, %s, %s", "a", "b", "c", "d");

        logger.warn("log4j v2 {}, {}, {}, {}", "a", "b", "c", "d");

        logger.warn("log4j v2 %s, %s, %s, %s, %s", "a", "b", "c", "d", "e");

        logger.warn("log4j v2 {}, {}, {}, {}, {}", "a", "b", "c", "d", "e");

        logger.warn("log4j v2 %s, %s, %s, %s, %s, %s", "a", "b", "c", "d", "e", "f");

        logger.warn("log4j v2 {}, {}, {}, {}, {}, {}", "a", "b", "c", "d", "e", "f");

        logger.warn("log4j v2 %s, %s, %s, %s, %s, %s, %s", "a", "b", "c", "d", "e", "f", "g");

        logger.warn("log4j v2 {}, {}, {}, {}, {}, {}, {}", "a", "b", "c", "d", "e", "f", "g");

        logger.warn("log4j v2 %s, %s, %s, %s, %s, %s, %s, %s", "a", "b", "c", "d", "e", "f", "g", "h");

        logger.warn("log4j v2 {}, {}, {}, {}, {}, {}, {}, {}", "a", "b", "c", "d", "e", "f", "g", "h");

        N.sleep(1000);
    }

}
