package com.landawn.abacus.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.spi.LocationAwareLogger;

import com.landawn.abacus.TestBase;

public class LoggerCallerTest extends TestBase {
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4 })
    public void log4jEveryOverloadPointsToApplication(final int level) {
        final String name = "review.caller." + level + "." + System.nanoTime();
        final LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        final List<StackTraceElement> locations = new ArrayList<>();
        final List<Level> levels = new ArrayList<>();
        final AbstractAppender appender = new AbstractAppender(name, null, null, false, Property.EMPTY_ARRAY) {
            @Override
            public void append(final LogEvent event) {
                locations.add(event.getSource());
                levels.add(event.getLevel());
            }
        };
        final LoggerConfig config = new LoggerConfig(name, Level.ALL, false);
        appender.start();
        config.addAppender(appender, Level.ALL, null);
        context.getConfiguration().addLogger(name, config);
        context.updateLoggers();
        try {
            emit(new Log4Jv2Logger(name), level);
            assertEquals(17, locations.size());
            final Level expected = new Level[] { Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR }[level];
            for (int i = 0; i < locations.size(); i++) {
                assertNotNull(locations.get(i), "missing caller at overload " + i);
                assertEquals(getClass().getName(), locations.get(i).getClassName(), "overload " + i);
                assertEquals("emit", locations.get(i).getMethodName(), "overload " + i);
                assertEquals(expected, levels.get(i));
            }
        } finally {
            context.getConfiguration().removeLogger(name);
            context.updateLoggers();
            appender.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4 })
    public void slf4jLocationBoundaryIncludesInheritedOverloads(final int level) {
        final List<StackTraceElement> locations = new ArrayList<>();
        final List<Integer> levels = new ArrayList<>();
        final LocationAwareLogger backend = (LocationAwareLogger) Proxy.newProxyInstance(LocationAwareLogger.class.getClassLoader(),
                new Class<?>[] { LocationAwareLogger.class }, (proxy, method, args) -> {
                    if (method.getName().equals("log")) {
                        // Resolve the supplied boundary with a real backend stack locator.
                        locations.add(StackLocatorUtil.calcLocation((String) args[1]));
                        levels.add((Integer) args[2]);
                        return null;
                    }
                    if (method.getName().startsWith("is")) {
                        return true;
                    }
                    return null;
                });
        emit(new SLF4JLogger("review.slf4j", backend), level);
        assertEquals(17, locations.size());
        for (int i = 0; i < locations.size(); i++) {
            assertNotNull(locations.get(i), "missing caller at overload " + i);
            assertEquals(getClass().getName(), locations.get(i).getClassName(), "overload " + i);
            assertEquals("emit", locations.get(i).getMethodName(), "overload " + i);
            assertEquals(level * 10, levels.get(i));
        }
    }

    @Test
    public void disabledSuppliersAndArgumentsAreNotEvaluated() {
        final AtomicInteger logs = new AtomicInteger();
        final LocationAwareLogger backend = (LocationAwareLogger) Proxy.newProxyInstance(LocationAwareLogger.class.getClassLoader(),
                new Class<?>[] { LocationAwareLogger.class }, (proxy, method, args) -> {
                    if (method.getName().startsWith("is")) {
                        return false;
                    }
                    if (method.getName().equals("log")) {
                        logs.incrementAndGet();
                    }
                    return null;
                });
        final Logger logger = new SLF4JLogger("disabled", backend);
        final java.util.function.Supplier<String> supplier = () -> {
            throw new AssertionError("evaluated disabled supplier");
        };
        final Object argument = new Object() {
            @Override
            public String toString() {
                throw new AssertionError("formatted disabled argument");
            }
        };
        logger.trace(supplier);
        logger.debug(supplier);
        logger.info(supplier);
        logger.warn(supplier);
        logger.error(supplier);
        logger.trace("{}", argument);
        logger.debug("{}", argument);
        logger.info("{}", argument);
        logger.warn("{}", argument);
        logger.error("{}", argument);
        assertEquals(0, logs.get());
        assertThrows(IllegalArgumentException.class, () -> logger.info((java.util.function.Supplier<String>) null));
    }

    @Test
    public void supplierCanLogRecursivelyWithoutLeakingCallerState() {
        final List<StackTraceElement> locations = new ArrayList<>();
        final LocationAwareLogger backend = (LocationAwareLogger) Proxy.newProxyInstance(LocationAwareLogger.class.getClassLoader(),
                new Class<?>[] { LocationAwareLogger.class }, (proxy, method, args) -> {
                    if (method.getName().startsWith("is")) {
                        return true;
                    }
                    if (method.getName().equals("log")) {
                        locations.add(StackLocatorUtil.calcLocation((String) args[1]));
                    }
                    return null;
                });
        final Logger logger = new SLF4JLogger("recursive", backend);
        logger.info(() -> {
            logger.info("nested");
            return "";
        });
        logger.info((String) null);
        assertEquals(3, locations.size());
        for (final StackTraceElement location : locations) {
            assertEquals(getClass().getName(), location.getClassName());
        }
        final IllegalStateException failure = new IllegalStateException("supplier");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> logger.info(() -> {
            throw failure;
        })));
    }

    @SuppressWarnings("deprecation")
    private static void emit(final Logger logger, final int level) {
        final Exception failure = new Exception("test");
        switch (level) {
            case 0 -> {
                logger.trace("direct");
                logger.trace("direct throwable", failure);
                logger.trace("{}", "one");
                logger.trace("{} {}", "one", "two");
                logger.trace("{} {} {}", "one", "two", "three");
                logger.trace("{}", 1, 2, 3, 4);
                logger.trace("{}", 1, 2, 3, 4, 5);
                logger.trace("{}", 1, 2, 3, 4, 5, 6);
                logger.trace("{}", 1, 2, 3, 4, 5, 6, 7);
                logger.trace("{}", new Object[] { "\u03bb", null });
                logger.trace(failure, "throwable");
                logger.trace(failure, "{}", 1);
                logger.trace(failure, "{} {}", 1, 2);
                logger.trace(failure, "{} {} {}", 1, 2, 3);
                logger.trace(() -> "supplier");
                logger.trace(() -> "supplier throwable", failure);
                logger.trace(failure, () -> "throwable supplier");
            }
            case 1 -> {
                logger.debug("direct");
                logger.debug("direct throwable", failure);
                logger.debug("{}", "one");
                logger.debug("{} {}", "one", "two");
                logger.debug("{} {} {}", "one", "two", "three");
                logger.debug("{}", 1, 2, 3, 4);
                logger.debug("{}", 1, 2, 3, 4, 5);
                logger.debug("{}", 1, 2, 3, 4, 5, 6);
                logger.debug("{}", 1, 2, 3, 4, 5, 6, 7);
                logger.debug("{}", new Object[] { "\u03bb", null });
                logger.debug(failure, "throwable");
                logger.debug(failure, "{}", 1);
                logger.debug(failure, "{} {}", 1, 2);
                logger.debug(failure, "{} {} {}", 1, 2, 3);
                logger.debug(() -> "supplier");
                logger.debug(() -> "supplier throwable", failure);
                logger.debug(failure, () -> "throwable supplier");
            }
            case 2 -> {
                logger.info("direct");
                logger.info("direct throwable", failure);
                logger.info("{}", "one");
                logger.info("{} {}", "one", "two");
                logger.info("{} {} {}", "one", "two", "three");
                logger.info("{}", 1, 2, 3, 4);
                logger.info("{}", 1, 2, 3, 4, 5);
                logger.info("{}", 1, 2, 3, 4, 5, 6);
                logger.info("{}", 1, 2, 3, 4, 5, 6, 7);
                logger.info("{}", new Object[] { "\u03bb", null });
                logger.info(failure, "throwable");
                logger.info(failure, "{}", 1);
                logger.info(failure, "{} {}", 1, 2);
                logger.info(failure, "{} {} {}", 1, 2, 3);
                logger.info(() -> "supplier");
                logger.info(() -> "supplier throwable", failure);
                logger.info(failure, () -> "throwable supplier");
            }
            case 3 -> {
                logger.warn("direct");
                logger.warn("direct throwable", failure);
                logger.warn("{}", "one");
                logger.warn("{} {}", "one", "two");
                logger.warn("{} {} {}", "one", "two", "three");
                logger.warn("{}", 1, 2, 3, 4);
                logger.warn("{}", 1, 2, 3, 4, 5);
                logger.warn("{}", 1, 2, 3, 4, 5, 6);
                logger.warn("{}", 1, 2, 3, 4, 5, 6, 7);
                logger.warn("{}", new Object[] { "\u03bb", null });
                logger.warn(failure, "throwable");
                logger.warn(failure, "{}", 1);
                logger.warn(failure, "{} {}", 1, 2);
                logger.warn(failure, "{} {} {}", 1, 2, 3);
                logger.warn(() -> "supplier");
                logger.warn(() -> "supplier throwable", failure);
                logger.warn(failure, () -> "throwable supplier");
            }
            case 4 -> {
                logger.error("direct");
                logger.error("direct throwable", failure);
                logger.error("{}", "one");
                logger.error("{} {}", "one", "two");
                logger.error("{} {} {}", "one", "two", "three");
                logger.error("{}", 1, 2, 3, 4);
                logger.error("{}", 1, 2, 3, 4, 5);
                logger.error("{}", 1, 2, 3, 4, 5, 6);
                logger.error("{}", 1, 2, 3, 4, 5, 6, 7);
                logger.error("{}", new Object[] { "\u03bb", null });
                logger.error(failure, "throwable");
                logger.error(failure, "{}", 1);
                logger.error(failure, "{} {}", 1, 2);
                logger.error(failure, "{} {} {}", 1, 2, 3);
                logger.error(() -> "supplier");
                logger.error(() -> "supplier throwable", failure);
                logger.error(failure, () -> "throwable supplier");
            }
            default -> throw new AssertionError(level);
        }
    }
}
