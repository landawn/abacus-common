package com.landawn.abacus.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ThreadMode;

@Tag("2025")
public class EventBus2025Test extends TestBase {

    private EventBus eventBus;

    @BeforeEach
    public void setUp() {
        eventBus = EventBus.create();
    }

    @AfterEach
    public void tearDown() {
        if (eventBus != null) {
            eventBus.removeAllStickyEvents();
        }
    }

    @Test
    public void testGetDefaultInstance() {
        EventBus defaultBus = EventBus.getDefault();
        assertNotNull(defaultBus);
        assertEquals("default", defaultBus.identifier());
    }

    @Test
    public void testConstructorWithIdentifier() {
        EventBus bus = EventBus.create("testBus");
        assertEquals("testBus", bus.identifier());
    }

    @Test
    public void testRegisterAndPost() {
        AtomicReference<String> result = new AtomicReference<>();
        Subscriber<String> subscriber = event -> result.set(event);

        eventBus.register(subscriber, "testId");
        eventBus.post("testId", "hello");

        assertEquals("hello", result.get());
    }

    @Test
    public void testRegisterWithAnnotation() {
        TestHandler handler = new TestHandler();
        eventBus.register(handler);
        eventBus.post("test event");

        assertEquals("test event", handler.lastEvent);
    }

    @Test
    public void testUnregister() {
        AtomicReference<String> result = new AtomicReference<>();
        Subscriber<String> subscriber = event -> result.set(event);

        eventBus.register(subscriber, "testId");
        eventBus.unregister(subscriber);
        eventBus.post("testId", "hello");

        assertNull(result.get());
    }

    @Test
    public void testPostWithoutEventId() {
        AtomicReference<String> result = new AtomicReference<>();
        Subscriber<String> subscriber = event -> result.set(event);

        eventBus.register(subscriber, "testId");
        eventBus.post("hello");

        assertNull(result.get()); // Should not receive without matching event ID
    }

    @Test
    public void testPostWithEmptyEventIdBehavesAsNoEventId() {
        AtomicReference<String> result = new AtomicReference<>();
        Object subscriber = new Object() {
            @Subscribe
            public void onEvent(String event) {
                result.set(event);
            }
        };

        eventBus.register(subscriber);
        eventBus.post("", "hello");

        assertEquals("hello", result.get());
    }

    @Test
    public void testStickyEvent() {
        eventBus.postSticky("sticky message");

        AtomicReference<String> result = new AtomicReference<>();
        Object handler = new Object() {
            @Subscribe(sticky = true)
            public void handle(String event) {
                result.set(event);
            }
        };

        eventBus.register(handler);
        assertEquals("sticky message", result.get());

        eventBus.unregister(handler);
    }

    @Test
    public void testStickyEventWithEmptyEventIdBehavesAsNoEventId() {
        eventBus.postSticky("", "sticky message");

        AtomicReference<String> result = new AtomicReference<>();
        Object handler = new Object() {
            @Subscribe(sticky = true)
            public void handle(String event) {
                result.set(event);
            }
        };

        eventBus.register(handler);
        assertEquals("sticky message", result.get());
        assertEquals(1, eventBus.stickyEvents("", String.class).size());
        assertEquals(1, eventBus.stickyEvents(String.class).size());
        assertTrue(eventBus.removeStickyEvent("", "sticky message"));
        assertTrue(eventBus.stickyEvents(String.class).isEmpty());

        eventBus.unregister(handler);
    }

    @Test
    public void testRemoveStickyEvent() {
        String event = "sticky";
        eventBus.postSticky(event);
        assertTrue(eventBus.removeStickyEvent(event));
        assertFalse(eventBus.removeStickyEvent(event)); // Already removed
    }

    @Test
    public void testGetSubscribers() {
        Subscriber<String> subscriber = event -> {
        };
        eventBus.register(subscriber, "testId");

        List<Object> subscribers = eventBus.subscribers("testId", String.class);
        assertTrue(subscribers.contains(subscriber));

        eventBus.unregister(subscriber);
    }

