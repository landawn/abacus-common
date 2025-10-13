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
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class AbstractLogger100Test extends TestBase {

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
    @DisplayName("Test warn with exception")
    public void testWarnWithException() {
        Exception ex = new Exception("Warning");
        logger.warn("Warning occurred", ex);
        assertEquals("WARN", logger.logs.get(0).level);
        assertEquals("Warning occurred", logger.logs.get(0).message);
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
}
