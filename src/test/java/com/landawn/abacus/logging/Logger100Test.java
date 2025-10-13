package com.landawn.abacus.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Logger100Test extends TestBase {

    private static class MockLogger implements Logger {
        private final String name;
        public int traceCount = 0;
        public int debugCount = 0;
        public int infoCount = 0;
        public int warnCount = 0;
        public int errorCount = 0;
        public String lastMessage = null;
        public Throwable lastThrowable = null;

        public MockLogger(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isTraceEnabled() {
            return true;
        }

        @Override
        public void trace(String msg) {
            traceCount++;
            lastMessage = msg;
        }

        @Override
        public void trace(String template, Object arg) {
            traceCount++;
            lastMessage = template + " " + arg;
        }

        @Override
        public void trace(String template, Object arg1, Object arg2) {
            traceCount++;
            lastMessage = template + " " + arg1 + " " + arg2;
        }

        @Override
        public void trace(String template, Object arg1, Object arg2, Object arg3) {
            traceCount++;
            lastMessage = template + " " + arg1 + " " + arg2 + " " + arg3;
        }

        @Override
        public void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4) {
            traceCount++;
        }

        @Override
        public void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
            traceCount++;
        }

        @Override
        public void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
            traceCount++;
        }

        @Override
        public void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
            traceCount++;
        }

        @Override
        public void trace(String template, Object... args) {
            traceCount++;
        }

        @Override
        public void trace(String msg, Throwable t) {
            traceCount++;
            lastMessage = msg;
            lastThrowable = t;
        }

        @Override
        public void trace(Throwable t, String msg) {
            traceCount++;
            lastMessage = msg;
            lastThrowable = t;
        }

        @Override
        public void trace(Throwable t, String template, Object arg) {
            traceCount++;
            lastThrowable = t;
        }

        @Override
        public void trace(Throwable t, String template, Object arg1, Object arg2) {
            traceCount++;
            lastThrowable = t;
        }

        @Override
        public void trace(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
            traceCount++;
            lastThrowable = t;
        }

        @Override
        public void trace(Supplier<String> supplier) {
            traceCount++;
            lastMessage = supplier.get();
        }

        @Override
        public void trace(Supplier<String> supplier, Throwable t) {
            traceCount++;
            lastMessage = supplier.get();
            lastThrowable = t;
        }

        @Override
        public void trace(Throwable t, Supplier<String> supplier) {
            traceCount++;
            lastMessage = supplier.get();
            lastThrowable = t;
        }

        @Override
        public boolean isDebugEnabled() {
            return true;
        }

        @Override
        public void debug(String msg) {
            debugCount++;
            lastMessage = msg;
        }

        @Override
        public void debug(String template, Object arg) {
            debugCount++;
        }

        @Override
        public void debug(String template, Object arg1, Object arg2) {
            debugCount++;
        }

        @Override
        public void debug(String template, Object arg1, Object arg2, Object arg3) {
            debugCount++;
        }

        @Override
        public void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4) {
            debugCount++;
        }

        @Override
        public void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
            debugCount++;
        }

        @Override
        public void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
            debugCount++;
        }

        @Override
        public void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
            debugCount++;
        }

        @Override
        public void debug(String template, Object... args) {
            debugCount++;
        }

        @Override
        public void debug(String msg, Throwable t) {
            debugCount++;
            lastMessage = msg;
            lastThrowable = t;
        }

        @Override
        public void debug(Throwable t, String msg) {
            debugCount++;
            lastMessage = msg;
            lastThrowable = t;
        }

        @Override
        public void debug(Throwable t, String template, Object arg) {
            debugCount++;
            lastThrowable = t;
        }

        @Override
        public void debug(Throwable t, String template, Object arg1, Object arg2) {
            debugCount++;
            lastThrowable = t;
        }

        @Override
        public void debug(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
            debugCount++;
            lastThrowable = t;
        }

        @Override
        public void debug(Supplier<String> supplier) {
            debugCount++;
            lastMessage = supplier.get();
        }

        @Override
        public void debug(Supplier<String> supplier, Throwable t) {
            debugCount++;
            lastMessage = supplier.get();
            lastThrowable = t;
        }

        @Override
        public void debug(Throwable t, Supplier<String> supplier) {
            debugCount++;
            lastMessage = supplier.get();
            lastThrowable = t;
        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public void info(String msg) {
            infoCount++;
            lastMessage = msg;
        }

        @Override
        public void info(String template, Object arg) {
            infoCount++;
        }

        @Override
        public void info(String template, Object arg1, Object arg2) {
            infoCount++;
        }

        @Override
        public void info(String template, Object arg1, Object arg2, Object arg3) {
            infoCount++;
        }

        @Override
        public void info(String template, Object arg1, Object arg2, Object arg3, Object arg4) {
            infoCount++;
        }

        @Override
        public void info(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
            infoCount++;
        }

        @Override
        public void info(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
            infoCount++;
        }

        @Override
        public void info(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
            infoCount++;
        }

        @Override
        public void info(String template, Object... args) {
            infoCount++;
        }

        @Override
        public void info(String msg, Throwable t) {
            infoCount++;
            lastMessage = msg;
            lastThrowable = t;
        }

        @Override
        public void info(Throwable t, String msg) {
            infoCount++;
            lastMessage = msg;
            lastThrowable = t;
        }

        @Override
        public void info(Throwable t, String template, Object arg) {
            infoCount++;
            lastThrowable = t;
        }

        @Override
        public void info(Throwable t, String template, Object arg1, Object arg2) {
            infoCount++;
            lastThrowable = t;
        }

        @Override
        public void info(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
            infoCount++;
            lastThrowable = t;
        }

        @Override
        public void info(Supplier<String> supplier) {
            infoCount++;
            lastMessage = supplier.get();
        }

        @Override
        public void info(Supplier<String> supplier, Throwable t) {
            infoCount++;
            lastMessage = supplier.get();
            lastThrowable = t;
        }

        @Override
        public void info(Throwable t, Supplier<String> supplier) {
            infoCount++;
            lastMessage = supplier.get();
            lastThrowable = t;
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public void warn(String msg) {
            warnCount++;
            lastMessage = msg;
        }

        @Override
        public void warn(String template, Object arg) {
            warnCount++;
        }

        @Override
        public void warn(String template, Object arg1, Object arg2) {
            warnCount++;
        }

        @Override
        public void warn(String template, Object arg1, Object arg2, Object arg3) {
            warnCount++;
        }

        @Override
        public void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4) {
            warnCount++;
        }

        @Override
        public void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
            warnCount++;
        }

        @Override
        public void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
            warnCount++;
        }

        @Override
        public void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
            warnCount++;
        }

        @Override
        public void warn(String template, Object... args) {
            warnCount++;
        }

        @Override
        public void warn(String msg, Throwable t) {
            warnCount++;
            lastMessage = msg;
            lastThrowable = t;
        }

        @Override
        public void warn(Throwable t, String msg) {
            warnCount++;
            lastMessage = msg;
            lastThrowable = t;
        }

        @Override
        public void warn(Throwable t, String template, Object arg) {
            warnCount++;
            lastThrowable = t;
        }

        @Override
        public void warn(Throwable t, String template, Object arg1, Object arg2) {
            warnCount++;
            lastThrowable = t;
        }

        @Override
        public void warn(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
            warnCount++;
            lastThrowable = t;
        }

        @Override
        public void warn(Supplier<String> supplier) {
            warnCount++;
            lastMessage = supplier.get();
        }

        @Override
        public void warn(Supplier<String> supplier, Throwable t) {
            warnCount++;
            lastMessage = supplier.get();
            lastThrowable = t;
        }

        @Override
        public void warn(Throwable t, Supplier<String> supplier) {
            warnCount++;
            lastMessage = supplier.get();
            lastThrowable = t;
        }

        @Override
        public boolean isErrorEnabled() {
            return true;
        }

        @Override
        public void error(String msg) {
            errorCount++;
            lastMessage = msg;
        }

        @Override
        public void error(String template, Object arg) {
            errorCount++;
        }

        @Override
        public void error(String template, Object arg1, Object arg2) {
            errorCount++;
        }

        @Override
        public void error(String template, Object arg1, Object arg2, Object arg3) {
            errorCount++;
        }

        @Override
        public void error(String template, Object arg1, Object arg2, Object arg3, Object arg4) {
            errorCount++;
        }

        @Override
        public void error(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
            errorCount++;
        }

        @Override
        public void error(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
            errorCount++;
        }

        @Override
        public void error(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
            errorCount++;
        }

        @Override
        public void error(String template, Object... args) {
            errorCount++;
        }

        @Override
        public void error(String msg, Throwable t) {
            errorCount++;
            lastMessage = msg;
            lastThrowable = t;
        }

        @Override
        public void error(Throwable t, String msg) {
            errorCount++;
            lastMessage = msg;
            lastThrowable = t;
        }

        @Override
        public void error(Throwable t, String template, Object arg) {
            errorCount++;
            lastThrowable = t;
        }

        @Override
        public void error(Throwable t, String template, Object arg1, Object arg2) {
            errorCount++;
            lastThrowable = t;
        }

        @Override
        public void error(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
            errorCount++;
            lastThrowable = t;
        }

        @Override
        public void error(Supplier<String> supplier) {
            errorCount++;
            lastMessage = supplier.get();
        }

        @Override
        public void error(Supplier<String> supplier, Throwable t) {
            errorCount++;
            lastMessage = supplier.get();
            lastThrowable = t;
        }

        @Override
        public void error(Throwable t, Supplier<String> supplier) {
            errorCount++;
            lastMessage = supplier.get();
            lastThrowable = t;
        }
    }

    @Test
    @DisplayName("Test ROOT_LOGGER_NAME constant")
    public void testRootLoggerNameConstant() {
        assertEquals("ROOT", Logger.ROOT_LOGGER_NAME);
    }

    @Test
    @DisplayName("Test getName method")
    public void testGetName() {
        MockLogger logger = new MockLogger("test.logger");
        assertEquals("test.logger", logger.getName());
    }

    @Test
    @DisplayName("Test all trace methods")
    public void testTraceMethods() {
        MockLogger logger = new MockLogger("test");
        Exception ex = new Exception("Test exception");

        logger.trace("Simple trace");
        assertEquals(1, logger.traceCount);
        assertEquals("Simple trace", logger.lastMessage);

        logger.trace("Template {}", "arg1");
        assertEquals(2, logger.traceCount);

        logger.trace("Template {} {}", "arg1", "arg2");
        assertEquals(3, logger.traceCount);

        logger.trace("Template {} {} {}", "arg1", "arg2", "arg3");
        assertEquals(4, logger.traceCount);

        logger.trace("{} {} {} {}", "a", "b", "c", "d");
        assertEquals(5, logger.traceCount);

        logger.trace("{} {} {} {} {}", 1, 2, 3, 4, 5);
        assertEquals(6, logger.traceCount);

        logger.trace("{} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        assertEquals(7, logger.traceCount);

        logger.trace("{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        assertEquals(8, logger.traceCount);

        logger.trace("Template", "arg1", "arg2", "arg3", "arg4", "arg5", "arg6", "arg7", "arg8");
        assertEquals(9, logger.traceCount);

        logger.trace("Message", ex);
        assertEquals(10, logger.traceCount);
        assertSame(ex, logger.lastThrowable);

        logger.trace(ex, "Message");
        assertEquals(11, logger.traceCount);
        assertSame(ex, logger.lastThrowable);

        logger.trace(ex, "Template {}", "arg");
        assertEquals(12, logger.traceCount);
        assertSame(ex, logger.lastThrowable);

        logger.trace(ex, "Template {} {}", "arg1", "arg2");
        assertEquals(13, logger.traceCount);

        logger.trace(ex, "Template {} {} {}", "arg1", "arg2", "arg3");
        assertEquals(14, logger.traceCount);

        logger.trace(() -> "Supplier message");
        assertEquals(15, logger.traceCount);
        assertEquals("Supplier message", logger.lastMessage);

        logger.trace(() -> "Supplier with exception", ex);
        assertEquals(16, logger.traceCount);
        assertSame(ex, logger.lastThrowable);

        logger.trace(ex, () -> "Exception and supplier");
        assertEquals(17, logger.traceCount);
        assertSame(ex, logger.lastThrowable);
    }

    @Test
    @DisplayName("Test all debug methods")
    public void testDebugMethods() {
        MockLogger logger = new MockLogger("test");
        RuntimeException ex = new RuntimeException("Debug exception");

        logger.debug("Debug message");
        assertEquals(1, logger.debugCount);

        logger.debug("Template {}", "arg");
        logger.debug("Template {} {}", "arg1", "arg2");
        logger.debug("Template {} {} {}", 1, 2, 3);
        logger.debug("{} {} {} {}", "a", "b", "c", "d");
        logger.debug("{} {} {} {} {}", 1, 2, 3, 4, 5);
        logger.debug("{} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        logger.debug("{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        logger.debug("Varargs", "a", "b", "c", "d", "e", "f", "g", "h");
        assertEquals(9, logger.debugCount);

        logger.debug("Debug with exception", ex);
        logger.debug(ex, "Exception first");
        logger.debug(ex, "Template {}", "arg");
        logger.debug(ex, "Template {} {}", "arg1", "arg2");
        logger.debug(ex, "Template {} {} {}", "arg1", "arg2", "arg3");

        logger.debug(() -> "Debug supplier");
        logger.debug(() -> "Supplier with ex", ex);
        logger.debug(ex, () -> "Ex and supplier");

        assertTrue(logger.debugCount > 9);
    }

    @Test
    @DisplayName("Test all info methods")
    public void testInfoMethods() {
        MockLogger logger = new MockLogger("test");
        IllegalArgumentException ex = new IllegalArgumentException("Info exception");

        logger.info("Info message");
        assertEquals(1, logger.infoCount);

        logger.info("Template {}", "arg");
        logger.info("Template {} {}", "arg1", "arg2");
        logger.info("Template {} {} {}", 1, 2, 3);
        logger.info("{} {} {} {}", "a", "b", "c", "d");
        logger.info("{} {} {} {} {}", 1, 2, 3, 4, 5);
        logger.info("{} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        logger.info("{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        logger.info("Varargs", "a", "b", "c", "d", "e", "f", "g", "h");

        logger.info("Info with exception", ex);
        logger.info(ex, "Exception first");
        logger.info(ex, "Template {}", "arg");
        logger.info(ex, "Template {} {}", "arg1", "arg2");
        logger.info(ex, "Template {} {} {}", "arg1", "arg2", "arg3");

        logger.info(() -> "Info supplier");
        logger.info(() -> "Supplier with ex", ex);
        logger.info(ex, () -> "Ex and supplier");

        assertTrue(logger.infoCount > 1);
    }

    @Test
    @DisplayName("Test all warn methods")
    public void testWarnMethods() {
        MockLogger logger = new MockLogger("test");
        IllegalStateException ex = new IllegalStateException("Warn exception");

        logger.warn("Warn message");
        assertEquals(1, logger.warnCount);

        logger.warn("Template {}", "arg");
        logger.warn("Template {} {}", "arg1", "arg2");
        logger.warn("Template {} {} {}", 1, 2, 3);
        logger.warn("{} {} {} {}", "a", "b", "c", "d");
        logger.warn("{} {} {} {} {}", 1, 2, 3, 4, 5);
        logger.warn("{} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        logger.warn("{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        logger.warn("Varargs", "a", "b", "c", "d", "e", "f", "g", "h");

        logger.warn("Warn with exception", ex);
        logger.warn(ex, "Exception first");
        logger.warn(ex, "Template {}", "arg");
        logger.warn(ex, "Template {} {}", "arg1", "arg2");
        logger.warn(ex, "Template {} {} {}", "arg1", "arg2", "arg3");

        logger.warn(() -> "Warn supplier");
        logger.warn(() -> "Supplier with ex", ex);
        logger.warn(ex, () -> "Ex and supplier");

        assertTrue(logger.warnCount > 1);
    }

    @Test
    @DisplayName("Test all error methods")
    public void testErrorMethods() {
        MockLogger logger = new MockLogger("test");
        Error err = new Error("Error exception");

        logger.error("Error message");
        assertEquals(1, logger.errorCount);

        logger.error("Template {}", "arg");
        logger.error("Template {} {}", "arg1", "arg2");
        logger.error("Template {} {} {}", 1, 2, 3);
        logger.error("{} {} {} {}", "a", "b", "c", "d");
        logger.error("{} {} {} {} {}", 1, 2, 3, 4, 5);
        logger.error("{} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);
        logger.error("{} {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6, 7);
        logger.error("Varargs", "a", "b", "c", "d", "e", "f", "g", "h");

        logger.error("Error with exception", err);
        logger.error(err, "Exception first");
        logger.error(err, "Template {}", "arg");
        logger.error(err, "Template {} {}", "arg1", "arg2");
        logger.error(err, "Template {} {} {}", "arg1", "arg2", "arg3");

        logger.error(() -> "Error supplier");
        logger.error(() -> "Supplier with ex", err);
        logger.error(err, () -> "Ex and supplier");

        assertTrue(logger.errorCount > 1);
    }

    @Test
    @DisplayName("Test isEnabled methods")
    public void testIsEnabledMethods() {
        MockLogger logger = new MockLogger("test");

        assertTrue(logger.isTraceEnabled());
        assertTrue(logger.isDebugEnabled());
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
    }

    @Test
    @DisplayName("Test supplier lazy evaluation")
    public void testSupplierLazyEvaluation() {
        MockLogger logger = new MockLogger("test");

        boolean[] called = { false };
        Supplier<String> supplier = () -> {
            called[0] = true;
            return "Lazy message";
        };

        logger.trace(supplier);
        assertTrue(called[0]);
        assertEquals("Lazy message", logger.lastMessage);

        called[0] = false;
        Exception ex = new Exception("Test");
        logger.debug(ex, supplier);
        assertTrue(called[0]);
        assertEquals("Lazy message", logger.lastMessage);
        assertSame(ex, logger.lastThrowable);
    }

    @Test
    @DisplayName("Test deprecated methods")
    @SuppressWarnings("deprecation")
    public void testDeprecatedMethods() {
        MockLogger logger = new MockLogger("test");
        Exception ex = new Exception("Test");

        logger.trace(() -> "Trace deprecated", ex);
        assertEquals("Trace deprecated", logger.lastMessage);
        assertSame(ex, logger.lastThrowable);

        logger.debug(() -> "Debug deprecated", ex);
        assertEquals("Debug deprecated", logger.lastMessage);
        assertSame(ex, logger.lastThrowable);

        logger.info(() -> "Info deprecated", ex);
        assertEquals("Info deprecated", logger.lastMessage);
        assertSame(ex, logger.lastThrowable);

        logger.warn(() -> "Warn deprecated", ex);
        assertEquals("Warn deprecated", logger.lastMessage);
        assertSame(ex, logger.lastThrowable);

        logger.error(() -> "Error deprecated", ex);
        assertEquals("Error deprecated", logger.lastMessage);
        assertSame(ex, logger.lastThrowable);

        logger.trace("Trace varargs {}", "arg1", "arg2");
        logger.debug("Debug varargs {}", "arg1", "arg2");
        logger.info("Info varargs {}", "arg1", "arg2");
        logger.warn("Warn varargs {}", "arg1", "arg2");
        logger.error("Error varargs {}", "arg1", "arg2");
    }
}