    @Test
    public void testGetAllSubscribers() {
        Subscriber<String> subscriber1 = event -> {
        };
        Subscriber<Integer> subscriber2 = event -> {
        };

        eventBus.register(subscriber1, "id1");
        eventBus.register(subscriber2, "id2");

        List<Object> allSubscribers = eventBus.allSubscribers();
        assertEquals(2, allSubscribers.size());
        assertTrue(allSubscribers.contains(subscriber1));
        assertTrue(allSubscribers.contains(subscriber2));

        eventBus.unregister(subscriber1);
        eventBus.unregister(subscriber2);
    }

    @Test
    public void testThreadMode() throws InterruptedException {
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<String> threadName = new AtomicReference<>();

        Object handler = new Object() {
            @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
            public void handle(String event) {
                result.set(event);
                threadName.set(Thread.currentThread().getName());
            }
        };

        eventBus.register(handler);
        eventBus.post("async event");

        Thread.sleep(100); // Wait for async execution
        assertEquals("async event", result.get());
        assertNotEquals(Thread.currentThread().getName(), threadName.get());

        eventBus.unregister(handler);
    }

    @Test
    public void testEventIdFiltering() {
        AtomicInteger count = new AtomicInteger(0);
        Object handler = new Object() {
            @Subscribe(eventId = "specific")
            public void handle(String event) {
                count.incrementAndGet();
            }
        };

        eventBus.register(handler);
        eventBus.post("specific", "match");
        eventBus.post("other", "no match");
        eventBus.post("match without ID");

        assertEquals(1, count.get());
        eventBus.unregister(handler);
    }

    @Test
    public void testEqualSubscribersAreTrackedByIdentity() {
        EqualSubscriber first = new EqualSubscriber("same");
        EqualSubscriber second = new EqualSubscriber("same");

        eventBus.register(first);
        eventBus.register(second);
        eventBus.post("event");

        assertEquals(List.of("event"), first.events);
        assertEquals(List.of("event"), second.events);
        assertEquals(2, eventBus.allSubscribers().size());

        eventBus.unregister(first);
        eventBus.post("event2");

        assertEquals(List.of("event"), first.events);
        assertEquals(List.of("event", "event2"), second.events);
    }

    @Test
    public void testSubclassOverrideKeepsSubclassSubscriptionMetadata() {
        ChildOverrideSubscriber subscriber = new ChildOverrideSubscriber();

        eventBus.register(subscriber);
        eventBus.post("base", "ignored");
        eventBus.post("child", "handled");

        assertEquals(List.of("handled"), subscriber.events);
    }

    @Test
    public void testGetStickyEvents() {
        eventBus.postSticky("event1", "sticky1");
        eventBus.postSticky("event1", "sticky2");

        List<?> events = eventBus.stickyEvents("event1", String.class);
        assertEquals(2, events.size());

        eventBus.removeAllStickyEvents();
    }

    @Test
    public void testRemoveStickyEvents() {
        eventBus.postSticky("eventId", "test1");
        eventBus.postSticky("eventId", "test2");

        assertTrue(eventBus.removeStickyEvents("eventId", String.class));
        List<?> events = eventBus.stickyEvents("eventId", String.class);
        assertEquals(0, events.size());
    }

    @Test
    public void testRemoveStickyEventsWithEmptyEventIdBehavesAsNoEventId() {
        eventBus.postSticky("", "test1");
        eventBus.postSticky("", "test2");

        assertTrue(eventBus.removeStickyEvents("", String.class));
        assertEquals(0, eventBus.stickyEvents("", String.class).size());
    }

    static class TestHandler {
        String lastEvent;

        @Subscribe
        public void onEvent(String event) {
            this.lastEvent = event;
        }
    }

    static final class EqualSubscriber {
        final String id;
        final List<String> events = new ArrayList<>();

        EqualSubscriber(String id) {
            this.id = id;
        }

        @Subscribe
        public void onEvent(String event) {
            events.add(event);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof EqualSubscriber other && id.equals(other.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    static class BaseOverrideSubscriber {
        @Subscribe(eventId = "base")
        public void onEvent(String event) {
            throw new AssertionError("Base subscriber metadata should be overridden");
        }
    }

    static final class ChildOverrideSubscriber extends BaseOverrideSubscriber {
        final List<String> events = new ArrayList<>();

        @Override
        @Subscribe(eventId = "child")
        public void onEvent(String event) {
            events.add(event);
        }
    }
}
