package com.landawn.abacus.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class LoggerFactory100Test extends TestBase {

    @BeforeEach
    public void setUp() {
    }

    @Test
    @DisplayName("Test getLogger with Class parameter")
    public void testGetLoggerWithClass() {
        Logger logger = LoggerFactory.getLogger(LoggerFactory100Test.class);
        assertNotNull(logger);
        assertEquals(LoggerFactory100Test.class.getName(), logger.getName());

        Logger logger2 = LoggerFactory.getLogger(LoggerFactory100Test.class);
        assertSame(logger, logger2);
    }

    @Test
    @DisplayName("Test getLogger with String parameter")
    public void testGetLoggerWithString() {
        String loggerName = "com.test.MyLogger";
        Logger logger = LoggerFactory.getLogger(loggerName);
        assertNotNull(logger);
        assertEquals(loggerName, logger.getName());

        Logger logger2 = LoggerFactory.getLogger(loggerName);
        assertSame(logger, logger2);
    }

    @Test
    @DisplayName("Test getLogger with null Class throws NullPointerException")
    public void testGetLoggerWithNullClass() {
        assertThrows(NullPointerException.class, () -> {
            LoggerFactory.getLogger((Class<?>) null);
        });
    }

    @Test
    @DisplayName("Test getLogger with different names returns different loggers")
    public void testGetLoggerWithDifferentNames() {
        Logger logger1 = LoggerFactory.getLogger("logger1");
        Logger logger2 = LoggerFactory.getLogger("logger2");

        assertNotNull(logger1);
        assertNotNull(logger2);
        assertNotSame(logger1, logger2);
        assertEquals("logger1", logger1.getName());
        assertEquals("logger2", logger2.getName());
    }

    @Test
    @DisplayName("Test getLogger caching mechanism")
    public void testLoggerCaching() {
        Logger logger1 = LoggerFactory.getLogger("cached.logger");
        Logger logger2 = LoggerFactory.getLogger("cached.logger");
        Logger logger3 = LoggerFactory.getLogger(LoggerFactory100Test.class);
        Logger logger4 = LoggerFactory.getLogger(LoggerFactory100Test.class);

        assertSame(logger1, logger2);
        assertSame(logger3, logger4);
        assertNotSame(logger1, logger3);
    }

    @Test
    @DisplayName("Test logger initialization")
    public void testLoggerInitialization() {
        Logger logger = LoggerFactory.getLogger("init.test");
        assertNotNull(logger);

        assertDoesNotThrow(() -> {
            logger.info("Test message");
            logger.debug("Debug message");
            logger.error("Error message");
        });
    }

    @Test
    @DisplayName("Test concurrent logger creation")
    public void testConcurrentLoggerCreation() throws InterruptedException {
        final int threadCount = 10;
        final String loggerName = "concurrent.test";
        final Logger[] loggers = new Logger[threadCount];
        final Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                loggers[index] = LoggerFactory.getLogger(loggerName);
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Logger firstLogger = loggers[0];
        assertNotNull(firstLogger);
        for (int i = 1; i < threadCount; i++) {
            assertSame(firstLogger, loggers[i]);
        }
    }

    @Test
    @DisplayName("Test logger hierarchy naming")
    public void testLoggerHierarchyNaming() {
        Logger rootLogger = LoggerFactory.getLogger("com");
        Logger parentLogger = LoggerFactory.getLogger("com.landawn");
        Logger childLogger = LoggerFactory.getLogger("com.landawn.abacus");

        assertNotNull(rootLogger);
        assertNotNull(parentLogger);
        assertNotNull(childLogger);

        assertEquals("com", rootLogger.getName());
        assertEquals("com.landawn", parentLogger.getName());
        assertEquals("com.landawn.abacus", childLogger.getName());

        assertNotSame(rootLogger, parentLogger);
        assertNotSame(parentLogger, childLogger);
        assertNotSame(rootLogger, childLogger);
    }

    @Test
    @DisplayName("Test special logger names")
    public void testSpecialLoggerNames() {
        Logger emptyLogger = LoggerFactory.getLogger("");
        assertNotNull(emptyLogger);
        assertEquals("", emptyLogger.getName());

        Logger rootLogger = LoggerFactory.getLogger("ROOT");
        assertNotNull(rootLogger);
        assertEquals("ROOT", rootLogger.getName());

        Logger specialLogger = LoggerFactory.getLogger("com.test$Special_Logger-123");
        assertNotNull(specialLogger);
        assertEquals("com.test$Special_Logger-123", specialLogger.getName());
    }
}
