package com.landawn.abacus.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class LoggerTest extends TestBase {

    @Test
    public void testRootLoggerNameConstant() {
        assertEquals("ROOT", Logger.ROOT_LOGGER_NAME);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testFormatOverloadsSmoke() {
        final Logger logger = LoggerFactory.getLogger(LoggerTest.class);
        final Exception ex = new Exception("test");
        assertDoesNotThrow(() -> {
            for (int level = 0; level < 5; level++) {
                emit(logger, level, ex);
            }
        });
    }

    @SuppressWarnings("deprecation")
    private static void emit(final Logger logger, final int level, final Exception ex) {
        switch (level) {
            case 0 -> {
                logger.trace("**************");
                logger.trace("t {}", "a");
                logger.trace("t {} {}", "a", "b");
                logger.trace("t {} {} {}", "a", "b", "c");
                logger.trace("t {} {} {} {}", "a", "b", "c", "d");
                logger.trace("t {} {} {} {} {}", 1, 2, 3, 4, 5);
                logger.trace("t {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
                logger.trace("t {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
                logger.trace("t", "a", "b", "c", "d", "e", "f", "g", "h");
                logger.trace("ex", ex);
                logger.trace(ex, "ex first");
                logger.trace(() -> "supplier");
                logger.trace(() -> "supplier+ex", ex);
                logger.trace(ex, () -> "ex+supplier");
            }
            case 1 -> {
                logger.debug("d");
                logger.debug("d {}", "a");
                logger.debug("d {} {}", "a", "b");
                logger.debug("d {} {} {}", 1, 2, 3);
                logger.debug("d {} {} {} {}", "a", "b", "c", "d");
                logger.debug("d {} {} {} {} {}", 1, 2, 3, 4, 5);
                logger.debug("d {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
                logger.debug("d {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
                logger.debug("d", "a", "b", "c", "d", "e", "f", "g", "h");
                logger.debug("ex", ex);
                logger.debug(ex, "ex first");
                logger.debug(() -> "supplier");
                logger.debug(() -> "supplier+ex", ex);
                logger.debug(ex, () -> "ex+supplier");
            }
            case 2 -> {
                logger.info("i");
                logger.info("i {}", "a");
                logger.info("i {} {}", "a", "b");
                logger.info("i {} {} {}", 1, 2, 3);
                logger.info("i {} {} {} {}", "a", "b", "c", "d");
                logger.info("i {} {} {} {} {}", 1, 2, 3, 4, 5);
                logger.info("i {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
                logger.info("i {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
                logger.info("i", "a", "b", "c", "d", "e", "f", "g", "h");
                logger.info("ex", ex);
                logger.info(ex, "ex first");
                logger.info(() -> "supplier");
                logger.info(() -> "supplier+ex", ex);
                logger.info(ex, () -> "ex+supplier");
            }
            case 3 -> {
                logger.warn("**************");
                logger.warn("log4j v2", "a");
                logger.warn("log4j v2 %s", "a");
                logger.warn("log4j v2 {}", "a");
                logger.warn("log4j v2", "a", "b");
                logger.warn("log4j v2 %s, %s", "a", "b");
                logger.warn("log4j v2 {}, {}", "a", "b");
                logger.warn("log4j v2", "a", "b", "c");
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
                logger.warn("ex", ex);
                logger.warn(ex, "ex first");
                logger.warn(() -> "supplier");
                logger.warn(() -> "supplier+ex", ex);
                logger.warn(ex, () -> "ex+supplier");
            }
            default -> {
                logger.error("e");
                logger.error("e {}", "a");
                logger.error("e {} {}", "a", "b");
                logger.error("e {} {} {}", 1, 2, 3);
                logger.error("e {} {} {} {}", "a", "b", "c", "d");
                logger.error("e {} {} {} {} {}", 1, 2, 3, 4, 5);
                logger.error("e {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
                logger.error("e {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
                logger.error("e", "a", "b", "c", "d", "e", "f", "g", "h");
                logger.error("ex", ex);
                logger.error(ex, "ex first");
                logger.error(() -> "supplier");
                logger.error(() -> "supplier+ex", ex);
                logger.error(ex, () -> "ex+supplier");
            }
        }
    }
}
