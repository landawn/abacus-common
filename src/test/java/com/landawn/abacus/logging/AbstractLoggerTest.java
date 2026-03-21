package com.landawn.abacus.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AbstractLoggerTest extends TestBase {

    private TestLogger logger;

    private static class TestLogger extends AbstractLogger {
        public final List<LogEntry> logs = new ArrayList<>();
        public boolean traceEnabled = true;
        public boolean debugEnabled = true;
        public boolean infoEnabled = true;
        public boolean warnEnabled = true;
        public boolean errorEnabled = true;

        public TestLogger(String name) {
            super(name);
        }

        @Override
        public boolean isTraceEnabled() {
            return traceEnabled;
        }

        @Override
        public void trace(String msg) {
            if (traceEnabled) {
                logs.add(new LogEntry("TRACE", msg, null));
            }
        }

        @Override
        public void trace(String msg, Throwable t) {
            if (traceEnabled) {
                logs.add(new LogEntry("TRACE", msg, t));
            }
        }

        @Override
        public boolean isDebugEnabled() {
            return debugEnabled;
        }

        @Override
        public void debug(String msg) {
            if (debugEnabled) {
                logs.add(new LogEntry("DEBUG", msg, null));
            }
        }

        @Override
        public void debug(String msg, Throwable t) {
            if (debugEnabled) {
                logs.add(new LogEntry("DEBUG", msg, t));
            }
        }

        @Override
        public boolean isInfoEnabled() {
            return infoEnabled;
        }

        @Override
        public void info(String msg) {
            if (infoEnabled) {
                logs.add(new LogEntry("INFO", msg, null));
            }
        }

        @Override
        public void info(String msg, Throwable t) {
            if (infoEnabled) {
                logs.add(new LogEntry("INFO", msg, t));
            }
        }

        @Override
        public boolean isWarnEnabled() {
            return warnEnabled;
        }

        @Override
        public void warn(String msg) {
            if (warnEnabled) {
                logs.add(new LogEntry("WARN", msg, null));
            }
        }

        @Override
        public void warn(String msg, Throwable t) {
            if (warnEnabled) {
                logs.add(new LogEntry("WARN", msg, t));
            }
        }

        @Override
        public boolean isErrorEnabled() {
            return errorEnabled;
        }

        @Override
        public void error(String msg) {
            if (errorEnabled) {
                logs.add(new LogEntry("ERROR", msg, null));
            }
        }

        @Override
        public void error(String msg, Throwable t) {
            if (errorEnabled) {
                logs.add(new LogEntry("ERROR", msg, t));
            }
        }
    }

    private static class LogEntry {
        public final String level;
        public final String message;
        public final Throwable throwable;

        LogEntry(String level, String message, Throwable throwable) {
            this.level = level;
            this.message = message;
            this.throwable = throwable;
        }
    }

    @BeforeEach
    public void setUp() {
        logger = new TestLogger("test.logger");
    }

    @Test
    @DisplayName("Test getName method")
    public void testGetName() {
        assertEquals("test.logger", logger.getName());

        TestLogger logger2 = new TestLogger("another.logger");
        assertEquals("another.logger", logger2.getName());
    }

    @Test
    @DisplayName("Test trace with template and one argument")
    public void testTraceWithOneArg() {
        logger.trace("Hello {}", "World");
        assertEquals(1, logger.logs.size());
        assertEquals("TRACE", logger.logs.get(0).level);
        assertEquals("Hello World", logger.logs.get(0).message);

        logger.logs.clear();
        logger.trace("Value: %s", 42);
        assertEquals("Value: 42", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test trace with template and two arguments")
    public void testTraceWithTwoArgs() {
        logger.trace("User {} logged in at {}", "john", "10:30");
        assertEquals("User john logged in at 10:30", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test trace with template and three arguments")
    public void testTraceWithThreeArgs() {
        logger.trace("Processing {} records for user {} at {}", 100, "alice", "2023-01-01");
        assertEquals("Processing 100 records for user alice at 2023-01-01", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test trace with template and four arguments")
    public void testTraceWithFourArgs() {
        logger.trace("{} {} {} {}", "a", "b", "c", "d");
        assertEquals("a b c d", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test trace with template and five arguments")
    public void testTraceWithFiveArgs() {
        logger.trace("{} {} {} {} {}", 1, 2, 3, 4, 5);
        assertEquals("1 2 3 4 5", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test trace with template and six arguments")
    public void testTraceWithSixArgs() {
        logger.trace("{} {} {} {} {} {}", "a", "b", "c", "d", "e", "f");
        assertEquals("a b c d e f", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test trace with template and seven arguments")
    public void testTraceWithSevenArgs() {
        logger.trace("{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        assertEquals("1 2 3 4 5 6 7", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test trace with varargs")
    public void testTraceWithVarargs() {
        logger.trace("Values: {} {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7, 8);
        assertEquals("Values: 1 2 3 4 5 6 7 8", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test trace with Supplier")
    public void testTraceWithSupplier() {
        Supplier<String> supplier = () -> "Expensive message";
        logger.trace(supplier);
        assertEquals("Expensive message", logger.logs.get(0).message);

        logger.logs.clear();
        logger.traceEnabled = false;
        boolean[] called = { false };
        logger.trace(() -> {
            called[0] = true;
            return "Should not be called";
        });
        assertFalse(called[0]);
        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test trace disabled skips templated logging")
    public void testTraceDisabledSkipsTemplatedLogging() {
        logger.traceEnabled = false;
        logger.trace("Template {}", "arg1");
        logger.trace("Template {} {}", "a", "b");
        logger.trace("Template {} {} {}", "a", "b", "c");
        logger.trace("Template {} {} {} {}", "a", "b", "c", "d");
        logger.trace("Template {} {} {} {} {}", 1, 2, 3, 4, 5);
        logger.trace("Template {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        logger.trace("Template {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        logger.trace("Template", "a", "b", "c", "d", "e", "f", "g", "h");
        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test trace with Throwable and message")
    public void testTraceWithThrowableAndMessage() {
        Exception ex = new Exception("Test exception");
        logger.trace(ex, "Error occurred");
        assertEquals("Error occurred", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test trace with Throwable and template")
    public void testTraceWithThrowableAndTemplate() {
        Exception ex = new Exception("Test");
        logger.trace(ex, "Error in module {}", "core");
        assertEquals("Error in module core", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test trace with Supplier and Throwable (deprecated)")
    @SuppressWarnings("deprecation")
    public void testTraceWithSupplierAndThrowable() {
        Exception ex = new Exception("Test");
        logger.trace(() -> "Message from supplier", ex);
        assertEquals("Message from supplier", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test trace with Throwable and Supplier")
    public void testTraceWithThrowableAndSupplier() {
        Exception ex = new Exception("Test");
        logger.trace(ex, () -> "Message from supplier");
        assertEquals("Message from supplier", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test throwable+template disabled skips logging")
    public void testThrowableTemplateDisabledSkipsLogging() {
        Exception ex = new Exception("Test");

        logger.traceEnabled = false;
        logger.trace(ex, "msg");
        logger.trace(ex, "T {}", "a");
        logger.trace(ex, "T {} {}", "a", "b");
        logger.trace(ex, "T {} {} {}", "a", "b", "c");

        logger.debugEnabled = false;
        logger.debug(ex, "msg");
        logger.debug(ex, "T {}", "a");
        logger.debug(ex, "T {} {}", "a", "b");
        logger.debug(ex, "T {} {} {}", "a", "b", "c");

        logger.infoEnabled = false;
        logger.info(ex, "msg");
        logger.info(ex, "T {}", "a");
        logger.info(ex, "T {} {}", "a", "b");
        logger.info(ex, "T {} {} {}", "a", "b", "c");

        logger.warnEnabled = false;
        logger.warn(ex, "msg");
        logger.warn(ex, "T {}", "a");
        logger.warn(ex, "T {} {}", "a", "b");
        logger.warn(ex, "T {} {} {}", "a", "b", "c");

        logger.errorEnabled = false;
        logger.error(ex, "msg");
        logger.error(ex, "T {}", "a");
        logger.error(ex, "T {} {}", "a", "b");
        logger.error(ex, "T {} {} {}", "a", "b", "c");

        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test logging when levels are disabled")
    public void testLoggingWhenDisabled() {
        logger.traceEnabled = false;
        logger.debugEnabled = false;
        logger.infoEnabled = false;
        logger.warnEnabled = false;
        logger.errorEnabled = false;

        logger.trace("trace");
        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        assertEquals(0, logger.logs.size());

        boolean[] called = { false };
        Supplier<String> supplier = () -> {
            called[0] = true;
            return "message";
        };

        logger.trace(supplier);
        logger.debug(supplier);
        logger.info(supplier);
        logger.warn(supplier);
        logger.error(supplier);

        assertFalse(called[0]);
    }

    @Test
    @DisplayName("Test debug with template and arguments")
    public void testDebugWithArguments() {
        logger.debug("Debug: {}", "test");
        assertEquals("DEBUG", logger.logs.get(0).level);
        assertEquals("Debug: test", logger.logs.get(0).message);

        logger.logs.clear();
        logger.debug("{} + {} = {}", 1, 2, 3);
        assertEquals("1 + 2 = 3", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test debug level disabled")
    public void testDebugDisabled() {
        logger.debugEnabled = false;
        logger.debug("Should not log {}", "this");
        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test debug with template and all argument counts")
    public void testDebugWithAllArgCounts() {
        logger.debug("One: {}", "a");
        assertEquals("DEBUG", logger.logs.get(0).level);
        assertEquals("One: a", logger.logs.get(0).message);

        logger.logs.clear();
        logger.debug("{} {}", "a", "b");
        assertEquals("a b", logger.logs.get(0).message);

        logger.logs.clear();
        logger.debug("{} {} {}", "a", "b", "c");
        assertEquals("a b c", logger.logs.get(0).message);

        logger.logs.clear();
        logger.debug("{} {} {} {}", "a", "b", "c", "d");
        assertEquals("a b c d", logger.logs.get(0).message);

        logger.logs.clear();
        logger.debug("{} {} {} {} {}", 1, 2, 3, 4, 5);
        assertEquals("1 2 3 4 5", logger.logs.get(0).message);

        logger.logs.clear();
        logger.debug("{} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        assertEquals("1 2 3 4 5 6", logger.logs.get(0).message);

        logger.logs.clear();
        logger.debug("{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        assertEquals("1 2 3 4 5 6 7", logger.logs.get(0).message);

        logger.logs.clear();
        logger.debug("{} {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7, 8);
        assertEquals("1 2 3 4 5 6 7 8", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test debug with Supplier")
    public void testDebugWithSupplier() {
        Supplier<String> supplier = () -> "Debug supplier message";
        logger.debug(supplier);
        assertEquals("Debug supplier message", logger.logs.get(0).message);

        logger.logs.clear();
        logger.debugEnabled = false;
        boolean[] called = { false };
        logger.debug(() -> {
            called[0] = true;
            return "Should not be called";
        });
        assertFalse(called[0]);
        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test debug disabled skips templated logging")
    public void testDebugDisabledSkipsTemplatedLogging() {
        logger.debugEnabled = false;
        logger.debug("Template {}", "arg1");
        logger.debug("Template {} {}", "a", "b");
        logger.debug("Template {} {} {}", "a", "b", "c");
        logger.debug("Template {} {} {} {}", "a", "b", "c", "d");
        logger.debug("Template {} {} {} {} {}", 1, 2, 3, 4, 5);
        logger.debug("Template {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        logger.debug("Template {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        logger.debug("Template", "a", "b", "c", "d", "e", "f", "g", "h");
        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test debug with Throwable and message")
    public void testDebugWithThrowableAndMessage() {
        Exception ex = new Exception("Test");
        logger.debug(ex, "Error in debug");
        assertEquals("Error in debug", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test debug with Throwable and template")
    public void testDebugWithThrowableAndTemplate() {
        Exception ex = new Exception("Test");
        logger.debug(ex, "Debug error in {}", "core");
        assertEquals("Debug error in core", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);

        logger.logs.clear();
        logger.debug(ex, "Debug {} {}", "a", "b");
        assertEquals("Debug a b", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);

        logger.logs.clear();
        logger.debug(ex, "Debug {} {} {}", "a", "b", "c");
        assertEquals("Debug a b c", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test debug with Supplier and Throwable (deprecated)")
    @SuppressWarnings("deprecation")
    public void testDebugWithSupplierAndThrowable() {
        Exception ex = new Exception("Test");
        logger.debug(() -> "Debug supplier msg", ex);
        assertEquals("Debug supplier msg", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test debug with Throwable and Supplier")
    public void testDebugWithThrowableAndSupplier() {
        Exception ex = new Exception("Test");
        logger.debug(ex, () -> "Debug t+s msg");
        assertEquals("Debug t+s msg", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test info with template and all argument counts")
    public void testInfoWithAllArgCounts() {
        logger.info("One: {}", "a");
        assertEquals("INFO", logger.logs.get(0).level);
        assertEquals("One: a", logger.logs.get(0).message);

        logger.logs.clear();
        logger.info("{} {}", "a", "b");
        assertEquals("a b", logger.logs.get(0).message);

        logger.logs.clear();
        logger.info("{} {} {}", "a", "b", "c");
        assertEquals("a b c", logger.logs.get(0).message);

        logger.logs.clear();
        logger.info("{} {} {} {}", "a", "b", "c", "d");
        assertEquals("a b c d", logger.logs.get(0).message);

        logger.logs.clear();
        logger.info("{} {} {} {} {}", 1, 2, 3, 4, 5);
        assertEquals("1 2 3 4 5", logger.logs.get(0).message);

        logger.logs.clear();
        logger.info("{} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        assertEquals("1 2 3 4 5 6", logger.logs.get(0).message);

        logger.logs.clear();
        logger.info("{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        assertEquals("1 2 3 4 5 6 7", logger.logs.get(0).message);

        logger.logs.clear();
        logger.info("{} {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7, 8);
        assertEquals("1 2 3 4 5 6 7 8", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test info with Supplier")
    public void testInfoWithSupplier() {
        Supplier<String> supplier = () -> "Info supplier message";
        logger.info(supplier);
        assertEquals("Info supplier message", logger.logs.get(0).message);

        logger.logs.clear();
        logger.infoEnabled = false;
        boolean[] called = { false };
        logger.info(() -> {
            called[0] = true;
            return "Should not be called";
        });
        assertFalse(called[0]);
        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test info disabled skips templated logging")
    public void testInfoDisabledSkipsTemplatedLogging() {
        logger.infoEnabled = false;
        logger.info("Template {}", "arg1");
        logger.info("Template {} {}", "a", "b");
        logger.info("Template {} {} {}", "a", "b", "c");
        logger.info("Template {} {} {} {}", "a", "b", "c", "d");
        logger.info("Template {} {} {} {} {}", 1, 2, 3, 4, 5);
        logger.info("Template {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        logger.info("Template {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        logger.info("Template", "a", "b", "c", "d", "e", "f", "g", "h");
        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test info with various argument counts")
    public void testInfoWithArguments() {
        logger.info("Info message");
        assertEquals("INFO", logger.logs.get(0).level);

        logger.logs.clear();
        logger.info("User: {}", "admin");
        assertEquals("User: admin", logger.logs.get(0).message);

        logger.logs.clear();
        logger.info("{} {} {} {} {}", "a", "b", "c", "d", "e");
        assertEquals("a b c d e", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test info with Throwable and message")
    public void testInfoWithThrowableAndMessage() {
        Exception ex = new Exception("Test");
        logger.info(ex, "Info error");
        assertEquals("Info error", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test info with Throwable and template")
    public void testInfoWithThrowableAndTemplate() {
        Exception ex = new Exception("Test");
        logger.info(ex, "Info error in {}", "core");
        assertEquals("Info error in core", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);

        logger.logs.clear();
        logger.info(ex, "Info {} {}", "a", "b");
        assertEquals("Info a b", logger.logs.get(0).message);

        logger.logs.clear();
        logger.info(ex, "Info {} {} {}", "a", "b", "c");
        assertEquals("Info a b c", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test info with Supplier and Throwable (deprecated)")
    @SuppressWarnings("deprecation")
    public void testInfoWithSupplierAndThrowable() {
        Exception ex = new Exception("Test");
        logger.info(() -> "Info supplier msg", ex);
        assertEquals("Info supplier msg", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test info with Throwable and Supplier")
    public void testInfoWithThrowableAndSupplier() {
        Exception ex = new Exception("Test");
        logger.info(ex, () -> "Info t+s msg");
        assertEquals("Info t+s msg", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test warn with template and all argument counts")
    public void testWarnWithAllArgCounts() {
        logger.warn("One: {}", "a");
        assertEquals("WARN", logger.logs.get(0).level);
        assertEquals("One: a", logger.logs.get(0).message);

        logger.logs.clear();
        logger.warn("{} {}", "a", "b");
        assertEquals("a b", logger.logs.get(0).message);

        logger.logs.clear();
        logger.warn("{} {} {}", "a", "b", "c");
        assertEquals("a b c", logger.logs.get(0).message);

        logger.logs.clear();
        logger.warn("{} {} {} {}", "a", "b", "c", "d");
        assertEquals("a b c d", logger.logs.get(0).message);

        logger.logs.clear();
        logger.warn("{} {} {} {} {}", 1, 2, 3, 4, 5);
        assertEquals("1 2 3 4 5", logger.logs.get(0).message);

        logger.logs.clear();
        logger.warn("{} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        assertEquals("1 2 3 4 5 6", logger.logs.get(0).message);

        logger.logs.clear();
        logger.warn("{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        assertEquals("1 2 3 4 5 6 7", logger.logs.get(0).message);

        logger.logs.clear();
        logger.warn("{} {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7, 8);
        assertEquals("1 2 3 4 5 6 7 8", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test warn with Supplier")
    public void testWarnWithSupplier() {
        Supplier<String> supplier = () -> "Warn supplier message";
        logger.warn(supplier);
        assertEquals("Warn supplier message", logger.logs.get(0).message);

        logger.logs.clear();
        logger.warnEnabled = false;
        boolean[] called = { false };
        logger.warn(() -> {
            called[0] = true;
            return "Should not be called";
        });
        assertFalse(called[0]);
        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test warn disabled skips templated logging")
    public void testWarnDisabledSkipsTemplatedLogging() {
        logger.warnEnabled = false;
        logger.warn("Template {}", "arg1");
        logger.warn("Template {} {}", "a", "b");
        logger.warn("Template {} {} {}", "a", "b", "c");
        logger.warn("Template {} {} {} {}", "a", "b", "c", "d");
        logger.warn("Template {} {} {} {} {}", 1, 2, 3, 4, 5);
        logger.warn("Template {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        logger.warn("Template {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        logger.warn("Template", "a", "b", "c", "d", "e", "f", "g", "h");
        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test warn with exception")
    public void testWarnWithException() {
        Exception ex = new Exception("Warning");
        logger.warn("Warning occurred", ex);
        assertEquals("WARN", logger.logs.get(0).level);
        assertEquals("Warning occurred", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test warn with Throwable and message")
    public void testWarnWithThrowableAndMessage() {
        Exception ex = new Exception("Test");
        logger.warn(ex, "Warn error");
        assertEquals("Warn error", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test warn with Throwable and template")
    public void testWarnWithThrowableAndTemplate() {
        Exception ex = new Exception("Test");
        logger.warn(ex, "Warn error in {}", "core");
        assertEquals("Warn error in core", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);

        logger.logs.clear();
        logger.warn(ex, "Warn {} {}", "a", "b");
        assertEquals("Warn a b", logger.logs.get(0).message);

        logger.logs.clear();
        logger.warn(ex, "Warn {} {} {}", "a", "b", "c");
        assertEquals("Warn a b c", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test warn with Supplier and Throwable (deprecated)")
    @SuppressWarnings("deprecation")
    public void testWarnWithSupplierAndThrowable() {
        Exception ex = new Exception("Test");
        logger.warn(() -> "Warn supplier msg", ex);
        assertEquals("Warn supplier msg", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test warn with Throwable and Supplier")
    public void testWarnWithThrowableAndSupplier() {
        Exception ex = new Exception("Test");
        logger.warn(ex, () -> "Warn t+s msg");
        assertEquals("Warn t+s msg", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test error with all argument variations")
    public void testErrorWithAllVariations() {
        logger.error("Error!");
        assertEquals("ERROR", logger.logs.get(0).level);

        logger.logs.clear();
        logger.error("Error code: {}", 500);
        assertEquals("Error code: 500", logger.logs.get(0).message);

        logger.logs.clear();
        logger.error(() -> "Error from supplier");
        assertEquals("Error from supplier", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test error with template and all argument counts")
    public void testErrorWithAllArgCounts() {
        logger.error("One: {}", "a");
        assertEquals("ERROR", logger.logs.get(0).level);
        assertEquals("One: a", logger.logs.get(0).message);

        logger.logs.clear();
        logger.error("{} {}", "a", "b");
        assertEquals("a b", logger.logs.get(0).message);

        logger.logs.clear();
        logger.error("{} {} {}", "a", "b", "c");
        assertEquals("a b c", logger.logs.get(0).message);

        logger.logs.clear();
        logger.error("{} {} {} {}", "a", "b", "c", "d");
        assertEquals("a b c d", logger.logs.get(0).message);

        logger.logs.clear();
        logger.error("{} {} {} {} {}", 1, 2, 3, 4, 5);
        assertEquals("1 2 3 4 5", logger.logs.get(0).message);

        logger.logs.clear();
        logger.error("{} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        assertEquals("1 2 3 4 5 6", logger.logs.get(0).message);

        logger.logs.clear();
        logger.error("{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        assertEquals("1 2 3 4 5 6 7", logger.logs.get(0).message);

        logger.logs.clear();
        logger.error("{} {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7, 8);
        assertEquals("1 2 3 4 5 6 7 8", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test error with Throwable and message")
    public void testErrorWithThrowableAndMessage() {
        Exception ex = new Exception("Test");
        logger.error(ex, "Error occurred");
        assertEquals("Error occurred", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test error with Throwable and template")
    public void testErrorWithThrowableAndTemplate() {
        Exception ex = new Exception("Test");
        logger.error(ex, "Error in {}", "core");
        assertEquals("Error in core", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);

        logger.logs.clear();
        logger.error(ex, "Error {} {}", "a", "b");
        assertEquals("Error a b", logger.logs.get(0).message);

        logger.logs.clear();
        logger.error(ex, "Error {} {} {}", "a", "b", "c");
        assertEquals("Error a b c", logger.logs.get(0).message);
    }

    @Test
    @DisplayName("Test error with Supplier")
    public void testErrorWithSupplier() {
        Supplier<String> supplier = () -> "Error supplier message";
        logger.error(supplier);
        assertEquals("Error supplier message", logger.logs.get(0).message);

        logger.logs.clear();
        logger.errorEnabled = false;
        boolean[] called = { false };
        logger.error(() -> {
            called[0] = true;
            return "Should not be called";
        });
        assertFalse(called[0]);
        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test error with Supplier and Throwable (deprecated)")
    @SuppressWarnings("deprecation")
    public void testErrorWithSupplierAndThrowable() {
        Exception ex = new Exception("Test");
        logger.error(() -> "Error supplier msg", ex);
        assertEquals("Error supplier msg", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test error with Throwable and Supplier")
    public void testErrorWithThrowableAndSupplier() {
        Exception ex = new Exception("Test");
        logger.error(ex, () -> "Error t+s msg");
        assertEquals("Error t+s msg", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    @DisplayName("Test error disabled skips templated logging")
    public void testErrorDisabledSkipsTemplatedLogging() {
        logger.errorEnabled = false;
        logger.error("Template {}", "arg1");
        logger.error("Template {} {}", "a", "b");
        logger.error("Template {} {} {}", "a", "b", "c");
        logger.error("Template {} {} {} {}", "a", "b", "c", "d");
        logger.error("Template {} {} {} {} {}", 1, 2, 3, 4, 5);
        logger.error("Template {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        logger.error("Template {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        logger.error("Template", "a", "b", "c", "d", "e", "f", "g", "h");
        assertEquals(0, logger.logs.size());
    }

    @Test
    @DisplayName("Test format with different argument counts")
    public void testFormatWithDifferentArgCounts() {
        assertEquals("Value: 42", AbstractLogger.format("Value: {}", 42));

        assertEquals("x=10, y=20", AbstractLogger.format("x={}, y={}", 10, 20));

        assertEquals("RGB: 255,128,0", AbstractLogger.format("RGB: {},{},{}", 255, 128, 0));

        assertEquals("1-2-3-4", AbstractLogger.format("{}-{}-{}-{}", 1, 2, 3, 4));

        assertEquals("a b c d e", AbstractLogger.format("{} {} {} {} {}", "a", "b", "c", "d", "e"));

        assertEquals("1,2,3,4,5,6", AbstractLogger.format("{},{},{},{},{},{}", 1, 2, 3, 4, 5, 6));

        assertEquals("Mon Tue Wed Thu Fri Sat Sun", AbstractLogger.format("{} {} {} {} {} {} {}", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
    }

    @Test
    @DisplayName("Test format with no placeholders and no args")
    public void testFormatWithNoPlaceholdersNoArgs() {
        assertEquals("Hello World", AbstractLogger.format("Hello World"));
    }

    @Test
    @DisplayName("Test format with placeholder in various positions")
    public void testFormatWithPlaceholderPositions() {
        assertEquals("start x", AbstractLogger.format("{} x", "start"));
        assertEquals("x end", AbstractLogger.format("x {}", "end"));
        assertEquals("x middle y", AbstractLogger.format("x {} y", "middle"));
    }

    @Test
    @DisplayName("Test format method with various scenarios")
    public void testFormatMethod() {
        assertEquals("Hello World", AbstractLogger.format("Hello {}", "World"));
        assertEquals("a b c", AbstractLogger.format("{} {} {}", "a", "b", "c"));

        assertEquals("Hello World", AbstractLogger.format("Hello %s", "World"));
        assertEquals("a b c", AbstractLogger.format("%s %s %s", "a", "b", "c"));

        assertEquals("Hello [World]", AbstractLogger.format("Hello", "World"));

        assertEquals("Value: null", AbstractLogger.format("Value: {}", (Object) null));
        assertEquals("null [arg]", AbstractLogger.format(null, "arg"));

        assertEquals("a b [c, d]", AbstractLogger.format("{} {}", "a", "b", "c", "d"));

        Object[] args = { 1, 2, 3, 4, 5 };
        assertEquals("1 2 3 4 5", AbstractLogger.format("{} {} {} {} {}", args));
    }

    @Test
    @DisplayName("Test format with empty string template")
    public void testFormatWithEmptyTemplate() {
        assertEquals("", AbstractLogger.format(""));
        assertEquals(" [x]", AbstractLogger.format("", "x"));
    }
}
