package com.landawn.abacus.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AbstractLoggerTest extends TestBase {

    private static final String[] LEVELS = { "TRACE", "DEBUG", "INFO", "WARN", "ERROR" };

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

    private void setEnabled(String level, boolean enabled) {
        switch (level) {
            case "TRACE" -> logger.traceEnabled = enabled;
            case "DEBUG" -> logger.debugEnabled = enabled;
            case "INFO" -> logger.infoEnabled = enabled;
            case "WARN" -> logger.warnEnabled = enabled;
            case "ERROR" -> logger.errorEnabled = enabled;
            default -> throw new AssertionError(level);
        }
    }

    private void logTemplate(String level, String template, Object... args) {
        int n = args.length;
        if ("TRACE".equals(level)) {
            if (n == 1) {
                logger.trace(template, args[0]);
            } else if (n == 2) {
                logger.trace(template, args[0], args[1]);
            } else if (n == 3) {
                logger.trace(template, args[0], args[1], args[2]);
            } else if (n == 4) {
                logger.trace(template, args[0], args[1], args[2], args[3]);
            } else if (n == 5) {
                logger.trace(template, args[0], args[1], args[2], args[3], args[4]);
            } else if (n == 6) {
                logger.trace(template, args[0], args[1], args[2], args[3], args[4], args[5]);
            } else if (n == 7) {
                logger.trace(template, args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            } else {
                logger.trace(template, args);
            }
        } else if ("DEBUG".equals(level)) {
            if (n == 1) {
                logger.debug(template, args[0]);
            } else if (n == 2) {
                logger.debug(template, args[0], args[1]);
            } else if (n == 3) {
                logger.debug(template, args[0], args[1], args[2]);
            } else if (n == 4) {
                logger.debug(template, args[0], args[1], args[2], args[3]);
            } else if (n == 5) {
                logger.debug(template, args[0], args[1], args[2], args[3], args[4]);
            } else if (n == 6) {
                logger.debug(template, args[0], args[1], args[2], args[3], args[4], args[5]);
            } else if (n == 7) {
                logger.debug(template, args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            } else {
                logger.debug(template, args);
            }
        } else if ("INFO".equals(level)) {
            if (n == 1) {
                logger.info(template, args[0]);
            } else if (n == 2) {
                logger.info(template, args[0], args[1]);
            } else if (n == 3) {
                logger.info(template, args[0], args[1], args[2]);
            } else if (n == 4) {
                logger.info(template, args[0], args[1], args[2], args[3]);
            } else if (n == 5) {
                logger.info(template, args[0], args[1], args[2], args[3], args[4]);
            } else if (n == 6) {
                logger.info(template, args[0], args[1], args[2], args[3], args[4], args[5]);
            } else if (n == 7) {
                logger.info(template, args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            } else {
                logger.info(template, args);
            }
        } else if ("WARN".equals(level)) {
            if (n == 1) {
                logger.warn(template, args[0]);
            } else if (n == 2) {
                logger.warn(template, args[0], args[1]);
            } else if (n == 3) {
                logger.warn(template, args[0], args[1], args[2]);
            } else if (n == 4) {
                logger.warn(template, args[0], args[1], args[2], args[3]);
            } else if (n == 5) {
                logger.warn(template, args[0], args[1], args[2], args[3], args[4]);
            } else if (n == 6) {
                logger.warn(template, args[0], args[1], args[2], args[3], args[4], args[5]);
            } else if (n == 7) {
                logger.warn(template, args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            } else {
                logger.warn(template, args);
            }
        } else if ("ERROR".equals(level)) {
            if (n == 1) {
                logger.error(template, args[0]);
            } else if (n == 2) {
                logger.error(template, args[0], args[1]);
            } else if (n == 3) {
                logger.error(template, args[0], args[1], args[2]);
            } else if (n == 4) {
                logger.error(template, args[0], args[1], args[2], args[3]);
            } else if (n == 5) {
                logger.error(template, args[0], args[1], args[2], args[3], args[4]);
            } else if (n == 6) {
                logger.error(template, args[0], args[1], args[2], args[3], args[4], args[5]);
            } else if (n == 7) {
                logger.error(template, args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            } else {
                logger.error(template, args);
            }
        } else {
            throw new AssertionError(level);
        }
    }

    private void logThrowable(String level, Throwable t, String template, Object... args) {
        int n = args.length;
        if ("TRACE".equals(level)) {
            if (n == 0) {
                logger.trace(t, template);
            } else if (n == 1) {
                logger.trace(t, template, args[0]);
            } else if (n == 2) {
                logger.trace(t, template, args[0], args[1]);
            } else {
                logger.trace(t, template, args[0], args[1], args[2]);
            }
        } else if ("DEBUG".equals(level)) {
            if (n == 0) {
                logger.debug(t, template);
            } else if (n == 1) {
                logger.debug(t, template, args[0]);
            } else if (n == 2) {
                logger.debug(t, template, args[0], args[1]);
            } else {
                logger.debug(t, template, args[0], args[1], args[2]);
            }
        } else if ("INFO".equals(level)) {
            if (n == 0) {
                logger.info(t, template);
            } else if (n == 1) {
                logger.info(t, template, args[0]);
            } else if (n == 2) {
                logger.info(t, template, args[0], args[1]);
            } else {
                logger.info(t, template, args[0], args[1], args[2]);
            }
        } else if ("WARN".equals(level)) {
            if (n == 0) {
                logger.warn(t, template);
            } else if (n == 1) {
                logger.warn(t, template, args[0]);
            } else if (n == 2) {
                logger.warn(t, template, args[0], args[1]);
            } else {
                logger.warn(t, template, args[0], args[1], args[2]);
            }
        } else if ("ERROR".equals(level)) {
            if (n == 0) {
                logger.error(t, template);
            } else if (n == 1) {
                logger.error(t, template, args[0]);
            } else if (n == 2) {
                logger.error(t, template, args[0], args[1]);
            } else {
                logger.error(t, template, args[0], args[1], args[2]);
            }
        } else {
            throw new AssertionError(level);
        }
    }

    private void logSupplier(String level, Supplier<String> supplier) {
        switch (level) {
            case "TRACE" -> logger.trace(supplier);
            case "DEBUG" -> logger.debug(supplier);
            case "INFO" -> logger.info(supplier);
            case "WARN" -> logger.warn(supplier);
            case "ERROR" -> logger.error(supplier);
            default -> throw new AssertionError(level);
        }
    }

    @SuppressWarnings("deprecation")
    private void logSupplierThrowable(String level, Supplier<String> supplier, Throwable t) {
        switch (level) {
            case "TRACE" -> logger.trace(supplier, t);
            case "DEBUG" -> logger.debug(supplier, t);
            case "INFO" -> logger.info(supplier, t);
            case "WARN" -> logger.warn(supplier, t);
            case "ERROR" -> logger.error(supplier, t);
            default -> throw new AssertionError(level);
        }
    }

    private void logThrowableSupplier(String level, Throwable t, Supplier<String> supplier) {
        switch (level) {
            case "TRACE" -> logger.trace(t, supplier);
            case "DEBUG" -> logger.debug(t, supplier);
            case "INFO" -> logger.info(t, supplier);
            case "WARN" -> logger.warn(t, supplier);
            case "ERROR" -> logger.error(t, supplier);
            default -> throw new AssertionError(level);
        }
    }

    @Test
    public void testGetName() {
        assertEquals("test.logger", logger.getName());
        assertEquals("another.logger", new TestLogger("another.logger").getName());
    }

    @Test
    public void testTemplateOverloadsAtEveryLevel() {
        for (String level : LEVELS) {
            logger.logs.clear();
            logTemplate(level, "Hello {}", "World");
            assertEquals(level, logger.logs.get(0).level);
            assertEquals("Hello World", logger.logs.get(0).message);

            logger.logs.clear();
            logTemplate(level, "User {} at {}", "john", "10:30");
            assertEquals("User john at 10:30", logger.logs.get(0).message);

            logger.logs.clear();
            logTemplate(level, "{} {} {}", "a", "b", "c");
            assertEquals("a b c", logger.logs.get(0).message);

            logger.logs.clear();
            logTemplate(level, "{} {} {} {}", "a", "b", "c", "d");
            assertEquals("a b c d", logger.logs.get(0).message);

            logger.logs.clear();
            logTemplate(level, "{} {} {} {} {}", 1, 2, 3, 4, 5);
            assertEquals("1 2 3 4 5", logger.logs.get(0).message);

            logger.logs.clear();
            logTemplate(level, "{} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
            assertEquals("1 2 3 4 5 6", logger.logs.get(0).message);

            logger.logs.clear();
            logTemplate(level, "{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
            assertEquals("1 2 3 4 5 6 7", logger.logs.get(0).message);

            logger.logs.clear();
            logTemplate(level, "{} {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7, 8);
            assertEquals("1 2 3 4 5 6 7 8", logger.logs.get(0).message);
        }
        logger.logs.clear();
        logger.trace("Value: %s", 42);
        assertEquals("Value: 42", logger.logs.get(0).message);
    }

    @Test
    public void testDisabledLevelSkipsTemplatesSuppliersAndThrowables() {
        Exception ex = new Exception("Test");
        for (String level : LEVELS) {
            setEnabled(level, false);
            boolean[] called = { false };
            logTemplate(level, "Template {}", "arg1");
            logTemplate(level, "T {} {}", "a", "b");
            logTemplate(level, "T {} {} {}", "a", "b", "c");
            logTemplate(level, "T {} {} {} {}", "a", "b", "c", "d");
            logTemplate(level, "T {} {} {} {} {}", 1, 2, 3, 4, 5);
            logTemplate(level, "T {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
            logTemplate(level, "T {} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
            logTemplate(level, "Template", "a", "b", "c", "d", "e", "f", "g", "h");
            logThrowable(level, ex, "msg");
            logThrowable(level, ex, "T {}", "a");
            logThrowable(level, ex, "T {} {}", "a", "b");
            logThrowable(level, ex, "T {} {} {}", "a", "b", "c");
            logSupplier(level, () -> {
                called[0] = true;
                return "nope";
            });
            assertFalse(called[0], level);
        }
        logger.trace("trace");
        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");
        assertEquals(0, logger.logs.size());
    }

    @Test
    public void testThrowableAndSupplierOverloadsAtEveryLevel() {
        Exception ex = new Exception("Test");
        for (String level : LEVELS) {
            logger.logs.clear();
            logThrowable(level, ex, "Error occurred");
            assertEquals("Error occurred", logger.logs.get(0).message);
            assertSame(ex, logger.logs.get(0).throwable);

            logger.logs.clear();
            logThrowable(level, ex, "Error in {}", "core");
            assertEquals("Error in core", logger.logs.get(0).message);

            logger.logs.clear();
            logThrowable(level, ex, "{} {}", "a", "b");
            assertEquals("a b", logger.logs.get(0).message);

            logger.logs.clear();
            logThrowable(level, ex, "{} {} {}", "a", "b", "c");
            assertEquals("a b c", logger.logs.get(0).message);

            logger.logs.clear();
            logSupplier(level, () -> "from supplier");
            assertEquals("from supplier", logger.logs.get(0).message);

            logger.logs.clear();
            logSupplierThrowable(level, () -> "supplier+ex", ex);
            assertEquals("supplier+ex", logger.logs.get(0).message);
            assertSame(ex, logger.logs.get(0).throwable);

            logger.logs.clear();
            logThrowableSupplier(level, ex, () -> "ex+supplier");
            assertEquals("ex+supplier", logger.logs.get(0).message);
            assertSame(ex, logger.logs.get(0).throwable);
        }
        logger.logs.clear();
        logger.warn("Warning occurred", ex);
        assertEquals("WARN", logger.logs.get(0).level);
        assertSame(ex, logger.logs.get(0).throwable);
    }

    @Test
    public void testFormatOverloads() {
        assertEquals("Hello World", AbstractLogger.format("Hello World"));
        assertEquals("Hello World", AbstractLogger.format("Hello {}", "World"));
        assertEquals("x=10, y=20", AbstractLogger.format("x={}, y={}", 10, 20));
        assertEquals("RGB: 255,128,0", AbstractLogger.format("RGB: {},{},{}", 255, 128, 0));
        assertEquals("1-2-3-4", AbstractLogger.format("{}-{}-{}-{}", 1, 2, 3, 4));
        assertEquals("a b c d e", AbstractLogger.format("{} {} {} {} {}", "a", "b", "c", "d", "e"));
        assertEquals("1,2,3,4,5,6", AbstractLogger.format("{},{},{},{},{},{}", 1, 2, 3, 4, 5, 6));
        assertEquals("Mon Tue Wed Thu Fri Sat Sun", AbstractLogger.format("{} {} {} {} {} {} {}", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
        assertEquals("Hello World", AbstractLogger.format("Hello %s", "World"));
        assertEquals("a b c", AbstractLogger.format("%s %s %s", "a", "b", "c"));
        assertEquals("Hello [World]", AbstractLogger.format("Hello", "World"));
        assertEquals("Value: null", AbstractLogger.format("Value: {}", (Object) null));
        assertEquals("null [arg]", AbstractLogger.format(null, "arg"));
        assertEquals("a b [c, d]", AbstractLogger.format("{} {}", "a", "b", "c", "d"));
        assertEquals("1 2 3 4 5", AbstractLogger.format("{} {} {} {} {}", 1, 2, 3, 4, 5));
        assertEquals("", AbstractLogger.format(""));
        assertEquals(" [x]", AbstractLogger.format("", "x"));
        assertEquals("start x", AbstractLogger.format("{} x", "start"));
        assertEquals("x end", AbstractLogger.format("x {}", "end"));
        assertEquals("x middle y", AbstractLogger.format("x {} y", "middle"));
    }

    private static final class ThrowingToString {
        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    private static final class CountingToString {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public String toString() {
            if (calls.incrementAndGet() > 1) {
                throw new IllegalStateException("evaluated more than once");
            }
            return "once";
        }
    }

    private static final class UnicodeToString {
        @Override
        public String toString() {
            return "\u03bb";
        }
    }

    private static String failedMarker(final Object arg, final Class<? extends Throwable> failure) {
        return "[FAILED toString() of " + arg.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(arg)) + ": " + failure.getName() + "]";
    }

    @Test
    public void testThrowingToStringIsRenderedAsMarkerInEveryFormatOverload() {
        final ThrowingToString bad = new ThrowingToString();
        final String marker = failedMarker(bad, IllegalStateException.class);

        assertEquals("v " + marker, AbstractLogger.format("v {}", bad));
        assertEquals("v " + marker, AbstractLogger.format("v %s", bad));
        assertEquals("x [" + marker + "]", AbstractLogger.format("x", bad));
        assertEquals("a " + marker + " b", AbstractLogger.format("a {} {}", bad, "b"));
        assertEquals("a b [" + marker + "]", AbstractLogger.format("a {}", "b", bad));
        assertEquals("x [" + marker + ", b]", AbstractLogger.format("x", bad, "b"));
        assertEquals("1 " + marker + " 3", AbstractLogger.format("{} {} {}", 1, bad, 3));
        assertEquals("1 [" + marker + ", 3]", AbstractLogger.format("{}", 1, bad, 3));
        assertEquals("x [1, 2, " + marker + "]", AbstractLogger.format("x", 1, 2, bad));
        assertEquals("1 2 3 " + marker, AbstractLogger.format("{} {} {} {}", 1, 2, 3, bad));
        assertEquals("1 [2, " + marker + ", 4]", AbstractLogger.format("{}", 1, 2, bad, 4));
    }

    @Test
    public void testThrowingToStringIsLoggedAtEnabledLevelWithThrowableStillAttached() {
        final ThrowingToString bad = new ThrowingToString();
        final String marker = failedMarker(bad, IllegalStateException.class);
        final RuntimeException cause = new RuntimeException("cause");

        assertDoesNotThrow(() -> logger.info("value {}", bad));
        assertDoesNotThrow(() -> logger.warn(cause, "value {}", bad));
        assertDoesNotThrow(() -> logger.error(cause, "{} {} {}", bad, bad, bad));
        assertDoesNotThrow(() -> logger.debug("{} {} {} {}", 1, 2, 3, bad));
        assertDoesNotThrow(() -> logger.trace(cause, "{} {}", "a", bad));

        assertEquals(5, logger.logs.size());
        assertEquals("value " + marker, logger.logs.get(0).message);
        assertEquals("value " + marker, logger.logs.get(1).message);
        assertSame(cause, logger.logs.get(1).throwable);
        assertEquals(marker + " " + marker + " " + marker, logger.logs.get(2).message);
        assertSame(cause, logger.logs.get(2).throwable);
        assertEquals("1 2 3 " + marker, logger.logs.get(3).message);
        assertEquals("a " + marker, logger.logs.get(4).message);
        assertSame(cause, logger.logs.get(4).throwable);
    }

    @Test
    public void testSelfReferentialCollectionAndArrayDoNotOverflowTheLogCall() {
        final List<Object> list = new ArrayList<>();
        list.add("x");
        list.add(list);
        final String rendered = assertDoesNotThrow(() -> AbstractLogger.format("{}", list));
        assertEquals(failedMarker(list, StackOverflowError.class), rendered);

        final Object[] array = new Object[2];
        array[0] = "x";
        array[1] = array;
        assertEquals(failedMarker(array, StackOverflowError.class), assertDoesNotThrow(() -> AbstractLogger.format("{}", (Object) array)));

        assertDoesNotThrow(() -> logger.info("{}", list));
        assertEquals(1, logger.logs.size());
        assertEquals(failedMarker(list, StackOverflowError.class), logger.logs.get(0).message);
        assertEquals("[[1, 2], x]", AbstractLogger.format("{}", List.of(List.of(1, 2), "x")));
    }

    @Test
    public void testNullAndUnicodeArgumentsStillRenderVerbatim() {
        assertEquals("v null", AbstractLogger.format("v {}", (Object) null));
        assertEquals("null null", AbstractLogger.format("{} {}", null, null));
        assertEquals("v \u03bb", AbstractLogger.format("v {}", new UnicodeToString()));
        assertEquals("\u03bb \u03bb \u03bb \u03bb", AbstractLogger.format("{} {} {} {}", "\u03bb", new UnicodeToString(), "\u03bb", new UnicodeToString()));
        assertEquals("", AbstractLogger.format("{}", ""));
    }

    @Test
    public void testEachArgumentIsEvaluatedExactlyOnce() {
        final CountingToString c1 = new CountingToString();
        assertEquals("once", AbstractLogger.format("{}", c1));
        assertEquals(1, c1.calls.get());

        final CountingToString c2 = new CountingToString();
        final CountingToString c3 = new CountingToString();
        assertEquals("once once", AbstractLogger.format("{} {}", c2, c3));
        assertEquals(1, c2.calls.get());
        assertEquals(1, c3.calls.get());

        final CountingToString c4 = new CountingToString();
        assertEquals("once [once, once]", AbstractLogger.format("{}", c4, new CountingToString(), new CountingToString()));
        assertEquals(1, c4.calls.get());

        final CountingToString c5 = new CountingToString();
        assertEquals("a b c once", AbstractLogger.format("{} {} {} {}", "a", "b", "c", c5));
        assertEquals(1, c5.calls.get());
    }

    @Test
    public void testThrowingSupplierStillPropagatesAndDisabledLevelDoesNotRenderArguments() {
        final IllegalStateException failure = new IllegalStateException("supplier");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> logger.info(() -> {
            throw failure;
        })));

        logger.infoEnabled = false;
        final CountingToString notEvaluated = new CountingToString();
        logger.info("{}", notEvaluated);
        assertEquals(0, notEvaluated.calls.get());
        assertEquals(0, logger.logs.size());
    }

    @Test
    public void testFormatLeavesUnmatchedPlaceholdersAndHasNoEscapeSequences() {
        assertEquals("a {}", AbstractLogger.format("{} {}", "a"));
        assertEquals("\\a {}", AbstractLogger.format("\\{} {}", "a"));
        assertEquals("%d items [5]", AbstractLogger.format("%d items", 5));
        assertEquals("100%a", AbstractLogger.format("100%%s", "a"));
        assertEquals("{} b", AbstractLogger.format("{} {}", "{}", "b"));
        assertEquals("Rate is 90Xure", AbstractLogger.format("Rate is 90%sure", "X"));
        assertEquals("%s a", AbstractLogger.format("{} {}", "%s", "a"));
    }

    @Test
    public void testSingleThrowableArgumentBindsToMessageThrowableOverload() {
        final RuntimeException ex = new RuntimeException("x");
        logger.error("Failed {}", ex);

        assertEquals(1, logger.logs.size());
        assertEquals("Failed {}", logger.logs.get(0).message);
        assertSame(ex, logger.logs.get(0).throwable);
    }
}
