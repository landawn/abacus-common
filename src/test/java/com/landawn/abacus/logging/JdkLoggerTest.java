package com.landawn.abacus.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class JdkLoggerTest extends TestBase {

    private JdkLogger logger;
    private TestHandler testHandler;
    private java.util.logging.Logger jdkLogger;

    private static class TestHandler extends Handler {
        public final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            synchronized (records) {
                records.add(record);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    @BeforeEach
    public void setUp() {
        logger = new JdkLogger("test.jdk.logger");
        jdkLogger = java.util.logging.Logger.getLogger("test.jdk.logger");

        Handler[] handlers = jdkLogger.getHandlers();
        for (Handler handler : handlers) {
            jdkLogger.removeHandler(handler);
        }

        testHandler = new TestHandler();
        jdkLogger.addHandler(testHandler);
        jdkLogger.setLevel(Level.ALL);
        jdkLogger.setUseParentHandlers(false);
    }

    @Test
    @DisplayName("Test constructor and getName")
    public void testConstructorAndGetName() {
        JdkLogger logger = new JdkLogger("com.test.MyLogger");
        assertEquals("com.test.MyLogger", logger.getName());
    }

    @Test
    public void testRootNameUsesNativeRootLogger() throws Exception {
        JdkLogger logger = new JdkLogger(Logger.ROOT_LOGGER_NAME);
        Field loggerImplField = JdkLogger.class.getDeclaredField("loggerImpl");
        loggerImplField.setAccessible(true);

        assertSame(java.util.logging.Logger.getLogger(""), loggerImplField.get(logger));
        assertEquals(Logger.ROOT_LOGGER_NAME, logger.getName());
    }

    @Test
    @DisplayName("Test creating multiple loggers with different names")
    public void testMultipleLoggerInstances() {
        JdkLogger logger1 = new JdkLogger("com.test.Logger1");
        JdkLogger logger2 = new JdkLogger("com.test.Logger2");
        assertEquals("com.test.Logger1", logger1.getName());
        assertEquals("com.test.Logger2", logger2.getName());
    }

    @Test
    public void testIsEnabledAtEachLevel() {
        jdkLogger.setLevel(Level.FINEST);
        assertTrue(logger.isTraceEnabled());
        jdkLogger.setLevel(Level.FINE);
        assertFalse(logger.isTraceEnabled());
        assertTrue(logger.isDebugEnabled());
        jdkLogger.setLevel(Level.INFO);
        assertFalse(logger.isDebugEnabled());
        assertTrue(logger.isInfoEnabled());
        jdkLogger.setLevel(Level.WARNING);
        assertFalse(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        jdkLogger.setLevel(Level.SEVERE);
        assertFalse(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
        jdkLogger.setLevel(Level.OFF);
        assertFalse(logger.isErrorEnabled());
    }

    @Test
    public void testEachLevelLogsMessageAndThrowable() {
        jdkLogger.setLevel(Level.ALL);
        Exception ex = new Exception("boom");

        logger.trace("trace");
        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");
        assertEquals(5, testHandler.records.size());
        assertEquals(Level.FINEST, testHandler.records.get(0).getLevel());
        assertEquals(Level.FINE, testHandler.records.get(1).getLevel());
        assertEquals(Level.INFO, testHandler.records.get(2).getLevel());
        assertEquals(Level.WARNING, testHandler.records.get(3).getLevel());
        assertEquals(Level.SEVERE, testHandler.records.get(4).getLevel());
        assertEquals("trace", testHandler.records.get(0).getMessage());

        testHandler.records.clear();
        logger.trace("t-ex", ex);
        logger.debug("d-ex", ex);
        logger.info("i-ex", ex);
        logger.warn("w-ex", ex);
        logger.error("e-ex", ex);
        assertEquals(5, testHandler.records.size());
        assertSame(ex, testHandler.records.get(0).getThrown());
        assertSame(ex, testHandler.records.get(4).getThrown());
        assertEquals("e-ex", testHandler.records.get(4).getMessage());
    }

    @Test
    public void testCallerLocationAtEachLevel() {
        jdkLogger.setLevel(Level.ALL);
        logger.trace("trace caller");
        logger.debug("debug caller");
        logger.info("info caller");
        logger.warn("warn caller");
        logger.error("error caller");
        assertEquals(5, testHandler.records.size());
        for (LogRecord record : testHandler.records) {
            assertEquals(JdkLoggerTest.class.getName(), record.getSourceClassName());
            assertEquals("testCallerLocationAtEachLevel", record.getSourceMethodName());
        }

        testHandler.records.clear();
        Exception ex = new Exception("test");
        logger.trace("trace caller ex", ex);
        assertEquals(JdkLoggerTest.class.getName(), testHandler.records.get(0).getSourceClassName());
        assertEquals("testCallerLocationAtEachLevel", testHandler.records.get(0).getSourceMethodName());
        assertSame(ex, testHandler.records.get(0).getThrown());
    }

    @Test
    @DisplayName("Test logging when disabled")
    public void testLoggingWhenDisabled() {
        jdkLogger.setLevel(Level.OFF);

        logger.trace("trace");
        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        assertEquals(0, testHandler.records.size());
    }

    @Test
    public void testLoggerNameEmptyAndNullMessages() {
        jdkLogger.setLevel(Level.ALL);
        logger.info("Test logger name");
        assertEquals("test.jdk.logger", testHandler.records.get(0).getLoggerName());

        testHandler.records.clear();
        logger.info("");
        assertEquals("", testHandler.records.get(0).getMessage());

        testHandler.records.clear();
        logger.info((String) null);
        assertEquals(null, testHandler.records.get(0).getMessage());

        testHandler.records.clear();
        Exception ex = new Exception("Test");
        logger.error((String) null, ex);
        assertEquals(null, testHandler.records.get(0).getMessage());
        assertSame(ex, testHandler.records.get(0).getThrown());
    }

    @Test
    @DisplayName("Test concurrent logging")
    public void testConcurrentLogging() throws InterruptedException {
        jdkLogger.setLevel(Level.ALL);

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

        assertEquals(threadCount * messagesPerThread, testHandler.records.size());
    }

    @Test
    @DisplayName("Test SELF and SUPER constants")
    public void testSelfAndSuperConstants() {
        assertEquals(JdkLogger.class.getName(), JdkLogger.SELF);
        assertEquals("com.landawn.abacus.logging.AbstractLogger", JdkLogger.SUPER);
    }

    @Test
    @DisplayName("Test logger name is correct")
    public void testLoggerName() {
        assertEquals("test.jdk.logger", logger.getName());
    }

    // ---- review fixes 2026-09-06 (a11 F-5): root facade records carry JUL's root logger name ----

    @Test
    public void testRootFacadeRecordCarriesJulRootLoggerName() {
        final java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        final Level oldLevel = root.getLevel();
        final TestHandler rootHandler = new TestHandler();
        root.addHandler(rootHandler);
        root.setLevel(Level.ALL);
        try {
            new JdkLogger(Logger.ROOT_LOGGER_NAME).info("via root facade \u03bb");
            new JdkLogger("").info("via empty-name facade");

            LogRecord viaFacade = null;
            LogRecord viaEmpty = null;
            synchronized (rootHandler.records) {
                for (LogRecord record : rootHandler.records) {
                    if ("via root facade \u03bb".equals(record.getMessage())) {
                        viaFacade = record;
                    } else if ("via empty-name facade".equals(record.getMessage())) {
                        viaEmpty = record;
                    }
                }
            }

            assertTrue(viaFacade != null && viaEmpty != null, "both records must reach the root handler");
            assertEquals("", viaFacade.getLoggerName(), "the root facade must stamp JUL's root logger name, not \"ROOT\"");
            assertEquals("", viaEmpty.getLoggerName());
        } finally {
            root.removeHandler(rootHandler);
            root.setLevel(oldLevel);
        }

        // Regression guard: a normal name is unchanged, and the facade's own name is still "ROOT".
        logger.info("normal name");
        assertEquals(1, testHandler.records.size());
        assertEquals("test.jdk.logger", testHandler.records.get(0).getLoggerName());
        assertEquals(Logger.ROOT_LOGGER_NAME, new JdkLogger(Logger.ROOT_LOGGER_NAME).getName());
    }
}
