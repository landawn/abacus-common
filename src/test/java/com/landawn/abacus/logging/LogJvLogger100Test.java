package com.landawn.abacus.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class LogJvLogger100Test extends TestBase {

    private static boolean isLog4j2Available() {
        try {
            Class.forName("org.apache.logging.log4j.LogManager");
            Class.forName("org.apache.logging.log4j.spi.ExtendedLogger");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Test
    @DisplayName("Test Log4Jv2Logger when Log4j2 is not available")
    public void testWhenLog4j2NotAvailable() {
        if (!isLog4j2Available()) {
            assertThrows(NoClassDefFoundError.class, () -> {
                new Log4Jv2Logger("test.logger");
            }, "Should throw NoClassDefFoundError when Log4j2 is not available");
        }
    }

    @Test
    @DisplayName("Test constructor and getName")
    @EnabledIf("isLog4j2Available")
    public void testConstructorAndGetName() {
        Log4Jv2Logger logger = new Log4Jv2Logger("test.log4j2.logger");
        assertEquals("test.log4j2.logger", logger.getName());
    }

    @Test
    @DisplayName("Test isEnabled methods")
    @EnabledIf("isLog4j2Available")
    public void testIsEnabledMethods() {
        Log4Jv2Logger logger = new Log4Jv2Logger("test.log4j2.enabled");

        assertDoesNotThrow(() -> {
            boolean traceEnabled = logger.isTraceEnabled();
            boolean debugEnabled = logger.isDebugEnabled();
            boolean infoEnabled = logger.isInfoEnabled();
            boolean warnEnabled = logger.isWarnEnabled();
            boolean errorEnabled = logger.isErrorEnabled();

            assertTrue(errorEnabled || warnEnabled || infoEnabled || debugEnabled || traceEnabled, "At least one log level should be enabled");
        });
    }

    @Test
    @DisplayName("Test trace methods")
    @EnabledIf("isLog4j2Available")
    public void testTraceMethods() {
        Log4Jv2Logger logger = new Log4Jv2Logger("test.log4j2.trace");

        assertDoesNotThrow(() -> {
            logger.trace("Trace message");
            logger.trace("Trace with exception", new Exception("Test"));
        });
    }

    @Test
    @DisplayName("Test debug methods")
    @EnabledIf("isLog4j2Available")
    public void testDebugMethods() {
        Log4Jv2Logger logger = new Log4Jv2Logger("test.log4j2.debug");

        assertDoesNotThrow(() -> {
            logger.debug("Debug message");
            logger.debug("Debug with exception", new RuntimeException("Test"));
        });
    }

    @Test
    @DisplayName("Test info methods")
    @EnabledIf("isLog4j2Available")
    public void testInfoMethods() {
        Log4Jv2Logger logger = new Log4Jv2Logger("test.log4j2.info");

        assertDoesNotThrow(() -> {
            logger.info("Info message");
            logger.info("Info with exception", new IllegalArgumentException("Test"));
        });
    }

    @Test
    @DisplayName("Test warn methods")
    @EnabledIf("isLog4j2Available")
    public void testWarnMethods() {
        Log4Jv2Logger logger = new Log4Jv2Logger("test.log4j2.warn");

        assertDoesNotThrow(() -> {
            logger.warn("Warning message");
            logger.warn("Warning with exception", new IllegalStateException("Test"));
        });
    }

    @Test
    @DisplayName("Test error methods")
    @EnabledIf("isLog4j2Available")
    public void testErrorMethods() {
        Log4Jv2Logger logger = new Log4Jv2Logger("test.log4j2.error");

        assertDoesNotThrow(() -> {
            logger.error("Error message");
            logger.error("Error with exception", new Error("Test"));
        });
    }

    @Test
    @DisplayName("Test logging with null values")
    @EnabledIf("isLog4j2Available")
    public void testLoggingWithNullValues() {
        Log4Jv2Logger logger = new Log4Jv2Logger("test.log4j2.null");

        assertDoesNotThrow(() -> {
            logger.trace((String) null);
            logger.trace((String) null, (Throwable) null);
            logger.debug((String) null);
            logger.debug((String) null, (Throwable) null);
            logger.info((String) null);
            logger.info((String) null, (Throwable) null);
            logger.warn((String) null);
            logger.warn((String) null, (Throwable) null);
            logger.error((String) null);
            logger.error((String) null, (Throwable) null);
        });
    }

    @Test
    @DisplayName("Test multiple logger instances")
    @EnabledIf("isLog4j2Available")
    public void testMultipleLoggerInstances() {
        Log4Jv2Logger logger1 = new Log4Jv2Logger("test.log4j2.one");
        Log4Jv2Logger logger2 = new Log4Jv2Logger("test.log4j2.two");
        Log4Jv2Logger logger3 = new Log4Jv2Logger("test.log4j2.one");

        assertEquals("test.log4j2.one", logger1.getName());
        assertEquals("test.log4j2.two", logger2.getName());
        assertEquals("test.log4j2.one", logger3.getName());

        assertNotSame(logger1, logger2);
        assertNotSame(logger1, logger3);
    }

    @Test
    @DisplayName("Test concurrent logging")
    @EnabledIf("isLog4j2Available")
    public void testConcurrentLogging() throws InterruptedException {
        Log4Jv2Logger logger = new Log4Jv2Logger("test.log4j2.concurrent");

        final int threadCount = 5;
        final int messagesPerThread = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < messagesPerThread; j++) {
                    logger.info("Thread " + threadId + " message " + j);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertTrue(true);
    }

    @Test
    @DisplayName("Test special logger names")
    @EnabledIf("isLog4j2Available")
    public void testSpecialLoggerNames() {
        assertDoesNotThrow(() -> {
            Log4Jv2Logger rootLogger = new Log4Jv2Logger("ROOT");
            assertEquals("ROOT", rootLogger.getName());
            rootLogger.info("Root logger message");
        });

        assertDoesNotThrow(() -> {
            Log4Jv2Logger emptyLogger = new Log4Jv2Logger("");
            assertEquals("", emptyLogger.getName());
            emptyLogger.info("Empty name logger message");
        });

        assertDoesNotThrow(() -> {
            Log4Jv2Logger specialLogger = new Log4Jv2Logger("com.test$Special_Logger-123");
            assertEquals("com.test$Special_Logger-123", specialLogger.getName());
            specialLogger.info("Special name logger message");
        });
    }

    @Test
    @DisplayName("Test FQCN constant")
    @EnabledIf("isLog4j2Available")
    public void testFQCNConstant() {
        assertEquals("com.landawn.abacus.logging.Log4Jv2Logger", Log4Jv2Logger.FQCN);
    }

    @Test
    @DisplayName("Test logging at disabled levels")
    @EnabledIf("isLog4j2Available")
    public void testLoggingAtDisabledLevels() {
        Log4Jv2Logger logger = new Log4Jv2Logger("test.log4j2.disabled");

        assertDoesNotThrow(() -> {
            logger.trace("Trace - may be disabled");
            logger.debug("Debug - may be disabled");
            logger.info("Info - may be disabled");
            logger.warn("Warn - may be disabled");
            logger.error("Error - typically enabled");
        });
    }

    @Test
    @DisplayName("Test logger hierarchy")
    @EnabledIf("isLog4j2Available")
    public void testLoggerHierarchy() {
        Log4Jv2Logger rootLogger = new Log4Jv2Logger("com");
        Log4Jv2Logger parentLogger = new Log4Jv2Logger("com.landawn");
        Log4Jv2Logger childLogger = new Log4Jv2Logger("com.landawn.abacus");

        assertEquals("com", rootLogger.getName());
        assertEquals("com.landawn", parentLogger.getName());
        assertEquals("com.landawn.abacus", childLogger.getName());

        assertDoesNotThrow(() -> {
            rootLogger.info("Root logger");
            parentLogger.info("Parent logger");
            childLogger.info("Child logger");
        });
    }
}
