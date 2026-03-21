package com.landawn.abacus.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ThreadMode;

public class EventBusTest extends TestBase {

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

    static class TestSubscriber {
        final List<String> receivedEvents = new ArrayList<>();

        @Subscribe
        public void onEvent(String event) {
            receivedEvents.add(event);
        }
    }

    static class TestStickySubscriber {
        final List<String> receivedEvents = new ArrayList<>();

        @Subscribe(sticky = true)
        public void onEvent(String event) {
            receivedEvents.add(event);
        }
    }

    static class StaticAnnotatedSubscriber {
        @Subscribe
        public static void onEvent(String event) {
            // static method - should be rejected
        }
    }

    static class InheritedSubscriberParent {
        final List<String> receivedEvents = new ArrayList<>();

        @Subscribe
        public void onEvent(String event) {
            receivedEvents.add(event);
        }
    }

    static class InheritedSubscriberChild extends InheritedSubscriberParent {
    }

    static class MultiMethodSubscriber {
        int stringCount;
        int integerCount;
        int doubleCount;

        @Subscribe
        public void onString(String event) {
            stringCount++;
        }

        @Subscribe
        public void onInteger(Integer event) {
            integerCount++;
        }

        @Subscribe
        public void onDouble(Double event) {
            doubleCount++;
        }
    }

    static class BaseEvent {
    }

    static class SubEvent extends BaseEvent {
    }

    static class SubSubEvent extends SubEvent {
    }

    static class HierarchySubscriber {
        int baseEventCount;
        int subEventCount;
        int subSubEventCount;

        @Subscribe
        public void onBaseEvent(BaseEvent event) {
            baseEventCount++;
        }

        @Subscribe
        public void onSubEvent(SubEvent event) {
            subEventCount++;
        }

        @Subscribe
        public void onSubSubEvent(SubSubEvent event) {
            subSubEventCount++;
        }
    }

    // ---- getDefault ----

    @Test
    public void testGetDefaultInstance() {
        EventBus defaultBus = EventBus.getDefault();
        assertNotNull(defaultBus);
        assertEquals("default", defaultBus.identifier());
    }

    @Test
    public void testGetDefault() {
        EventBus defaultBus = EventBus.getDefault();
        Assertions.assertNotNull(defaultBus);
        Assertions.assertEquals("default", defaultBus.identifier());

        EventBus anotherDefault = EventBus.getDefault();
        Assertions.assertSame(defaultBus, anotherDefault);
    }

    @Test
    public void testConstructorWithIdentifier() {
        EventBus bus = EventBus.create("testBus");
        assertEquals("testBus", bus.identifier());
    }

    @Test
    public void testCreateTwoBusesHaveDifferentIdentifiers() {
        EventBus bus1 = EventBus.create();
        EventBus bus2 = EventBus.create();
        assertNotEquals(bus1.identifier(), bus2.identifier());
    }

    // ---- create ----

    @Test
    public void testCreate() {
        EventBus bus = EventBus.create();
        Assertions.assertNotNull(bus);
        Assertions.assertNotNull(bus.identifier());
        Assertions.assertFalse(bus.identifier().isEmpty());
    }

    @Test
    public void testCreateWithIdentifierAndExecutor() {
        EventBus bus = EventBus.create("testBus", null);
        Assertions.assertEquals("testBus", bus.identifier());
    }

    @Test
    public void testCreateWithActualExecutor() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            EventBus bus = EventBus.create("executorBus", executor);
            assertEquals("executorBus", bus.identifier());

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Thread> eventThread = new AtomicReference<>();

            Object subscriber = new Object() {
                @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
                public void onEvent(String event) {
                    eventThread.set(Thread.currentThread());
                    latch.countDown();
                }
            };

