package com.landawn.abacus.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class JDKLogger100Test extends TestBase {

    private JDKLogger logger;
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
        logger = new JDKLogger("test.jdk.logger");
        jdkLogger = java.util.logging.Logger.getLogger("test.jdk.logger");

        // Remove existing handlers
        Handler[] handlers = jdkLogger.getHandlers();
        for (Handler handler : handlers) {
            jdkLogger.removeHandler(handler);
        }

        // Add test handler
        testHandler = new TestHandler();
        jdkLogger.addHandler(testHandler);
        jdkLogger.setLevel(Level.ALL);
        jdkLogger.setUseParentHandlers(false);
    }

    @Test
    @DisplayName("Test constructor and getName")
    public void testConstructorAndGetName() {
        JDKLogger logger = new JDKLogger("com.test.MyLogger");
        assertEquals("com.test.MyLogger", logger.getName());
    }

    // TRACE level tests (mapped to FINEST)
    @Test
    @DisplayName("Test isTraceEnabled")
    public void testIsTraceEnabled() {
        jdkLogger.setLevel(Level.FINEST);
        assertTrue(logger.isTraceEnabled());

        jdkLogger.setLevel(Level.FINE);
        assertFalse(logger.isTraceEnabled());
    }

    @Test
    @DisplayName("Test trace methods")
    public void testTraceMethods() {
        jdkLogger.setLevel(Level.FINEST);

        // Test trace(String)
        logger.trace("Trace message");
        assertEquals(1, testHandler.records.size());
        assertEquals(Level.FINEST, testHandler.records.get(0).getLevel());
        assertEquals("Trace message", testHandler.records.get(0).getMessage());

        // Test trace(String, Throwable)
        testHandler.records.clear();
        Exception ex = new Exception("Test exception");
        logger.trace("Trace with exception", ex);
        assertEquals(1, testHandler.records.size());
        assertEquals("Trace with exception", testHandler.records.get(0).getMessage());
        assertSame(ex, testHandler.records.get(0).getThrown());
    }

    // DEBUG level tests (mapped to FINE)
    @Test
    @DisplayName("Test isDebugEnabled")
    public void testIsDebugEnabled() {
        jdkLogger.setLevel(Level.FINE);
        assertTrue(logger.isDebugEnabled());

        jdkLogger.setLevel(Level.INFO);
        assertFalse(logger.isDebugEnabled());
    }

    @Test
    @DisplayName("Test debug methods")
    public void testDebugMethods() {
        jdkLogger.setLevel(Level.FINE);

        // Test debug(String)
        logger.debug("Debug message");
        assertEquals(1, testHandler.records.size());
        assertEquals(Level.FINE, testHandler.records.get(0).getLevel());
        assertEquals("Debug message", testHandler.records.get(0).getMessage());

        // Test debug(String, Throwable)
        testHandler.records.clear();
        RuntimeException ex = new RuntimeException("Debug exception");
        logger.debug("Debug with exception", ex);
        assertEquals(1, testHandler.records.size());
        assertEquals("Debug with exception", testHandler.records.get(0).getMessage());
        assertSame(ex, testHandler.records.get(0).getThrown());
    }

    // INFO level tests
    @Test
    @DisplayName("Test isInfoEnabled")
    public void testIsInfoEnabled() {
        jdkLogger.setLevel(Level.INFO);
        assertTrue(logger.isInfoEnabled());

        jdkLogger.setLevel(Level.WARNING);
        assertFalse(logger.isInfoEnabled());
    }

    @Test
    @DisplayName("Test info methods")
    public void testInfoMethods() {
        jdkLogger.setLevel(Level.INFO);

        // Test info(String)
        logger.info("Info message");
        assertEquals(1, testHandler.records.size());
        assertEquals(Level.INFO, testHandler.records.get(0).getLevel());
        assertEquals("Info message", testHandler.records.get(0).getMessage());

        // Test info(String, Throwable)
        testHandler.records.clear();
        IllegalArgumentException ex = new IllegalArgumentException("Info exception");
        logger.info("Info with exception", ex);
        assertEquals(1, testHandler.records.size());
        assertEquals("Info with exception", testHandler.records.get(0).getMessage());
        assertSame(ex, testHandler.records.get(0).getThrown());
    }

    // WARN level tests (mapped to WARNING)
    @Test
    @DisplayName("Test isWarnEnabled")
    public void testIsWarnEnabled() {
        jdkLogger.setLevel(Level.WARNING);
        assertTrue(logger.isWarnEnabled());

        jdkLogger.setLevel(Level.SEVERE);
        assertFalse(logger.isWarnEnabled());
    }

    @Test
    @DisplayName("Test warn methods")
    public void testWarnMethods() {
        jdkLogger.setLevel(Level.WARNING);

        // Test warn(String)
        logger.warn("Warning message");
        assertEquals(1, testHandler.records.size());
        assertEquals(Level.WARNING, testHandler.records.get(0).getLevel());
        assertEquals("Warning message", testHandler.records.get(0).getMessage());

        // Test warn(String, Throwable)
        testHandler.records.clear();
        IllegalStateException ex = new IllegalStateException("Warning exception");
        logger.warn("Warning with exception", ex);
        assertEquals(1, testHandler.records.size());
        assertEquals("Warning with exception", testHandler.records.get(0).getMessage());
        assertSame(ex, testHandler.records.get(0).getThrown());
    }

    // ERROR level tests (mapped to SEVERE)
    @Test
    @DisplayName("Test isErrorEnabled")
    public void testIsErrorEnabled() {
        jdkLogger.setLevel(Level.SEVERE);
        assertTrue(logger.isErrorEnabled());

        jdkLogger.setLevel(Level.OFF);
        assertFalse(logger.isErrorEnabled());
    }

    @Test
    @DisplayName("Test error methods")
    public void testErrorMethods() {
        jdkLogger.setLevel(Level.SEVERE);

        // Test error(String)
        logger.error("Error message");
        assertEquals(1, testHandler.records.size());
        assertEquals(Level.SEVERE, testHandler.records.get(0).getLevel());
        assertEquals("Error message", testHandler.records.get(0).getMessage());

        // Test error(String, Throwable)
        testHandler.records.clear();
        Error err = new Error("Error exception");
        logger.error("Error with exception", err);
        assertEquals(1, testHandler.records.size());
        assertEquals("Error with exception", testHandler.records.get(0).getMessage());
        assertSame(err, testHandler.records.get(0).getThrown());
    }

    @Test
    @DisplayName("Test caller location information")
    public void testCallerLocation() {
        jdkLogger.setLevel(Level.ALL);

        logger.info("Test caller location");
        assertEquals(1, testHandler.records.size());

        LogRecord record = testHandler.records.get(0);
        // The source class should be this test class, not the logger implementation
        assertEquals(JDKLogger100Test.class.getName(), record.getSourceClassName());
        assertEquals("testCallerLocation", record.getSourceMethodName());
    }

    @Test
    @DisplayName("Test logging with null messages")
    public void testNullMessages() {
        jdkLogger.setLevel(Level.ALL);

        // Test null message
        logger.info((String) null);
        assertEquals(1, testHandler.records.size());
        assertEquals(null, testHandler.records.get(0).getMessage());

        // Test null message with exception
        testHandler.records.clear();
        Exception ex = new Exception("Test");
        logger.error((String) null, ex);
        assertEquals(1, testHandler.records.size());
        assertEquals(null, testHandler.records.get(0).getMessage());
        assertSame(ex, testHandler.records.get(0).getThrown());
    }

    @Test
    @DisplayName("Test logging when disabled")
    public void testLoggingWhenDisabled() {
        // Set level to OFF
        jdkLogger.setLevel(Level.OFF);

        // Try logging at all levels
        logger.trace("trace");
        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        // Nothing should be logged
        assertEquals(0, testHandler.records.size());
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

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all messages were logged
        assertEquals(threadCount * messagesPerThread, testHandler.records.size());
    }

    @Test
    @DisplayName("Test logger name in LogRecord")
    public void testLoggerNameInRecord() {
        jdkLogger.setLevel(Level.ALL);

        logger.info("Test logger name");
        assertEquals(1, testHandler.records.size());
        assertEquals("test.jdk.logger", testHandler.records.get(0).getLoggerName());
    }
}
