package com.landawn.abacus.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class LoggerInitializationTest extends TestBase {
    @Test
    public void failedInitializationFallsBackBeforeCaching() throws Exception {
        synchronized (LoggerFactory.class) {
            final Field type = field("logType");
            final Field initialized = field("initialized");
            final int oldType = type.getInt(null);
            final boolean oldInitialized = initialized.getBoolean(null);
            final java.util.logging.Logger diagnostic = java.util.logging.Logger.getLogger(LoggerFactory.class.getName());
            final Level oldLevel = diagnostic.getLevel();
            final AtomicInteger calls = new AtomicInteger();
            final String name = "review.initialization.\u03bb." + System.nanoTime();
            final Handler handler = new Handler() {
                @Override
                public void publish(final LogRecord record) {
                    if (calls.getAndIncrement() == 0) {
                        throw new IllegalStateException("backend initialization failed");
                    }
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };
            try {
                type.setInt(null, 2);
                initialized.setBoolean(null, false);
                diagnostic.setLevel(Level.ALL);
                diagnostic.addHandler(handler);
                final Logger result = LoggerFactory.getLogger(name);
                assertInstanceOf(JdkLogger.class, result);
                assertTrue(initialized.getBoolean(null));
                assertEquals(3, type.getInt(null));
                assertSame(result, LoggerFactory.getLogger(name));
                assertEquals(name, result.getName());
            } finally {
                diagnostic.removeHandler(handler);
                diagnostic.setLevel(oldLevel);
                type.setInt(null, oldType);
                initialized.setBoolean(null, oldInitialized);
                ((Map<?, ?>) field("namedLoggers").get(null)).remove(name);
            }
        }
    }

    @Test
    public void virtualMachineErrorsEscapeReflectiveWrappersByIdentity() {
        final OutOfMemoryError fatal = new OutOfMemoryError("test only");
        assertSame(fatal, assertThrows(OutOfMemoryError.class, () -> LoggerFactory.rethrowIfFatal(fatal)));
        assertSame(fatal,
                assertThrows(OutOfMemoryError.class, () -> LoggerFactory.rethrowIfFatal(new InvocationTargetException(new InvocationTargetException(fatal)))));
    }

    @Test
    @SuppressWarnings("removal")
    public void threadDeathEscapesDirectlyAndReflectively() {
        final ThreadDeath fatal = new ThreadDeath();
        assertSame(fatal, assertThrows(ThreadDeath.class, () -> LoggerFactory.rethrowIfFatal(fatal)));
        assertSame(fatal, assertThrows(ThreadDeath.class, () -> LoggerFactory.rethrowIfFatal(new InvocationTargetException(fatal))));
    }

    @Test
    public void nonFatalAndAbsentReflectiveCausesAllowFallback() {
        assertDoesNotThrow(() -> LoggerFactory.rethrowIfFatal(new LinkageError("optional backend absent")));
        assertDoesNotThrow(() -> LoggerFactory.rethrowIfFatal(new InvocationTargetException(new IllegalStateException(""))));
        assertDoesNotThrow(() -> LoggerFactory.rethrowIfFatal(new InvocationTargetException(null)));
    }

    private static Field field(final String name) throws Exception {
        final Field field = LoggerFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    // ---- review fixes 2026-09-06 (a11 F-2): JDK-fallback banner failure is swallowed and the logger cached ----

    @Test
    public void jdkFallbackBannerFailureIsSwallowedAndLoggerCached() throws Exception {
        for (final int startType : new int[] { 3, 0, 2 }) {
            runJdkFallbackWithAlwaysThrowingHandler(startType);
        }
    }

    private static void runJdkFallbackWithAlwaysThrowingHandler(final int startType) throws Exception {
        synchronized (LoggerFactory.class) {
            final Field type = field("logType");
            final Field initialized = field("initialized");
            final int oldType = type.getInt(null);
            final boolean oldInitialized = initialized.getBoolean(null);
            final java.util.logging.Logger diagnostic = java.util.logging.Logger.getLogger(LoggerFactory.class.getName());
            final Level oldLevel = diagnostic.getLevel();
            final AtomicInteger calls = new AtomicInteger();
            final String name = "review.jdk.banner.\u03bb." + startType + "." + System.nanoTime();
            final Handler handler = new Handler() {
                @Override
                public void publish(final LogRecord record) {
                    calls.incrementAndGet();
                    throw new IllegalStateException("handler fails on every publish");
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };
            try {
                type.setInt(null, startType);
                initialized.setBoolean(null, false);
                diagnostic.setLevel(Level.ALL);
                diagnostic.addHandler(handler);
                final Logger result = assertDoesNotThrow(() -> LoggerFactory.getLogger(name), "start logType=" + startType);
                assertInstanceOf(JdkLogger.class, result);
                assertEquals(name, result.getName());
                assertTrue(calls.get() >= 1, "the banner must actually have reached the throwing handler");
                assertTrue(initialized.getBoolean(null));
                assertEquals(3, type.getInt(null));
                assertTrue(((Map<?, ?>) field("namedLoggers").get(null)).containsKey(name));
                assertSame(result, LoggerFactory.getLogger(name));
            } finally {
                diagnostic.removeHandler(handler);
                diagnostic.setLevel(oldLevel);
                type.setInt(null, oldType);
                initialized.setBoolean(null, oldInitialized);
                ((Map<?, ?>) field("namedLoggers").get(null)).remove(name);
            }
        }
    }

    @Test
    public void jdkFallbackBannerVirtualMachineErrorStillPropagates() throws Exception {
        synchronized (LoggerFactory.class) {
            final Field type = field("logType");
            final Field initialized = field("initialized");
            final int oldType = type.getInt(null);
            final boolean oldInitialized = initialized.getBoolean(null);
            final java.util.logging.Logger diagnostic = java.util.logging.Logger.getLogger(LoggerFactory.class.getName());
            final Level oldLevel = diagnostic.getLevel();
            final OutOfMemoryError fatal = new OutOfMemoryError("test only");
            final String name = "review.jdk.banner.fatal." + System.nanoTime();
            // Initialize every class on the getLogger/JdkLogger path first: a fatal error escaping from inside a
            // static initializer that happens to run during this call (e.g. CommonUtil's own logger lookup)
            // would poison that class for the rest of the JVM.
            LoggerFactory.getLogger(name + ".warmup");
            new JdkLogger(name + ".warmup").info("warm-up");
            final Handler handler = new Handler() {
                @Override
                public void publish(final LogRecord record) {
                    throw fatal;
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };
            try {
                type.setInt(null, 3);
                initialized.setBoolean(null, false);
                diagnostic.setLevel(Level.ALL);
                diagnostic.addHandler(handler);
                assertSame(fatal, assertThrows(OutOfMemoryError.class, () -> LoggerFactory.getLogger(name)));
                assertFalse(initialized.getBoolean(null));
                assertFalse(((Map<?, ?>) field("namedLoggers").get(null)).containsKey(name));
            } finally {
                diagnostic.removeHandler(handler);
                diagnostic.setLevel(oldLevel);
                type.setInt(null, oldType);
                initialized.setBoolean(null, oldInitialized);
                ((Map<?, ?>) field("namedLoggers").get(null)).remove(name);
                ((Map<?, ?>) field("namedLoggers").get(null)).remove(name + ".warmup");
            }
        }
    }
}