            bus.register(subscriber);
            bus.post("test");

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            Assertions.assertNotEquals(Thread.currentThread(), eventThread.get());
            bus.unregister(subscriber);
        } finally {
            executor.shutdownNow();
        }
    }

    // ---- identifier ----

    @Test
    public void testIdentifier() {
        EventBus bus = EventBus.create("myIdentifier");
        Assertions.assertEquals("myIdentifier", bus.identifier());
    }

    @Test
    public void testSubscribers_TypeHierarchy() {
        HierarchySubscriber subscriber = new HierarchySubscriber();
        eventBus.register(subscriber);

        // SubEvent extends BaseEvent, so subscriber should appear for SubEvent (because onBaseEvent accepts it)
        List<Object> baseSubs = eventBus.subscribers(BaseEvent.class);
        assertTrue(baseSubs.contains(subscriber));

        List<Object> subSubs = eventBus.subscribers(SubEvent.class);
        assertTrue(subSubs.contains(subscriber));

        eventBus.unregister(subscriber);
    }

    // ---- subscribers(String, Class) ----

    @Test
    public void testGetSubscribers() {
        Subscriber<String> subscriber = event -> {
        };
        eventBus.register(subscriber, "testId");

        List<Object> subscribers = eventBus.subscribers("testId", String.class);
        assertTrue(subscribers.contains(subscriber));

        eventBus.unregister(subscriber);
    }

    // ---- subscribers(Class) ----

    @Test
    public void testSubscribers() {
        TestSubscriber subscriber = new TestSubscriber();
        eventBus.register(subscriber);

        List<Object> subscribers = eventBus.subscribers(String.class);
        Assertions.assertEquals(1, subscribers.size());
        Assertions.assertTrue(subscribers.contains(subscriber));

        List<Object> noSubscribers = eventBus.subscribers(Integer.class);
        Assertions.assertTrue(noSubscribers.isEmpty());

        eventBus.unregister(subscriber);
    }

    @Test
    public void testSubscribers_EmptyBus() {
        List<Object> subscribers = eventBus.subscribers(String.class);
        assertNotNull(subscribers);
        assertTrue(subscribers.isEmpty());
    }

    @Test
    public void testSubscribersWithEventId_NonExistentId() {
        TestSubscriber subscriber = new TestSubscriber();
        eventBus.register(subscriber, "myId");

        List<Object> result = eventBus.subscribers("nonExistent", String.class);
        assertTrue(result.isEmpty());

        eventBus.unregister(subscriber);
    }

    // ---- allSubscribers ----

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
    public void testAllSubscribers_EmptyBus() {
        List<Object> all = eventBus.allSubscribers();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    public void testGetSubscribersWithEventId() {
        TestSubscriber subscriber1 = new TestSubscriber();
        TestSubscriber subscriber2 = new TestSubscriber();

        eventBus.register(subscriber1, "event1");
        eventBus.register(subscriber2, "event2");

        List<Object> subscribers = eventBus.subscribers("event1", String.class);
        Assertions.assertEquals(1, subscribers.size());
        Assertions.assertTrue(subscribers.contains(subscriber1));

        subscribers = eventBus.subscribers("event2", String.class);
        Assertions.assertEquals(1, subscribers.size());
        Assertions.assertTrue(subscribers.contains(subscriber2));
    }

    @Test
    public void testRegisterWithAnnotation() {
        TestHandler handler = new TestHandler();
        eventBus.register(handler);
        eventBus.post("test event");

        assertEquals("test event", handler.lastEvent);
    }

    @Test
    public void testRegisterSubscriberWithInheritedOnMethod() {
        InheritedSubscriberChild subscriber = new InheritedSubscriberChild();

        eventBus.register(subscriber, "inherited");
        eventBus.post("inherited", "inherited-event");

        Assertions.assertEquals(1, subscriber.receivedEvents.size());
        Assertions.assertEquals("inherited-event", subscriber.receivedEvents.get(0));
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
    public void testRegister_ReRegistrationReplaces() {
        TestSubscriber subscriber = new TestSubscriber();
        eventBus.register(subscriber, "id1");
        eventBus.register(subscriber, "id2");

        // After re-registration, subscriber should be under id2 now
        assertEquals(1, eventBus.allSubscribers().size());

        eventBus.post("id1", "msg1");
        eventBus.post("id2", "msg2");

        // Should only receive msg2 since re-registered under id2
        assertEquals(1, subscriber.receivedEvents.size());
        assertEquals("msg2", subscriber.receivedEvents.get(0));

        eventBus.unregister(subscriber);
    }

    @Test
    public void testRegisterAndPost() {
        AtomicReference<String> result = new AtomicReference<>();
        Subscriber<String> subscriber = event -> result.set(event);

        eventBus.register(subscriber, "testId");
        eventBus.post("testId", "hello");

        assertEquals("hello", result.get());
    }

    // ---- register(Subscriber, String) ----

    @Test
    public void testRegisterLambdaSubscriber() {
        AtomicReference<String> received = new AtomicReference<>();
        Subscriber<String> subscriber = event -> received.set(event);

        eventBus.register(subscriber, "lambdaEvent");
        eventBus.post("lambdaEvent", "Hello Lambda");

        Assertions.assertEquals("Hello Lambda", received.get());
    }

    // ---- register(Subscriber, String, ThreadMode) ----

    @Test
    public void testRegisterLambdaSubscriberWithThreadMode() {
        AtomicReference<String> received = new AtomicReference<>();
        Subscriber<String> subscriber = event -> received.set(event);

        eventBus.register(subscriber, "lambdaEvent", ThreadMode.DEFAULT);
        eventBus.post("lambdaEvent", "Hello Lambda");

        Assertions.assertEquals("Hello Lambda", received.get());
    }

    @Test
    public void testEventHierarchy() {
        HierarchySubscriber subscriber = new HierarchySubscriber();
        eventBus.register(subscriber);

        eventBus.post(new BaseEvent());
        eventBus.post(new SubEvent());
        eventBus.post(new SubSubEvent());

        Assertions.assertEquals(3, subscriber.baseEventCount);
        Assertions.assertEquals(2, subscriber.subEventCount);
        Assertions.assertEquals(1, subscriber.subSubEventCount);
    }

    @Test
    public void testStrictEventType() {
        AtomicInteger baseEventCount = new AtomicInteger(0);
        AtomicInteger strictEventCount = new AtomicInteger(0);

        Object subscriber = new Object() {
            @Subscribe
            public void onBaseEvent(BaseEvent event) {
                baseEventCount.incrementAndGet();
            }

            @Subscribe(strictEventType = true)
            public void onStrictBaseEvent(BaseEvent event) {
                strictEventCount.incrementAndGet();
            }
        };

        eventBus.register(subscriber);
        eventBus.post(new BaseEvent());
        eventBus.post(new SubEvent());

        Assertions.assertEquals(2, baseEventCount.get());
        Assertions.assertEquals(1, strictEventCount.get());
    }

    @Test
    public void testDeduplicate() {
        List<String> receivedEvents = new ArrayList<>();

        Object subscriber = new Object() {
            @Subscribe(deduplicate = true)
            public void onEvent(String event) {
                receivedEvents.add(event);
            }
        };

        eventBus.register(subscriber);

        eventBus.post("Event A");
        eventBus.post("Event A");
        eventBus.post("Event B");
        eventBus.post("Event B");
        eventBus.post("Event A");

        Assertions.assertEquals(3, receivedEvents.size());
        Assertions.assertEquals("Event A", receivedEvents.get(0));
        Assertions.assertEquals("Event B", receivedEvents.get(1));
        Assertions.assertEquals("Event A", receivedEvents.get(2));
    }

    @Test
    public void testDeduplicate_FirstEventAlwaysDelivered() {
        List<String> received = new ArrayList<>();
        Object subscriber = new Object() {
            @Subscribe(deduplicate = true)
            public void onEvent(String event) {
                received.add(event);
            }
        };

        eventBus.register(subscriber);
        eventBus.post("only");

        assertEquals(1, received.size());
        assertEquals("only", received.get(0));

        eventBus.unregister(subscriber);
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

    // ---- register(Object) ----

    @Test
    public void testRegister() {
        TestSubscriber subscriber = new TestSubscriber();
        EventBus result = eventBus.register(subscriber);

        Assertions.assertSame(eventBus, result);
        Assertions.assertEquals(1, eventBus.allSubscribers().size());
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
    public void testRegisterMultipleSubscribersForSameEventType() {
        TestSubscriber sub1 = new TestSubscriber();
        TestSubscriber sub2 = new TestSubscriber();

        eventBus.register(sub1);
        eventBus.register(sub2);
        eventBus.post("broadcast");

        assertEquals(1, sub1.receivedEvents.size());
        assertEquals("broadcast", sub1.receivedEvents.get(0));
        assertEquals(1, sub2.receivedEvents.size());
        assertEquals("broadcast", sub2.receivedEvents.get(0));

        eventBus.unregister(sub1);
        eventBus.unregister(sub2);
    }

    // ---- register(Object, String) ----

    @Test
    public void testRegisterWithEventId() {
        TestSubscriber subscriber = new TestSubscriber();
        EventBus result = eventBus.register(subscriber, "testEvent");

        Assertions.assertSame(eventBus, result);
        Assertions.assertEquals(1, eventBus.subscribers("testEvent", String.class).size());
    }

    // ---- register(Object, ThreadMode) ----

    @Test
    public void testRegisterWithThreadMode() {
        TestSubscriber subscriber = new TestSubscriber();
        EventBus result = eventBus.register(subscriber, ThreadMode.DEFAULT);

        Assertions.assertSame(eventBus, result);
        Assertions.assertEquals(1, eventBus.allSubscribers().size());
    }

    // ---- register(Object, String, ThreadMode) ----

    @Test
    public void testRegisterWithEventIdAndThreadMode() {
        TestSubscriber subscriber = new TestSubscriber();
        EventBus result = eventBus.register(subscriber, "testEvent", ThreadMode.DEFAULT);

        Assertions.assertSame(eventBus, result);
        Assertions.assertEquals(1, eventBus.subscribers("testEvent", String.class).size());
    }

    @Test
    public void testMultipleAnnotatedMethods() {
        MultiMethodSubscriber subscriber = new MultiMethodSubscriber();
        eventBus.register(subscriber);

        eventBus.post("String Event");
        eventBus.post(123);
        eventBus.post(45.67);

        Assertions.assertEquals(1, subscriber.stringCount);
        Assertions.assertEquals(1, subscriber.integerCount);
        Assertions.assertEquals(1, subscriber.doubleCount);
    }

    @Test
    public void testRegisterThrowsExceptionForNoSubscriberMethods() {
        Object noMethodSubscriber = new Object();

        Assertions.assertThrows(RuntimeException.class, () -> {
            eventBus.register(noMethodSubscriber);
        });
    }

    @Test
    public void testRegisterRejectsStaticAnnotatedSubscriberMethod() {
        Assertions.assertThrows(RuntimeException.class, () -> eventBus.register(new StaticAnnotatedSubscriber()));
    }

    @Test
    public void testRegister_NullSubscriber() {
        Assertions.assertThrows(NullPointerException.class, () -> eventBus.register(null));
    }

    @Test
    public void testRegisterThrowsExceptionForLambdaWithoutEventId() {
        Subscriber<Object> generalSubscriber = event -> {
        };

        Assertions.assertThrows(RuntimeException.class, () -> {
            eventBus.register(generalSubscriber);
        });
    }

    @Test
    public void testRegisterLambdaSubscriberWithThreadPoolMode() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        Subscriber<String> subscriber = event -> {
            received.set(event);
            latch.countDown();
        };

        eventBus.register(subscriber, "asyncLambda", ThreadMode.THREAD_POOL_EXECUTOR);
        eventBus.post("asyncLambda", "async value");

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("async value", received.get());

        eventBus.unregister(subscriber);
    }

    @Test
    public void testEventInterval() throws InterruptedException {
        AtomicInteger eventCount = new AtomicInteger(0);

        Object subscriber = new Object() {
            @Subscribe(intervalMillis = 100)
            public void onEvent(String event) {
                eventCount.incrementAndGet();
            }
        };

        eventBus.register(subscriber);

        for (int i = 0; i < 5; i++) {
            eventBus.post("Event " + i);
            Thread.sleep(10);
        }

        Assertions.assertEquals(1, eventCount.get());

        Thread.sleep(150);
        eventBus.post("Event after interval");

        Assertions.assertEquals(2, eventCount.get());
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
    public void testThreadPoolExecutorMode() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Thread> eventThread = new AtomicReference<>();

        Object subscriber = new Object() {
            @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
            public void onEvent(String event) {
                eventThread.set(Thread.currentThread());
                latch.countDown();
            }
        };

        eventBus.register(subscriber);
        eventBus.post("Test");

        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assertions.assertNotEquals(Thread.currentThread(), eventThread.get());
    }

    @Test
    public void testUnregister_VerifiesNoEventsAfter() {
        TestSubscriber subscriber = new TestSubscriber();
        eventBus.register(subscriber);

        eventBus.post("before");
        assertEquals(1, subscriber.receivedEvents.size());

        eventBus.unregister(subscriber);
        eventBus.post("after");

        // Should still have only the "before" event
        assertEquals(1, subscriber.receivedEvents.size());
        assertEquals("before", subscriber.receivedEvents.get(0));
    }

    // ---- unregister ----

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
    public void testUnregister_NotRegistered() {
        TestSubscriber subscriber = new TestSubscriber();
        // Unregistering a subscriber that was never registered should not throw
        EventBus result = eventBus.unregister(subscriber);
        Assertions.assertSame(eventBus, result);
    }

    @Test
    public void testUnregister_DoubleUnregister() {
        TestSubscriber subscriber = new TestSubscriber();
        eventBus.register(subscriber);

        EventBus result1 = eventBus.unregister(subscriber);
        Assertions.assertSame(eventBus, result1);

        // Second unregister should not throw
        EventBus result2 = eventBus.unregister(subscriber);
        Assertions.assertSame(eventBus, result2);

        assertTrue(eventBus.allSubscribers().isEmpty());
    }

    // ---- post(String, Object) ----

    @Test
    public void testPostWithEventId() {
        TestSubscriber subscriber1 = new TestSubscriber();
        TestSubscriber subscriber2 = new TestSubscriber();

        eventBus.register(subscriber1, "event1");
        eventBus.register(subscriber2, "event2");

        eventBus.post("event1", "Message 1");

        Assertions.assertEquals(1, subscriber1.receivedEvents.size());
        Assertions.assertEquals("Message 1", subscriber1.receivedEvents.get(0));
        Assertions.assertEquals(0, subscriber2.receivedEvents.size());
    }

    // ---- post(Object) ----

    @Test
    public void testPost() {
        TestSubscriber subscriber = new TestSubscriber();
        eventBus.register(subscriber);

        EventBus result = eventBus.post("Test Message");
        Assertions.assertSame(eventBus, result);
        Assertions.assertEquals(1, subscriber.receivedEvents.size());
        Assertions.assertEquals("Test Message", subscriber.receivedEvents.get(0));
    }

    @Test
    public void testPost_MultipleEventsInSequence() {
        TestSubscriber subscriber = new TestSubscriber();
        eventBus.register(subscriber);

        eventBus.post("first");
        eventBus.post("second");
        eventBus.post("third");

        assertEquals(3, subscriber.receivedEvents.size());
        assertEquals("first", subscriber.receivedEvents.get(0));
        assertEquals("second", subscriber.receivedEvents.get(1));
        assertEquals("third", subscriber.receivedEvents.get(2));

        eventBus.unregister(subscriber);
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
    public void testPost_NullEvent() {
        Assertions.assertThrows(NullPointerException.class, () -> eventBus.post((Object) null));
    }

    @Test
    public void testPost_NoSubscribers() {
        // Posting to a bus with no subscribers should not throw
        EventBus result = eventBus.post("orphan event");
        Assertions.assertSame(eventBus, result);
    }

    @Test
    public void testPost_SubscriberExceptionDoesNotStopOthers() {
        AtomicReference<String> goodResult = new AtomicReference<>();

        Object throwingSubscriber = new Object() {
            @Subscribe
            public void onEvent(String event) {
                throw new RuntimeException("intentional");
            }
        };

        Object goodSubscriber = new Object() {
            @Subscribe
            public void onEvent(String event) {
                goodResult.set(event);
            }
        };

        eventBus.register(throwingSubscriber);
        eventBus.register(goodSubscriber);

        // Should not throw even though one subscriber throws
        eventBus.post("test");

        eventBus.unregister(throwingSubscriber);
        eventBus.unregister(goodSubscriber);
    }

    @Test
    public void testPost_EventIdNoSubscribers() {
        // Posting with eventId that has no subscribers should not throw
        EventBus result = eventBus.post("nonExistentId", "orphan");
        Assertions.assertSame(eventBus, result);
    }

    @Test
    public void testPost_EventIdNullEvent() {
        Assertions.assertThrows(NullPointerException.class, () -> eventBus.post("someId", null));
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

    // ---- postSticky(String, Object) ----

    @Test
    public void testPostStickyWithEventId() {
        TestStickySubscriber subscriber = new TestStickySubscriber();

        eventBus.postSticky("stickyEvent", "Sticky Message");
        eventBus.register(subscriber, "stickyEvent");

        Assertions.assertEquals(1, subscriber.receivedEvents.size());
        Assertions.assertEquals("Sticky Message", subscriber.receivedEvents.get(0));
    }

    @Test
    public void testPostSticky_ThenRegisterWithMatchingEventId() {
        eventBus.postSticky("myId", "sticky value");

        AtomicReference<String> received = new AtomicReference<>();
        Object subscriber = new Object() {
            @Subscribe(sticky = true)
            public void onEvent(String event) {
                received.set(event);
            }
        };

        eventBus.register(subscriber, "myId");
        assertEquals("sticky value", received.get());

        eventBus.unregister(subscriber);
    }

    @Test
    public void testGetStickyEventsWithEventId() {
        eventBus.postSticky("id1", "Event 1");
        eventBus.postSticky("id2", "Event 2");
        eventBus.postSticky("id1", "Event 3");

        List<?> events = eventBus.stickyEvents("id1", String.class);
        Assertions.assertEquals(2, events.size());

        events = eventBus.stickyEvents("id2", String.class);
        Assertions.assertEquals(1, events.size());
    }

    // ---- postSticky(Object) ----

    @Test
    public void testPostSticky() {
        TestStickySubscriber subscriber = new TestStickySubscriber();

        EventBus result = eventBus.postSticky("Sticky Message");
        Assertions.assertSame(eventBus, result);

        eventBus.register(subscriber);

        Assertions.assertEquals(1, subscriber.receivedEvents.size());
        Assertions.assertEquals("Sticky Message", subscriber.receivedEvents.get(0));
    }

    @Test
    public void testPostSticky_NonStickySubscriberDoesNotGetOldEvent() {
        eventBus.postSticky("old sticky");

        TestSubscriber nonSticky = new TestSubscriber();
        eventBus.register(nonSticky);

        // Non-sticky subscriber should not get the sticky event posted before registration
        assertTrue(nonSticky.receivedEvents.isEmpty());

        eventBus.unregister(nonSticky);
    }

    @Test
    public void testPostSticky_MultipleStickyEventsDeliveredToNewSubscriber() {
        eventBus.postSticky("sticky1");
        eventBus.postSticky("sticky2");

        TestStickySubscriber subscriber = new TestStickySubscriber();
        eventBus.register(subscriber);

        assertEquals(2, subscriber.receivedEvents.size());
        assertTrue(subscriber.receivedEvents.contains("sticky1"));
        assertTrue(subscriber.receivedEvents.contains("sticky2"));
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
    public void testPostSticky_WithEventId_NonMatchingSubscriberIgnored() {
        eventBus.postSticky("id1", "sticky msg");

        TestStickySubscriber subscriber = new TestStickySubscriber();
        eventBus.register(subscriber, "id2");

        // subscriber registered with id2 should not get sticky posted with id1
        assertTrue(subscriber.receivedEvents.isEmpty());

        eventBus.unregister(subscriber);
    }

    @Test
    public void testPostStickyRejectsNullEvent() {
        Assertions.assertThrows(NullPointerException.class, () -> eventBus.postSticky((Object) null));
        Assertions.assertThrows(NullPointerException.class, () -> eventBus.postSticky("eventId", null));
    }

    // ---- removeStickyEvent(Object) ----

    @Test
    public void testRemoveStickyEvent() {
        String event = "sticky";
        eventBus.postSticky(event);
        assertTrue(eventBus.removeStickyEvent(event));
        assertFalse(eventBus.removeStickyEvent(event)); // Already removed
    }

    // ---- removeStickyEvent(String, Object) ----

    @Test
    public void testRemoveStickyEventWithEventId() {
        String event = "Sticky Event";
        eventBus.postSticky("eventId", event);

        boolean removed = eventBus.removeStickyEvent("eventId", event);
        Assertions.assertTrue(removed);

        removed = eventBus.removeStickyEvent("wrongId", event);
        Assertions.assertFalse(removed);
    }

    @Test
    public void testRemoveStickyEvent_WrongEventId() {
        String event = "sticky";
        eventBus.postSticky("correctId", event);

        assertFalse(eventBus.removeStickyEvent("wrongId", event));
        assertTrue(eventBus.removeStickyEvent("correctId", event));
    }

    @Test
    public void testRemoveStickyEvent_NonExistent() {
        assertFalse(eventBus.removeStickyEvent("never posted"));
    }

    @Test
    public void testRemoveStickyEvent_WithNullEventId() {
        String event = "sticky";
        eventBus.postSticky(event);

        // Removing with null eventId should match event posted without eventId
        assertTrue(eventBus.removeStickyEvent(null, event));
        assertFalse(eventBus.removeStickyEvent(null, event));
    }

    @Test
    public void testRemoveStickyEvents_NoMatchingType() {
        eventBus.postSticky("hello");
        assertFalse(eventBus.removeStickyEvents(Integer.class));
    }

    // ---- removeStickyEvents(String, Class) ----

    @Test
    public void testRemoveStickyEvents() {
        eventBus.postSticky("eventId", "test1");
        eventBus.postSticky("eventId", "test2");

        assertTrue(eventBus.removeStickyEvents("eventId", String.class));
        List<?> events = eventBus.stickyEvents("eventId", String.class);
        assertEquals(0, events.size());
    }

    @Test
    public void testRemoveStickyEventsWithEventId() {
        eventBus.postSticky("id1", "Event 1");
        eventBus.postSticky("id2", "Event 2");
        eventBus.postSticky("id1", 123);

        boolean removed = eventBus.removeStickyEvents("id1", String.class);
        Assertions.assertTrue(removed);

        List<?> remaining = eventBus.stickyEvents("id1", String.class);
        Assertions.assertEquals(0, remaining.size());

        remaining = eventBus.stickyEvents("id2", String.class);
        Assertions.assertEquals(1, remaining.size());
    }

    @Test
    public void testRemoveStickyEventsWithEventId_1() {
        eventBus.postSticky("id1", "Event 1");
        eventBus.postSticky("id2", "Event 2");
        eventBus.postSticky("id1", "Event 1");

        boolean removed = eventBus.removeStickyEvents("id1", String.class);
        Assertions.assertTrue(removed);

        List<?> remaining = eventBus.stickyEvents("id1", String.class);
        Assertions.assertEquals(0, remaining.size());

        remaining = eventBus.stickyEvents("id2", String.class);
        Assertions.assertEquals(1, remaining.size());
    }

    @Test
    public void testRemoveStickyEvents_WithEventId_NoMatch() {
        eventBus.postSticky("id1", "event");
        assertFalse(eventBus.removeStickyEvents("id2", String.class));
    }

    // ---- removeStickyEvents(Class) ----

    @Test
    public void testRemoveStickyEventsByType() {
        eventBus.postSticky("sticky1");
        eventBus.postSticky("sticky2");

        boolean removed = eventBus.removeStickyEvents(String.class);
        Assertions.assertTrue(removed);

        List<String> remaining = eventBus.stickyEvents(String.class);
        Assertions.assertTrue(remaining.isEmpty());

        // Removing again should return false
        Assertions.assertFalse(eventBus.removeStickyEvents(String.class));
    }

    @Test
    public void testRemoveStickyEventsWithEmptyEventIdBehavesAsNoEventId() {
        eventBus.postSticky("", "test1");
        eventBus.postSticky("", "test2");

        assertTrue(eventBus.removeStickyEvents("", String.class));
        assertEquals(0, eventBus.stickyEvents("", String.class).size());
    }

    @Test
    public void testRemoveStickyEvents_TypeHierarchy() {
        eventBus.postSticky(new SubEvent());
        eventBus.postSticky(new BaseEvent());

        // Removing BaseEvent.class should also remove SubEvent since SubEvent is assignable to BaseEvent
        assertTrue(eventBus.removeStickyEvents(BaseEvent.class));
        assertTrue(eventBus.stickyEvents(BaseEvent.class).isEmpty());
        assertTrue(eventBus.stickyEvents(SubEvent.class).isEmpty());
    }

    // ---- removeAllStickyEvents ----

    @Test
    public void testRemoveAllStickyEvents() {
        eventBus.postSticky("Event 1");
        eventBus.postSticky("Event 2");
        eventBus.postSticky(123);

        eventBus.removeAllStickyEvents();

        Assertions.assertEquals(0, eventBus.stickyEvents(String.class).size());
        Assertions.assertEquals(0, eventBus.stickyEvents(Integer.class).size());
    }

    @Test
    public void testRemoveAllStickyEvents_EmptyBus() {
        // Should not throw on empty bus
        eventBus.removeAllStickyEvents();
        assertTrue(eventBus.stickyEvents(Object.class).isEmpty());
    }

    // ---- stickyEvents(String, Class) ----

    @Test
    public void testGetStickyEvents() {
        eventBus.postSticky("event1", "sticky1");
        eventBus.postSticky("event1", "sticky2");

        List<?> events = eventBus.stickyEvents("event1", String.class);
        assertEquals(2, events.size());

        eventBus.removeAllStickyEvents();
    }

    // ---- stickyEvents(Class) ----

    @Test
    public void testStickyEventsByType() {
        eventBus.postSticky("hello");
        eventBus.postSticky("world");
        eventBus.postSticky(123);

        List<String> stringEvents = eventBus.stickyEvents(String.class);
        Assertions.assertEquals(2, stringEvents.size());
        Assertions.assertTrue(stringEvents.contains("hello"));
        Assertions.assertTrue(stringEvents.contains("world"));

        List<Integer> intEvents = eventBus.stickyEvents(Integer.class);
        Assertions.assertEquals(1, intEvents.size());
        Assertions.assertEquals(123, intEvents.get(0));

        List<Double> doubleEvents = eventBus.stickyEvents(Double.class);
        Assertions.assertTrue(doubleEvents.isEmpty());

        eventBus.removeAllStickyEvents();
    }

    @Test
    public void testStickyEvents_EmptyBus() {
        List<String> events = eventBus.stickyEvents(String.class);
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    public void testStickyEvents_TypeHierarchy() {
        SubEvent subEvent = new SubEvent();
        eventBus.postSticky(subEvent);

        // BaseEvent.class.isAssignableFrom(SubEvent.class) is true
        List<BaseEvent> baseEvents = eventBus.stickyEvents(BaseEvent.class);
        assertEquals(1, baseEvents.size());
        Assertions.assertSame(subEvent, baseEvents.get(0));

        List<SubEvent> subEvents = eventBus.stickyEvents(SubEvent.class);
        assertEquals(1, subEvents.size());

        // SubSubEvent should not match
        List<SubSubEvent> subSubEvents = eventBus.stickyEvents(SubSubEvent.class);
        assertTrue(subSubEvents.isEmpty());
    }

    @Test
    public void testStickyEvents_WithEventId_NonExistent() {
        List<String> events = eventBus.stickyEvents("nonExistent", String.class);
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    // ---- isSupportedThreadMode ----

    @Test
    public void testIsSupportedThreadMode() {
        EventBus bus = EventBus.create();

        Assertions.assertTrue(bus.isSupportedThreadMode(null));
        Assertions.assertTrue(bus.isSupportedThreadMode(ThreadMode.DEFAULT));
        Assertions.assertTrue(bus.isSupportedThreadMode(ThreadMode.THREAD_POOL_EXECUTOR));
    }

}
