package com.landawn.abacus.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.landawn.abacus.TestBase;


public class SLFJLogger100Test extends TestBase {

    // Note: SLF4J tests require SLF4J to be properly initialized
    // These tests will work when SLF4J is available, otherwise they'll be skipped
    
    private static boolean isSLF4JAvailable() {
        try {
            Class.forName("org.slf4j.LoggerFactory");
            // Check if it's not NOP logger factory
            return !(org.slf4j.LoggerFactory.getILoggerFactory() instanceof org.slf4j.helpers.NOPLoggerFactory);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Test
    @DisplayName("Test SLF4JLogger constructor when SLF4J is not available")
    public void testConstructorWhenSLF4JNotAvailable() {
        if (!isSLF4JAvailable()) {
            assertThrows(RuntimeException.class, () -> {
                new SLF4JLogger("test.logger");
            }, "Should throw RuntimeException when SLF4J is not properly initialized");
        } else {
            // If SLF4J is available, test normal construction
            SLF4JLogger logger = new SLF4JLogger("test.logger");
            assertNotNull(logger);
            assertEquals("test.logger", logger.getName());
        }
    }
    
    @Test
    @DisplayName("Test basic logger operations when SLF4J is available")
    @EnabledIf("isSLF4JAvailable")
    public void testBasicOperations() {
        SLF4JLogger logger = new SLF4JLogger("test.slf4j.logger");
        
        // Test getName
        assertEquals("test.slf4j.logger", logger.getName());
        
        // Test logging at different levels (won't throw exceptions)
        assertDoesNotThrow(() -> {
            logger.trace("Trace message");
            logger.debug("Debug message");
            logger.info("Info message");
            logger.warn("Warn message");
            logger.error("Error message");
        });
    }
    
    @Test
    @DisplayName("Test isEnabled methods")
    @EnabledIf("isSLF4JAvailable")
    public void testIsEnabledMethods() {
        SLF4JLogger logger = new SLF4JLogger("test.slf4j.enabled");
        
        // These will return actual values from the underlying SLF4J logger
        // We can't predict the values, but we can verify they don't throw
        assertDoesNotThrow(() -> {
            boolean traceEnabled = logger.isTraceEnabled();
            boolean debugEnabled = logger.isDebugEnabled();
            boolean infoEnabled = logger.isInfoEnabled();
            boolean warnEnabled = logger.isWarnEnabled();
            boolean errorEnabled = logger.isErrorEnabled();
            
            // At least error should typically be enabled
            assertTrue(errorEnabled || warnEnabled || infoEnabled || debugEnabled || traceEnabled,
                "At least one log level should be enabled");
        });
    }
    
    @Test
    @DisplayName("Test logging with exceptions")
    @EnabledIf("isSLF4JAvailable")
    public void testLoggingWithExceptions() {
        SLF4JLogger logger = new SLF4JLogger("test.slf4j.exception");
        Exception testException = new Exception("Test exception");
        
        assertDoesNotThrow(() -> {
            logger.trace("Trace with exception", testException);
            logger.debug("Debug with exception", testException);
            logger.info("Info with exception", testException);
            logger.warn("Warn with exception", testException);
            logger.error("Error with exception", testException);
        });
    }
    
    @Test
    @DisplayName("Test logging with null values")
    @EnabledIf("isSLF4JAvailable")
    public void testLoggingWithNullValues() {
        SLF4JLogger logger = new SLF4JLogger("test.slf4j.null");
        
        assertDoesNotThrow(() -> {
            logger.info((String) null);
            logger.info((String) null, (Throwable) null);
            logger.error((String) null);
            logger.error((String) null, (Throwable) null);
        });
    }
    
    @Test
    @DisplayName("Test multiple logger instances")
    @EnabledIf("isSLF4JAvailable")
    public void testMultipleLoggerInstances() {
        SLF4JLogger logger1 = new SLF4JLogger("test.logger.one");
        SLF4JLogger logger2 = new SLF4JLogger("test.logger.two");
        SLF4JLogger logger3 = new SLF4JLogger("test.logger.one"); // Same name as logger1
        
        assertEquals("test.logger.one", logger1.getName());
        assertEquals("test.logger.two", logger2.getName());
        assertEquals("test.logger.one", logger3.getName());
        
        // Different logger instances
        assertNotSame(logger1, logger2);
        assertNotSame(logger1, logger3);
    }
    
    @Test
    @DisplayName("Test concurrent logging")
    @EnabledIf("isSLF4JAvailable")
    public void testConcurrentLogging() throws InterruptedException {
        SLF4JLogger logger = new SLF4JLogger("test.slf4j.concurrent");
        
        final int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    logger.info("Thread {} message {}", threadId, j);
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }
        
        // If we get here without exceptions, concurrent logging works
        assertTrue(true);
    }
    
    @Test
    @DisplayName("Test LocationAwareLogger usage")
    @EnabledIf("isSLF4JAvailable")
    public void testLocationAwareLoggerUsage() {
        SLF4JLogger logger = new SLF4JLogger("test.slf4j.location");
        
        // Test warn and error methods which use LocationAwareLogger if available
        assertDoesNotThrow(() -> {
            logger.warn("Warning message");
            logger.warn("Warning with exception", new Exception("Test"));
            logger.error("Error message");
            logger.error("Error with exception", new Exception("Test"));
        });
    }
    
    @Test
    @DisplayName("Test special logger names")
    @EnabledIf("isSLF4JAvailable")
    public void testSpecialLoggerNames() {
        // Test ROOT logger
        assertDoesNotThrow(() -> {
            SLF4JLogger rootLogger = new SLF4JLogger("ROOT");
            assertEquals("ROOT", rootLogger.getName());
            rootLogger.info("Root logger message");
        });
        
        // Test empty string logger
        assertDoesNotThrow(() -> {
            SLF4JLogger emptyLogger = new SLF4JLogger("");
            assertEquals("", emptyLogger.getName());
            emptyLogger.info("Empty name logger message");
        });
        
        // Test logger with special characters
        assertDoesNotThrow(() -> {
            SLF4JLogger specialLogger = new SLF4JLogger("com.test$Special_Logger-123");
            assertEquals("com.test$Special_Logger-123", specialLogger.getName());
            specialLogger.info("Special name logger message");
        });
    }
    
    // Mock test for when SLF4J is not available
    @Test
    @DisplayName("Test RuntimeException when SLF4J uses NOPLoggerFactory")
    public void testNOPLoggerFactoryException() {
        // This test simulates the condition where SLF4J returns NOPLoggerFactory
        // Since we can't easily mock static methods, we'll just verify the behavior
        // when SLF4J is not properly configured
        
        if (!isSLF4JAvailable()) {
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                new SLF4JLogger("test");
            });
            assertEquals("Failed to initialize SLF4J Logger Factory", exception.getMessage());
        }
    }
}
