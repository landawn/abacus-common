package com.landawn.abacus.eventbus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ThreadMode;

public class EventBusDiagnosticsTest extends TestBase {
    public static class ThrowingText {
        @Override
        public String toString() {
            throw new IllegalStateException("broken event diagnostic");
        }
    }

    public static class FailingHandler {
        @Subscribe(sticky = true)
        public void on(Object event) {
            throw new IllegalArgumentException("original handler failure");
        }
    }

    public static class BrokenHandlerText extends FailingHandler {
        @Override
        public String toString() {
            throw new IllegalStateException("broken subscriber diagnostic");
        }
    }

    public static class Collector {
        final List<Object> events = new ArrayList<>();

        @Subscribe(sticky = true)
        public void on(Object event) {
            events.add(event);
        }
    }

    public static class BrokenCollectorText extends Collector {
        @Override
        public String toString() {
            throw new IllegalStateException("broken subscriber diagnostic");
        }
    }

    private static void withLogs(Level level, Consumer<List<LogEvent>> test) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        var configuration = context.getConfiguration();
        String name = EventBus.class.getName();
        LoggerConfig previous = configuration.getLoggers().get(name);
        List<LogEvent> events = new ArrayList<>();
        AbstractAppender appender = new AbstractAppender("eventbus-diagnostics-review", null, null, false, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                events.add(event.toImmutable());
            }
        };
        appender.start();
        LoggerConfig config = new LoggerConfig(name, level, false);
        config.addAppender(appender, null, null);
        configuration.removeLogger(name);
        configuration.addLogger(name, config);
        context.updateLoggers();
        try {
            test.accept(events);
        } finally {
            configuration.removeLogger(name);
            if (previous != null) {
                configuration.addLogger(name, previous);
            }
            context.updateLoggers();
            appender.stop();
        }
    }

    @Test
    public void brokenEventDescriptionDoesNotHideFailureOrSkipHealthySubscriber() {
        withLogs(Level.INFO, logs -> {
            EventBus bus = EventBus.create();
            Collector healthy = new Collector();
            bus.register(new FailingHandler(), "\u4e8b\u4ef6\ud83d\ude80").register(healthy, "\u4e8b\u4ef6\ud83d\ude80");
            ThrowingText event = new ThrowingText();
            assertDoesNotThrow(() -> bus.post("\u4e8b\u4ef6\ud83d\ude80", event));
            assertEquals(1, healthy.events.size());
            assertSame(event, healthy.events.get(0));
            assertTrue(logs.stream().anyMatch(log -> log.getMessage().getFormattedMessage().contains(ThrowingText.class.getName() + "@")));
            assertTrue(logs.stream()
                    .anyMatch(log -> log.getThrown() != null && log.getThrown().getCause() != null
                            && "original handler failure".equals(log.getThrown().getCause().getMessage())));
        });
    }

    @Test
    public void brokenSubscriberDescriptionKeepsEmptyAndUnicodeDelivery() {
        withLogs(Level.INFO, logs -> {
            EventBus bus = EventBus.create();
            Collector healthy = new Collector();
            bus.register(new BrokenHandlerText()).register(healthy);
            assertDoesNotThrow(() -> bus.post("").post("\ud83d\ude80"));
            assertEquals(List.of("", "\ud83d\ude80"), healthy.events);
            assertTrue(logs.stream().anyMatch(log -> log.getMessage().getFormattedMessage().contains(BrokenHandlerText.class.getName() + "@")));
        });
    }

    @Test
    public void rejectedAsyncDispatchDoesNotSkipSynchronousSubscriber() {
        withLogs(Level.INFO, logs -> {
            EventBus bus = EventBus.create("reject", task -> {
                throw new RejectedExecutionException("expected rejection");
            });
            Collector healthy = new Collector();
            bus.register(new Collector(), "id", ThreadMode.THREAD_POOL_EXECUTOR).register(healthy, "id");
            ThrowingText event = new ThrowingText();
            assertDoesNotThrow(() -> bus.post("id", event));
            assertEquals(1, healthy.events.size());
            assertSame(event, healthy.events.get(0));
            assertTrue(logs.stream().anyMatch(log -> log.getThrown() instanceof RejectedExecutionException));
        });
    }

    @Test
    public void stickyReplayDiagnosticsDoNotAbortRegistration() {
        withLogs(Level.INFO, logs -> {
            EventBus bus = EventBus.create("sticky", task -> {
                throw new RejectedExecutionException("expected rejection");
            });
            ThrowingText event = new ThrowingText();
            bus.postSticky("id", event);
            assertDoesNotThrow(() -> bus.register(new Collector(), "id", ThreadMode.THREAD_POOL_EXECUTOR));
            assertDoesNotThrow(() -> bus.register(new BrokenHandlerText(), "id"));
            Collector healthy = new Collector();
            assertDoesNotThrow(() -> bus.register(healthy, "id"));
            assertEquals(1, healthy.events.size());
            assertSame(event, healthy.events.get(0));
            assertTrue(logs.stream().anyMatch(log -> log.getMessage().getFormattedMessage().startsWith("Failed to post sticky event:")));
        });
    }

    @Test
    public void debugDescriptionsCannotBlockRegistrationDeliveryOrUnregistration() {
        withLogs(Level.DEBUG, logs -> {
            EventBus bus = EventBus.create();
            BrokenCollectorText handler = new BrokenCollectorText();
            assertDoesNotThrow(() -> bus.register(handler));
            ThrowingText event = new ThrowingText();
            assertDoesNotThrow(() -> bus.post((String) null, event).post("", "\ud83d\ude80").post(""));
            assertEquals(3, handler.events.size());
            assertSame(event, handler.events.get(0));
            assertEquals(List.of("\ud83d\ude80", ""), handler.events.subList(1, 3));
            assertThrows(IllegalArgumentException.class, () -> bus.post((Object) null));
            assertDoesNotThrow(() -> bus.unregister(handler));
            assertEquals(0, bus.countOfSubscribers());
            assertTrue(logs.stream().anyMatch(log -> log.getMessage().getFormattedMessage().contains("Posting event: \ud83d\ude80")));
            assertTrue(logs.stream().anyMatch(log -> log.getMessage().getFormattedMessage().startsWith("Unregistering subscriber:")));
        });
    }
}
