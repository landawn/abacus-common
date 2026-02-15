package com.landawn.abacus.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        eventBus = new EventBus();
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
        EventBus bus = new EventBus("testBus");
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

        assertNull(result.get());   // Should not receive without matching event ID
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
    public void testRemoveStickyEvent() {
        String event = "sticky";
        eventBus.postSticky(event);
        assertTrue(eventBus.removeStickyEvent(event));
        assertFalse(eventBus.removeStickyEvent(event));   // Already removed
    }

    @Test
    public void testGetSubscribers() {
        Subscriber<String> subscriber = event -> {
        };
        eventBus.register(subscriber, "testId");

        List<Object> subscribers = eventBus.getSubscribers("testId", String.class);
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

        List<Object> allSubscribers = eventBus.getAllSubscribers();
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

        Thread.sleep(100);   // Wait for async execution
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
    public void testGetStickyEvents() {
        eventBus.postSticky("event1", "sticky1");
        eventBus.postSticky("event1", "sticky2");

        List<Object> events = eventBus.getStickyEvents("event1", String.class);
        assertEquals(2, events.size());

        eventBus.removeAllStickyEvents();
    }

    @Test
    public void testRemoveStickyEvents() {
        eventBus.postSticky("eventId", "test1");
        eventBus.postSticky("eventId", "test2");

        assertTrue(eventBus.removeStickyEvents("eventId", String.class));
        List<Object> events = eventBus.getStickyEvents("eventId", String.class);
        assertEquals(0, events.size());
    }

    static class TestHandler {
        String lastEvent;

        @Subscribe
        public void onEvent(String event) {
            this.lastEvent = event;
        }
    }
}
