package com.landawn.abacus.eventbus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
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

    @Test
    public void testUnregisterWaitsForConcurrentRegistrationToUpdateBothIndexes() throws Exception {
        final TestHandler subscriber = new TestHandler();
        final java.lang.reflect.Field indexField = EventBus.class.getDeclaredField("registeredEventIdSubMap");
        indexField.setAccessible(true);
        final Object eventIdIndex = indexField.get(eventBus);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread registering = new Thread(() -> {
            try {
                eventBus.register(subscriber, "concurrent");
            } catch (Throwable e) {
                failure.compareAndSet(null, e);
            }
        });
        final Thread unregistering = new Thread(() -> {
            try {
                eventBus.unregister(subscriber);
            } catch (Throwable e) {
                failure.compareAndSet(null, e);
            }
        });

        try {
            synchronized (eventIdIndex) {
                // Pause registration after its main-registry update, before its event-ID update.
                registering.start();
                awaitBlockedReviewThread(registering);
                assertEquals(List.of(subscriber), eventBus.allSubscribers());

                unregistering.start();
                awaitBlockedReviewThread(unregistering);
                // Removal must wait for the pending registration transaction, otherwise its
                // later event-ID insertion can leave an unlisted subscriber receiving events.
                assertEquals(List.of(subscriber), eventBus.allSubscribers());
            }
        } finally {
            registering.join(5000);
            unregistering.join(5000);
        }

        assertFalse(registering.isAlive());
        assertFalse(unregistering.isAlive());
        assertNull(failure.get());
        assertTrue(eventBus.allSubscribers().isEmpty());
        eventBus.post("concurrent", "after removal");
        assertNull(subscriber.lastEvent);
    }

    private static void awaitBlockedReviewThread(final Thread thread) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (thread.getState() != Thread.State.BLOCKED && thread.isAlive() && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
        assertEquals(Thread.State.BLOCKED, thread.getState());
    }

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

    public static class TestHandler {
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

    public static class BaseOverrideSubscriber {
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

    public static class TestSubscriber {
        final List<String> receivedEvents = new ArrayList<>();

        @Subscribe
        public void onEvent(String event) {
            receivedEvents.add(event);
        }
    }

    public static class TestStickySubscriber {
        final List<String> receivedEvents = new ArrayList<>();

        @Subscribe(sticky = true)
        public void onEvent(String event) {
            receivedEvents.add(event);
        }
    }

    public static class StaticAnnotatedSubscriber {
        @Subscribe
        public static void onEvent(String event) {
            // static method - should be rejected
        }
    }

    public static class InheritedSubscriberParent {
        final List<String> receivedEvents = new ArrayList<>();

        @Subscribe
        public void onEvent(String event) {
            receivedEvents.add(event);
        }
    }

    public static class InheritedSubscriberChild extends InheritedSubscriberParent {
    }

    public static class MultiMethodSubscriber {
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

    public static class BaseEvent {
    }

    public static class SubEvent extends BaseEvent {
    }

    public static class SubSubEvent extends SubEvent {
    }

    public static class HierarchySubscriber {
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
    public void testGetDefault() {
        EventBus defaultBus = EventBus.getDefault();
        assertNotNull(defaultBus);
        assertEquals("default", defaultBus.identifier());
        Assertions.assertSame(defaultBus, EventBus.getDefault());
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
    public void testCreateWithNullExecutor() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> EventBus.create("testBus", null));
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

    @Test
    public void testSubscribersTreatsEmptyEventIdAsNoEventId() {
        TestSubscriber subscriber = new TestSubscriber();
        eventBus.register(subscriber);

        assertTrue(eventBus.subscribers("", String.class).contains(subscriber));

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

    @Test
    public void testTypeQueriesRejectNullDeterministically() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.subscribers((Class<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.subscribers(null, (Class<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.stickyEvents((Class<Object>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.stickyEvents(null, (Class<Object>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.removeStickyEvents((Class<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.removeStickyEvents(null, (Class<?>) null));

        eventBus.register(new TestSubscriber());
        eventBus.postSticky("sticky");

        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.subscribers((Class<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.stickyEvents((Class<Object>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.removeStickyEvents((Class<?>) null));
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
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.register(null));
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
    public void testEventIntervalUsesMonotonicElapsedTimeAndHonorsBoundary() throws NoSuchMethodException {
        class SubscriberWithInterval {
            @Subscribe(intervalMillis = 100)
            public void onEvent(String event) {
                // No-op: this test exercises the interval state carried by the subscriber metadata.
            }
        }

        final Method method = SubscriberWithInterval.class.getDeclaredMethod("onEvent", String.class);
        final EventBus.SubIdentifier sub = new EventBus.SubIdentifier(method);
        final long firstPostTime = 1_000_000_000L;
        final long intervalNanos = TimeUnit.MILLISECONDS.toNanos(100);

        assertFalse(sub.isWithinPostInterval(firstPostTime));

        sub.recordPostTime(firstPostTime);

        assertTrue(sub.isWithinPostInterval(firstPostTime + intervalNanos - 1));
        assertFalse(sub.isWithinPostInterval(firstPostTime + intervalNanos));
        assertFalse(sub.isWithinPostInterval(firstPostTime - 1));
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
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.post((Object) null));
    }

    @Test
    public void testPost_NoSubscribers() {
        // Posting to a bus with no subscribers should not throw
        EventBus result = eventBus.post("orphan event");
        Assertions.assertSame(eventBus, result);
    }

    @Test
    public void testPost_SubscriberExceptionDoesNotStopOthers() {
        assertDoesNotThrow(() -> {
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
        });
    }

    @Test
    public void testPost_EventIdNoSubscribers() {
        // Posting with eventId that has no subscribers should not throw
        EventBus result = eventBus.post("nonExistentId", "orphan");
        Assertions.assertSame(eventBus, result);
    }

    @Test
    public void testPost_EventIdNullEvent() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.post("someId", null));
    }

    @Test
    public void testStickyPostThenRegister_deliveredExactlyOnce() {
        // Event already recorded before the subscriber registers: delivered once via register's replay.
        TestStickySubscriber sub = new TestStickySubscriber();
        eventBus.postSticky("only-once");
        eventBus.register(sub);

        assertEquals(1, sub.receivedEvents.size());
        assertEquals("only-once", sub.receivedEvents.get(0));
    }

    @Test
    public void testStickyRegisterThenPostSticky_deliveredExactlyOnce() {
        // Subscriber already visible before the sticky event is posted: delivered once via postSticky,
        // and register's earlier (empty) sticky snapshot must not also deliver it.
        TestStickySubscriber sub = new TestStickySubscriber();
        eventBus.register(sub);
        eventBus.postSticky("only-once");

        assertEquals(1, sub.receivedEvents.size());
        assertEquals("only-once", sub.receivedEvents.get(0));
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
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.postSticky((Object) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> eventBus.postSticky("eventId", null));
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
        Assertions.assertEquals(1, eventBus.stickyEvents("id1", Integer.class).size());

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

        assertTrue(eventBus.removeAllStickyEvents());

        Assertions.assertEquals(0, eventBus.stickyEvents(String.class).size());
        Assertions.assertEquals(0, eventBus.stickyEvents(Integer.class).size());
    }

    @Test
    public void testRemoveAllStickyEvents_EmptyBus() {
        // Should not throw on empty bus and should report that nothing was removed.
        assertFalse(eventBus.removeAllStickyEvents());
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

    // ---- Bug fix: double-checked locking in post(String, Object) ----

    /**
     * Verifies that concurrent calls to {@code post(Object)} (no eventId) never deliver
     * an event more than once per subscriber, even when the internal subscriber-list cache
     * ({@code listOfSubEventSubs}) is being lazily initialised by competing threads.
     *
     * <p>Before the fix the missing re-read of {@code listOfSubEventSubs} inside the
     * {@code synchronized} block allowed multiple threads to each build their own copy of
     * the list and replace the shared reference, causing redundant—but not duplicate—
     * deliveries only if both copies were used.  The real risk is that under the Java
     * Memory Model a partially-constructed list could be observed by a thread that read
     * the field between the {@code synchronized} block's exit on Thread A and the
     * publication of the final reference on Thread B.  The fix adds the re-read so that
     * at most one thread ever populates the cache.</p>
     */
    @Test
    public void testConcurrentPostWithoutEventId_subscriberListCacheIsInitialisedOnce() throws InterruptedException {
        final int THREAD_COUNT = 20;
        final AtomicInteger deliveryCount = new AtomicInteger(0);

        Object subscriber = new Object() {
            @Subscribe
            public void onEvent(String event) {
                deliveryCount.incrementAndGet();
            }
        };
        eventBus.register(subscriber);

        // All threads race to call post() before the cache is warm.
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread t = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                eventBus.post("concurrent");
                done.countDown();
            });
            t.start();
        }

        ready.await();
        start.countDown();
        Assertions.assertTrue(done.await(10, TimeUnit.SECONDS));

        // Each of the THREAD_COUNT post() calls must deliver exactly once.
        Assertions.assertEquals(THREAD_COUNT, deliveryCount.get(), "Each concurrent post() must deliver to the subscriber exactly once");

        eventBus.unregister(subscriber);
    }

    /**
     * Verifies that concurrent calls to {@code post(String, Object)} (with eventId) never
     * deliver an event more than once per subscriber, even when the per-eventId cache
     * ({@code listOfEventIdSubMap}) is being lazily populated by multiple threads
     * simultaneously.
     *
     * <p>Before the fix the inner DCL block checked {@code registeredEventIdSubMap}
     * but never re-read the cache map ({@code listOfEventIdSubMap}) after acquiring the
     * lock, so two threads could each insert their own snapshot into the map — wasting
     * work but also violating the single-writer guarantee that makes the cache safe to
     * read outside the lock.</p>
     */
    @Test
    public void testConcurrentPostWithEventId_subscriberListCacheIsInitialisedOnce() throws InterruptedException {
        final int THREAD_COUNT = 20;
        final AtomicInteger deliveryCount = new AtomicInteger(0);

        Object subscriber = new Object() {
            @Subscribe(eventId = "concurrentId")
            public void onEvent(String event) {
                deliveryCount.incrementAndGet();
            }
        };
        eventBus.register(subscriber);

        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread t = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                eventBus.post("concurrentId", "concurrent");
                done.countDown();
            });
            t.start();
        }

        ready.await();
        start.countDown();
        Assertions.assertTrue(done.await(10, TimeUnit.SECONDS));

        Assertions.assertEquals(THREAD_COUNT, deliveryCount.get(), "Each concurrent post(eventId, event) must deliver to the subscriber exactly once");

        eventBus.unregister(subscriber);
    }

    /**
     * Verifies that a concurrent register+post sequence does not deliver stale data.
     * After a new subscriber is registered, concurrent posts must see that subscriber
     * (the cache is invalidated on register).
     */
    @Test
    public void testRegisterInvalidatesCacheForConcurrentPost() throws InterruptedException {
        final AtomicInteger deliveryCount = new AtomicInteger(0);

        // Warm the cache with a first subscriber.
        Object warmupSubscriber = new Object() {
            @Subscribe
            public void onEvent(String event) {
            }
        };
        eventBus.register(warmupSubscriber);
        eventBus.post("warmup"); // populates listOfSubEventSubs cache

        Object lateSubscriber = new Object() {
            @Subscribe
            public void onEvent(String event) {
                deliveryCount.incrementAndGet();
            }
        };
        eventBus.register(lateSubscriber); // must invalidate the cache

        // The very next post must reach lateSubscriber.
        eventBus.post("after register");

        Assertions.assertEquals(1, deliveryCount.get(), "Subscriber registered after cache warm-up must receive subsequent events");

        eventBus.unregister(warmupSubscriber);
        eventBus.unregister(lateSubscriber);
    }

    // ---- review fixes 2026-09-06 (a12 F-1): default executor runs on daemon threads ----

    @Test
    public void testDefaultExecutorDeliversOnDaemonNormalPriorityThread() throws InterruptedException {
        final EventBus bus = EventBus.create();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Thread> eventThread = new AtomicReference<>();
        final Object subscriber = new Object() {
            @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
            public void onEvent(String event) {
                eventThread.set(Thread.currentThread());
                latch.countDown();
            }
        };

        bus.register(subscriber);
        bus.post("daemon-check \u03bb");

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        final Thread worker = eventThread.get();
        assertNotNull(worker);
        assertNotEquals(Thread.currentThread(), worker);
        assertTrue(worker.isDaemon(), "default executor worker must be a daemon thread: " + worker.getName());
        assertEquals(Thread.NORM_PRIORITY, worker.getPriority());
        bus.unregister(subscriber);
    }

    @Test
    public void testDefaultBusAsyncDeliveryUsesDaemonThreadAndSyncDeliveryStaysOnCaller() throws InterruptedException {
        final EventBus bus = EventBus.getDefault();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Thread> asyncThread = new AtomicReference<>();
        final AtomicReference<Thread> syncThread = new AtomicReference<>();
        final Object subscriber = new Object() {
            @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR, eventId = "daemon-async")
            public void onAsync(String event) {
                asyncThread.set(Thread.currentThread());
                latch.countDown();
            }

            @Subscribe(eventId = "daemon-sync")
            public void onSync(String event) {
                syncThread.set(Thread.currentThread());
            }
        };

        try {
            bus.register(subscriber);
            bus.post("daemon-sync", "s");
            bus.post("daemon-async", "a");

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertTrue(asyncThread.get().isDaemon());
            // Regression guard: DEFAULT mode is unaffected by the executor change.
            assertEquals(Thread.currentThread(), syncThread.get());
        } finally {
            bus.unregister(subscriber);
        }
    }

    // ---- review fixes 2026-09-06 (a12 F-2): one process-wide shutdown hook over a weak registry ----

    private static int applicationShutdownHookCount() throws Exception {
        final Class<?> hooksClass = Class.forName("java.lang.ApplicationShutdownHooks");
        final java.lang.reflect.Field hooksField = hooksClass.getDeclaredField("hooks");
        hooksField.setAccessible(true);
        synchronized (hooksClass) {
            return ((Map<?, ?>) hooksField.get(null)).size();
        }
    }

    @Test
    public void testManyBusesOverOneExecutorServiceRegisterAtMostOneShutdownHook() throws Exception {
        final ExecutorService shared = Executors.newSingleThreadExecutor();
        final ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            // Installs the single process-wide hook if this JVM has not done so yet.
            EventBus.create("hook-warmup", shared);
            final int before = applicationShutdownHookCount();

            for (int i = 0; i < 25; i++) {
                EventBus.create("hook-" + i, shared);
            }

            assertEquals(before, applicationShutdownHookCount(), "one process-wide hook, not one hook per bus");

            // A second, distinct executor service joins the same hook.
            EventBus.create("hook-other", other);
            assertEquals(before, applicationShutdownHookCount());
        } finally {
            shared.shutdownNow();
            other.shutdownNow();
        }
    }

    @Test
    public void testPlainExecutorAndDefaultExecutorRegisterNoShutdownHookAndNullExecutorStillRejected() throws Exception {
        final int before = applicationShutdownHookCount();

        EventBus.create("plain-executor", Runnable::run);
        EventBus.create("plain-executor-2", (Executor) Runnable::run);
        EventBus.create();
        EventBus.create("default-executor");

        assertEquals(before, applicationShutdownHookCount());
        Assertions.assertThrows(IllegalArgumentException.class, () -> EventBus.create("null-executor", null));
    }

    private static WeakReference<ExecutorService> createBusOverUnreferencedExecutorAndDropIt() {
        final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        EventBus.create("discarded-" + System.nanoTime(), executor);
        return new WeakReference<>(executor);
    }

    @Test
    public void testDiscardedBusDoesNotPinItsExecutorService() throws Exception {
        final WeakReference<ExecutorService> ref = createBusOverUnreferencedExecutorAndDropIt();

        for (int i = 0; i < 100 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(50);
        }

        assertNull(ref.get(), "the executor of a discarded bus must become unreachable (no strong capture by a per-bus hook)");
    }

    // ---- review fixes 2026-09-06 (a12 F-3): async throttle/dedup decided at post time ----

    public static class AsyncThrottledSubscriber {
        final List<String> received = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch latch;

        AsyncThrottledSubscriber(final int expected) {
            latch = new CountDownLatch(expected);
        }

        @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR, intervalMillis = 200)
        public void onEvent(String event) {
            received.add(event);
            latch.countDown();
        }
    }

    public static class AsyncDedupSubscriber {
        final List<String> received = Collections.synchronizedList(new ArrayList<>());

        @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR, deduplicate = true)
        public void onEvent(String event) {
            received.add(event);
        }
    }

    public static class AsyncUnfilteredSubscriber {
        final List<String> received = Collections.synchronizedList(new ArrayList<>());

        @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR, intervalMillis = -1)
        public void onEvent(String event) {
            received.add(event);
        }
    }

    private static Executor countingInlineExecutor(final AtomicInteger tasks) {
        return task -> {
            tasks.incrementAndGet();
            task.run();
        };
    }

    @Test
    public void testAsyncIntervalIsMeasuredAtPostTimeNotAtExecutionTime() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final CountDownLatch gate = new CountDownLatch(1);
        try {
            // Block the only worker so all three posts are queued before any of them runs.
            executor.execute(() -> {
                try {
                    gate.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            final EventBus bus = EventBus.create("async-interval", executor);
            final AsyncThrottledSubscriber subscriber = new AsyncThrottledSubscriber(3);
            bus.register(subscriber);

            bus.post("e1");
            Thread.sleep(300);
            bus.post("e2");
            Thread.sleep(300);
            bus.post("e3");
            gate.countDown();

            assertTrue(subscriber.latch.await(5, TimeUnit.SECONDS),
                    "posts spaced 300 ms apart with intervalMillis=200 must all be delivered; got " + subscriber.received);
            assertEquals(List.of("e1", "e2", "e3"), subscriber.received);
        } finally {
            gate.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void testAsyncIntervalSuppressedEventIsNeverHandedToTheExecutor() {
        final AtomicInteger tasks = new AtomicInteger();
        final EventBus bus = EventBus.create("async-suppressed", countingInlineExecutor(tasks));
        final AsyncThrottledSubscriber subscriber = new AsyncThrottledSubscriber(1);
        bus.register(subscriber);

        bus.post("e1");
        bus.post("e2"); // within 200 ms of e1

        assertEquals(1, tasks.get(), "a throttled event must not be enqueued");
        assertEquals(List.of("e1"), subscriber.received);
    }

    @Test
    public void testAsyncDeduplicateIsDecidedAtPostTime() {
        final AtomicInteger tasks = new AtomicInteger();
        final EventBus bus = EventBus.create("async-dedup", countingInlineExecutor(tasks));
        final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
        bus.register(subscriber);

        bus.post("same");
        bus.post("same");

        assertEquals(1, tasks.get(), "a consecutive duplicate must not be enqueued");
        assertEquals(List.of("same"), subscriber.received);

        // Regression guard: a non-consecutive repeat is still delivered.
        bus.post("other");
        bus.post("same");
        assertEquals(3, tasks.get());
        assertEquals(List.of("same", "other", "same"), subscriber.received);

        // Unicode / empty events participate in equals() like any other.
        bus.post("\u03bb");
        bus.post("\u03bb");
        bus.post("");
        bus.post("");
        assertEquals(List.of("same", "other", "same", "\u03bb", ""), subscriber.received);
    }

    @Test
    public void testAsyncWithoutIntervalOrDeduplicateEnqueuesEveryPost() {
        final AtomicInteger tasks = new AtomicInteger();
        final EventBus bus = EventBus.create("async-unfiltered", countingInlineExecutor(tasks));
        final AsyncUnfilteredSubscriber subscriber = new AsyncUnfilteredSubscriber();
        bus.register(subscriber);

        bus.post("same");
        bus.post("same");
        bus.post("same");

        assertEquals(3, tasks.get());
        assertEquals(3, subscriber.received.size());
    }

    @Test
    public void testRejectedAsyncSubmissionReleasesItsThrottleReservation() {
        final AtomicInteger offered = new AtomicInteger();
        final Executor rejecting = task -> {
            offered.incrementAndGet();
            throw new RejectedExecutionException("expected rejection");
        };
        final EventBus bus = EventBus.create("async-rejected", rejecting);
        final AsyncThrottledSubscriber subscriber = new AsyncThrottledSubscriber(1);
        bus.register(subscriber);

        assertDoesNotThrow(() -> bus.post("e1"));
        assertDoesNotThrow(() -> bus.post("e2"));

        assertEquals(2, offered.get(), "e1 was never delivered, so the interval slot it reserved must be given back to e2");
        assertEquals(0, subscriber.received.size());
    }

    @Test
    public void testRejectedAsyncSubmissionReleasesItsDeduplicationReservation() {
        final AtomicInteger offered = new AtomicInteger();
        final Executor rejectingFirstTask = task -> {
            if (offered.getAndIncrement() == 0) {
                throw new RejectedExecutionException("expected rejection");
            }

            task.run();
        };
        final EventBus bus = EventBus.create("async-rejected-dedup", rejectingFirstTask);
        final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
        bus.register(subscriber);

        assertDoesNotThrow(() -> bus.post("same"));
        bus.post("same");

        assertEquals(2, offered.get(), "the rejected event was never delivered, so it must not become the previous event");
        assertEquals(List.of("same"), subscriber.received);
    }

    @Test
    public void testOverlappingRejectedSubmissionsRestorePreviousEventInReverseOrder() throws Exception {
        verifyOverlappingRejectedSubmissions(true);
    }

    @Test
    public void testOverlappingRejectedSubmissionsRestorePreviousEventInPostOrder() throws Exception {
        verifyOverlappingRejectedSubmissions(false);
    }

    private static void verifyOverlappingRejectedSubmissions(final boolean rejectNewerFirst) throws Exception {
        final AtomicInteger offered = new AtomicInteger();
        final CountDownLatch[] entered = { new CountDownLatch(1), new CountDownLatch(1) };
        final CountDownLatch[] release = { new CountDownLatch(1), new CountDownLatch(1) };
        final EventBus bus = EventBus.create("overlapping-rejections", task -> {
            final int index = offered.getAndIncrement() - 1;
            if (index >= 0 && index < 2) {
                entered[index].countDown();
                try {
                    assertTrue(release[index].await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                throw new RejectedExecutionException("expected rejection " + index);
            }
            task.run();
        });
        final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
        bus.register(subscriber);
        bus.post("accepted");

        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread[] posting = { new Thread(() -> postAndRecordFailure(bus, "first", failure)),
                new Thread(() -> postAndRecordFailure(bus, "second", failure)) };
        try {
            posting[0].start();
            assertTrue(entered[0].await(5, TimeUnit.SECONDS));
            posting[1].start();
            assertTrue(entered[1].await(5, TimeUnit.SECONDS));

            final int firstRejected = rejectNewerFirst ? 1 : 0;
            release[firstRejected].countDown();
            posting[firstRejected].join(5000);
            assertFalse(posting[firstRejected].isAlive());
            release[1 - firstRejected].countDown();
        } finally {
            release[0].countDown();
            release[1].countDown();
            posting[0].join(5000);
            posting[1].join(5000);
        }

        assertFalse(posting[0].isAlive());
        assertFalse(posting[1].isAlive());
        assertNull(failure.get());
        bus.post("accepted");
        assertEquals(3, offered.get(), "both rejected submissions must leave the last accepted event in place");
        bus.post("first");
        assertEquals(4, offered.get(), "a rejected event must be eligible for a later retry");
        assertEquals(List.of("accepted", "first"), subscriber.received);
    }

    private static void postAndRecordFailure(final EventBus bus, final String event, final AtomicReference<Throwable> failure) {
        try {
            bus.post(event);
        } catch (Throwable e) {
            failure.compareAndSet(null, e);
        }
    }

    @Test
    public void testNestedRejectedSubmissionsDoNotSuppressRetry() {
        final AtomicInteger offered = new AtomicInteger();
        final AtomicReference<EventBus> reference = new AtomicReference<>();
        final EventBus bus = EventBus.create("nested-rejections", task -> {
            final int index = offered.incrementAndGet();
            if (index == 1) {
                reference.get().post("second");
            }
            if (index <= 2) {
                throw new RejectedExecutionException("expected rejection " + index);
            }
            task.run();
        });
        reference.set(bus);
        final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
        bus.register(subscriber);

        bus.post("first");
        bus.post("first");

        assertEquals(3, offered.get());
        assertEquals(List.of("first"), subscriber.received);
    }

    @Test
    public void testRejectedOuterSubmissionPreservesSuccessfulNewerSubmission() {
        final AtomicInteger offered = new AtomicInteger();
        final AtomicReference<EventBus> reference = new AtomicReference<>();
        final EventBus bus = EventBus.create("successful-newer-submission", task -> {
            if (offered.incrementAndGet() == 1) {
                reference.get().post("second");
                throw new RejectedExecutionException("expected outer rejection");
            }
            task.run();
        });
        reference.set(bus);
        final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
        bus.register(subscriber);

        bus.post("first");
        bus.post("second");
        assertEquals(2, offered.get(), "rejecting an older submission must preserve newer successful delivery");
        bus.post("first");
        assertEquals(List.of("second", "first"), subscriber.received);
    }

    @Test
    public void testRejectedNestedSubmissionPreservesSuccessfulOuterSubmission() {
        final AtomicInteger offered = new AtomicInteger();
        final AtomicReference<EventBus> reference = new AtomicReference<>();
        final EventBus bus = EventBus.create("successful-outer-submission", task -> {
            final int index = offered.incrementAndGet();
            if (index == 1) {
                reference.get().post("second");
            } else if (index == 2) {
                throw new RejectedExecutionException("expected nested rejection");
            }
            task.run();
        });
        reference.set(bus);
        final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
        bus.register(subscriber);

        bus.post("first");
        bus.post("first");
        assertEquals(2, offered.get(), "rejecting a newer submission must preserve an older successful delivery");
        bus.post("second");
        assertEquals(List.of("first", "second"), subscriber.received);
    }

    @Test
    public void testInlineAttemptBeforeExecutorRejectionRemainsDeduplicated() {
        final AtomicInteger offered = new AtomicInteger();
        final EventBus bus = EventBus.create("attempted-before-rejection", task -> {
            offered.incrementAndGet();
            task.run();
            throw new RejectedExecutionException("reported after callback attempt");
        });
        final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
        bus.register(subscriber);

        bus.post("same");
        bus.post("same");

        assertEquals(1, offered.get(), "an attempted callback still counts when execute subsequently throws");
        assertEquals(List.of("same"), subscriber.received);
    }

    @Test
    public void testRuntimeExecutorFailureReleasesReservationAndPreservesThrowable() throws Exception {
        verifyExecutorFailureCleanup(new IllegalStateException("executor failed before accepting task"));
    }

    @Test
    public void testExecutorErrorReleasesReservationAndPreservesThrowable() throws Exception {
        verifyExecutorFailureCleanup(new AssertionError("executor failed before accepting task"));
    }

    @Test
    public void testSneakyCheckedExecutorFailureReleasesReservationAndPreservesThrowable() throws Exception {
        verifyExecutorFailureCleanup(new Exception("executor escaped its declared throws contract"));
    }

    @Test
    public void testRepeatedExecutorFailuresDoNotRetainReservationHistory() throws Exception {
        final java.lang.reflect.Field current = EventBus.SubIdentifier.class.getDeclaredField("currentReservation");
        current.setAccessible(true);

        for (final Throwable expected : new Throwable[] { new RejectedExecutionException("repeated rejection"),
                new IllegalStateException("repeated executor failure"), new AssertionError("repeated executor error"),
                new Exception("repeated sneaky checked failure") }) {
            final AtomicInteger offered = new AtomicInteger();
            final EventBus bus = EventBus.create("repeated-executor-failures", task -> {
                offered.incrementAndGet();
                throwExecutorFailure(expected);
            });
            final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
            final EventBus.SubIdentifier identifier = asyncIdentifier(subscriber);

            for (int i = 0; i < 128; i++) {
                Assertions.assertSame(expected, Assertions.assertThrows(Throwable.class, () -> bus.dispatch(identifier, "retry")));
                assertNull(current.get(identifier), "failed submissions must release their entire reservation history without a successful retry");
                assertNull(identifier.previousEvent, "a failed submission must not retain its event");
            }

            assertEquals(128, offered.get());
            assertTrue(subscriber.received.isEmpty());
        }
    }

    private static void verifyExecutorFailureCleanup(final Throwable expected) throws Exception {
        final AtomicInteger offered = new AtomicInteger();
        final EventBus bus = EventBus.create("executor-failure-cleanup", task -> {
            if (offered.getAndIncrement() == 0) {
                throwExecutorFailure(expected);
            }
            task.run();
        });
        final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
        final EventBus.SubIdentifier identifier = asyncIdentifier(subscriber);

        Assertions.assertSame(expected, Assertions.assertThrows(Throwable.class, () -> bus.dispatch(identifier, "retry")));
        bus.dispatch(identifier, "retry");

        assertEquals(2, offered.get(), "an executor failure before acceptance must not deduplicate the retry");
        assertEquals(List.of("retry"), subscriber.received);
    }

    @Test
    public void testAttemptedCallbackRemainsCommittedWhenExecutorThrowsAnyFailure() throws Exception {
        for (final Throwable expected : new Throwable[] { new IllegalStateException("after attempt"), new AssertionError("after attempt"),
                new Exception("after attempt") }) {
            final AtomicInteger offered = new AtomicInteger();
            final EventBus bus = EventBus.create("attempted-before-executor-failure", task -> {
                offered.incrementAndGet();
                task.run();
                throwExecutorFailure(expected);
            });
            final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
            final EventBus.SubIdentifier identifier = asyncIdentifier(subscriber);

            Assertions.assertSame(expected, Assertions.assertThrows(Throwable.class, () -> bus.dispatch(identifier, "same")));
            bus.dispatch(identifier, "same");

            assertEquals(1, offered.get());
            assertEquals(List.of("same"), subscriber.received);
        }
    }

    @Test
    public void testFailedExecutorSubmissionCannotUndoNewerAcceptedSubmission() throws Exception {
        for (final Throwable expected : new Throwable[] { new IllegalStateException("outer failure"), new AssertionError("outer failure") }) {
            final AtomicInteger offered = new AtomicInteger();
            final AtomicReference<EventBus> busReference = new AtomicReference<>();
            final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
            final EventBus.SubIdentifier identifier = asyncIdentifier(subscriber);
            final EventBus bus = EventBus.create("newer-accepted-before-failure", task -> {
                if (offered.getAndIncrement() == 0) {
                    busReference.get().dispatch(identifier, "newer");
                    throwExecutorFailure(expected);
                }
                task.run();
            });
            busReference.set(bus);

            Assertions.assertSame(expected, Assertions.assertThrows(Throwable.class, () -> bus.dispatch(identifier, "older")));
            bus.dispatch(identifier, "newer");
            assertEquals(2, offered.get());
            bus.dispatch(identifier, "older");
            assertEquals(List.of("newer", "older"), subscriber.received);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwExecutorFailure(final Throwable failure) throws E {
        throw (E) failure;
    }

    private static EventBus.SubIdentifier asyncIdentifier(final Object subscriber) throws Exception {
        final EventBus.SubIdentifier prototype = new EventBus.SubIdentifier(subscriber.getClass().getMethod("onEvent", String.class));
        return new EventBus.SubIdentifier(prototype, subscriber, null, ThreadMode.THREAD_POOL_EXECUTOR);
    }

    @Test
    public void testCommitAndReleaseLeaveSharedReservationConstantsUnchanged() throws Exception {
        final EventBus.SubIdentifier identifier = asyncIdentifier(new AsyncDedupSubscriber());
        final Class<?> reservationClass = Class.forName(EventBus.class.getName() + "$Reservation");
        final Method commit = EventBus.SubIdentifier.class.getDeclaredMethod("commit", reservationClass);
        final Method release = EventBus.SubIdentifier.class.getDeclaredMethod("release", reservationClass);
        final java.lang.reflect.Field committed = reservationClass.getDeclaredField("committed");
        final java.lang.reflect.Field rejected = reservationClass.getDeclaredField("rejected");
        committed.setAccessible(true);
        rejected.setAccessible(true);

        for (final String name : List.of("INTERVAL", "DUPLICATE", "UNFILTERED")) {
            final java.lang.reflect.Field field = reservationClass.getDeclaredField(name);
            field.setAccessible(true);
            final Object reservation = field.get(null);
            final boolean wasCommitted = committed.getBoolean(reservation);
            final boolean wasRejected = rejected.getBoolean(reservation);
            try {
                commit.invoke(identifier, reservation);
                assertEquals(wasCommitted, committed.getBoolean(reservation), name + " is shared by all subscribers");
                release.invoke(identifier, reservation);
                assertEquals(wasRejected, rejected.getBoolean(reservation), name + " is shared by all subscribers");
            } finally {
                // Keep a failing baseline run from contaminating other tests through a shared singleton.
                committed.setBoolean(reservation, wasCommitted);
                rejected.setBoolean(reservation, wasRejected);
            }
        }
    }

    @Test
    public void testAcceptedQueuedTasksDiscardHistoryAndDoNotWaitForFilterMonitor() throws Exception {
        final List<Runnable> queued = new ArrayList<>();
        final EventBus bus = EventBus.create("accepted-queued-tasks", queued::add);
        final AsyncDedupSubscriber subscriber = new AsyncDedupSubscriber();
        final EventBus.SubIdentifier identifier = asyncIdentifier(subscriber);
        final java.lang.reflect.Field current = EventBus.SubIdentifier.class.getDeclaredField("currentReservation");
        current.setAccessible(true);
        final java.lang.reflect.Field previous = current.getType().getDeclaredField("previous");
        previous.setAccessible(true);

        for (final String event : List.of("first", "second", "third")) {
            bus.dispatch(identifier, event);
            assertNull(previous.get(current.get(identifier)), "successful submission must not retain older event history");
        }
        bus.dispatch(identifier, "third");
        assertEquals(3, queued.size(), "successful submission must commit deduplication before task execution");
        assertTrue(subscriber.received.isEmpty());

        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch completed = new CountDownLatch(1);
        final Thread worker = new Thread(() -> {
            try {
                queued.forEach(Runnable::run);
            } catch (Throwable e) {
                failure.set(e);
            } finally {
                completed.countDown();
            }
        }, "committed-eventbus-tasks");
        worker.setDaemon(true);
        try {
            synchronized (identifier) {
                worker.start();
                assertTrue(completed.await(5, TimeUnit.SECONDS), "permanently committed tasks must not reacquire the filter monitor");
            }
        } finally {
            worker.join(5000);
        }

        assertFalse(worker.isAlive());
        assertNull(failure.get());
        assertEquals(List.of("first", "second", "third"), subscriber.received);
    }

    @Test
    public void testOverlappingIntervalRejectionsRestoreAcceptedTimestampInReverseOrder() throws Exception {
        verifyOverlappingIntervalRejections(true);
    }

    @Test
    public void testOverlappingIntervalRejectionsRestoreAcceptedTimestampInPostOrder() throws Exception {
        verifyOverlappingIntervalRejections(false);
    }

    private static void verifyOverlappingIntervalRejections(final boolean rejectNewerFirst) throws Exception {
        final AtomicInteger offered = new AtomicInteger();
        final CountDownLatch[] entered = { new CountDownLatch(1), new CountDownLatch(1) };
        final CountDownLatch[] release = { new CountDownLatch(1), new CountDownLatch(1) };
        final RejectedExecutionException[] rejections = { new RejectedExecutionException("first rejection"),
                new RejectedExecutionException("second rejection") };
        final EventBus bus = EventBus.create("overlapping-interval-rejections", task -> {
            final int index = offered.getAndIncrement() - 1;
            if (index >= 0 && index < 2) {
                entered[index].countDown();
                try {
                    assertTrue(release[index].await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                throw rejections[index];
            }
            task.run();
        });
        final AsyncThrottledSubscriber subscriber = new AsyncThrottledSubscriber(2);
        final EventBus.SubIdentifier identifier = asyncIdentifier(subscriber);
        bus.dispatch(identifier, "accepted");

        // Use a deterministic past accepted time, avoiding real sleeps between throttled submissions.
        final long priorAcceptedTime = System.nanoTime() - TimeUnit.SECONDS.toNanos(10);
        final Method record = EventBus.SubIdentifier.class.getDeclaredMethod("recordReservation", long.class, Object.class);
        record.setAccessible(true);
        final Method commit = EventBus.SubIdentifier.class.getDeclaredMethod("commit", record.getReturnType());
        synchronized (identifier) {
            commit.invoke(identifier, record.invoke(identifier, priorAcceptedTime, "accepted"));
        }

        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread[] posting = new Thread[2];
        for (int i = 0; i < posting.length; i++) {
            final int index = i;
            posting[i] = new Thread(() -> {
                try {
                    Assertions.assertSame(rejections[index],
                            Assertions.assertThrows(RejectedExecutionException.class, () -> bus.dispatch(identifier, "rejected-" + index)));
                } catch (Throwable e) {
                    failure.compareAndSet(null, e);
                }
            }, "interval-rejection-" + index);
            posting[i].setDaemon(true);
        }
        try {
            posting[0].start();
            assertTrue(entered[0].await(5, TimeUnit.SECONDS));
            synchronized (identifier) {
                identifier.recordPostTime(priorAcceptedTime);
            }
            posting[1].start();
            assertTrue(entered[1].await(5, TimeUnit.SECONDS));
            final int firstRejected = rejectNewerFirst ? 1 : 0;
            release[firstRejected].countDown();
            posting[firstRejected].join(5000);
            assertFalse(posting[firstRejected].isAlive());
            release[1 - firstRejected].countDown();
        } finally {
            release[0].countDown();
            release[1].countDown();
            posting[0].join(5000);
            posting[1].join(5000);
        }

        assertFalse(posting[0].isAlive());
        assertFalse(posting[1].isAlive());
        assertNull(failure.get());
        assertEquals(priorAcceptedTime, identifier.lastPostTimeNanos, "both rejections must restore the prior accepted timestamp exactly");
        assertTrue(identifier.hasPosted, "rejection must not erase the earlier accepted delivery");
        assertTrue(identifier.isWithinPostInterval(priorAcceptedTime + TimeUnit.MILLISECONDS.toNanos(199)));
        assertFalse(identifier.isWithinPostInterval(priorAcceptedTime + TimeUnit.MILLISECONDS.toNanos(200)));
        bus.dispatch(identifier, "retry");
        assertEquals(4, offered.get(), "the rejected submissions must not throttle a later eligible retry");
        assertEquals(List.of("accepted", "retry"), subscriber.received);
    }

    // ---- review fixes 2026-09-06 (a12 F-4): documented registration edge cases ----

    public static class NamedObjectSubscriber implements Subscriber<Object> {
        final List<Object> received = new ArrayList<>();

        @Override
        public void on(Object event) {
            received.add(event);
        }
    }

    public static class PrivateStaticAnnotatedSubscriber {
        @Subscribe
        private static void onEvent(String event) {
        }
    }

    public static class PrivateOnlyAnnotatedSubscriber {
        @Subscribe
        private void onEvent(String event) {
        }
    }

    @Test
    public void testNamedSubscriberOfObjectRequiresEventIdLikeALambda() {
        final NamedObjectSubscriber subscriber = new NamedObjectSubscriber();

        Assertions.assertThrows(IllegalStateException.class, () -> eventBus.register(subscriber));

        eventBus.register(subscriber, "named-object");
        eventBus.post("named-object", "hello");
        eventBus.post("hello-without-id");

        assertEquals(List.of("hello"), subscriber.received);
        eventBus.unregister(subscriber);
    }

    @Test
    public void testPrivateStaticAnnotatedMethodIsRejectedNotIgnored() {
        final RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> eventBus.register(new PrivateStaticAnnotatedSubscriber()));
        assertTrue(e.getMessage().contains("must not be static"), e.getMessage());
    }

    @Test
    public void testPrivateAnnotatedMethodAloneIsIgnoredSoNoSubscriberMethodIsFound() {
        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> eventBus.register(new PrivateOnlyAnnotatedSubscriber()));
        assertTrue(e.getMessage().startsWith("No subscriber method found"), e.getMessage());
    }

    @Test
    public void testRegisterPostStickyAndDeduplicateSmoke() {
        final Object strSubscriber_1 = new Subscriber<String>() {
            @Override
            public void on(String event) {
                // smoke: registration + post
            }
        };

        final Object anySubscriber_2 = new Object() {
            @Subscribe(threadMode = ThreadMode.DEFAULT, intervalMillis = 1000)
            public void anyMethod(Object event) {
            }
        };

        final Object anySubscriber_3 = new Object() {
            @Subscribe(threadMode = ThreadMode.DEFAULT, sticky = true, deduplicate = true)
            public void anyMethod(Object event) {
            }
        };

        final EventBus bus = EventBus.getDefault();
        bus.register(strSubscriber_1);
        bus.register(strSubscriber_1);
        bus.register(anySubscriber_2, "eventId_2");
        bus.post("abc");
        bus.postSticky("sticky");
        bus.post("eventId_2", "abc");
        bus.post(123);
        bus.post("eventId_2", 123);
        bus.register(anySubscriber_3);
        bus.post("sticky1");
        bus.post("sticky");
        bus.post("sticky");
        assertNotNull(bus);
    }

}
