package com.landawn.abacus.eventbus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ThreadMode;

@Tag("new-test")
public class EventBus100Test extends TestBase {

    private EventBus eventBus;

    @BeforeEach
    public void setUp() {
        eventBus = new EventBus();
    }

    @Test
    public void testConstructorWithIdentifier() {
        EventBus bus = new EventBus("testBus");
        Assertions.assertEquals("testBus", bus.identifier());
    }

    @Test
    public void testConstructorWithIdentifierAndExecutor() {
        EventBus bus = new EventBus("testBus", null);
        Assertions.assertEquals("testBus", bus.identifier());
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
    public void testIdentifier() {
        EventBus bus = new EventBus("myIdentifier");
        Assertions.assertEquals("myIdentifier", bus.identifier());
    }

    @Test
    public void testGetSubscribers() {
        TestSubscriber subscriber1 = new TestSubscriber();
        TestSubscriber subscriber2 = new TestSubscriber();

        eventBus.register(subscriber1);
        eventBus.register(subscriber2);

        List<Object> subscribers = eventBus.getSubscribers(String.class);
        Assertions.assertEquals(2, subscribers.size());
        Assertions.assertTrue(subscribers.contains(subscriber1));
        Assertions.assertTrue(subscribers.contains(subscriber2));
    }

    @Test
    public void testGetSubscribersWithEventId() {
        TestSubscriber subscriber1 = new TestSubscriber();
        TestSubscriber subscriber2 = new TestSubscriber();

        eventBus.register(subscriber1, "event1");
        eventBus.register(subscriber2, "event2");

        List<Object> subscribers = eventBus.getSubscribers("event1", String.class);
        Assertions.assertEquals(1, subscribers.size());
        Assertions.assertTrue(subscribers.contains(subscriber1));

        subscribers = eventBus.getSubscribers("event2", String.class);
        Assertions.assertEquals(1, subscribers.size());
        Assertions.assertTrue(subscribers.contains(subscriber2));
    }

    @Test
    public void testGetAllSubscribers() {
        TestSubscriber subscriber1 = new TestSubscriber();
        TestSubscriber subscriber2 = new TestSubscriber();

        Assertions.assertEquals(0, eventBus.getAllSubscribers().size());

        eventBus.register(subscriber1);
        eventBus.register(subscriber2);

        List<Object> allSubscribers = eventBus.getAllSubscribers();
        Assertions.assertEquals(2, allSubscribers.size());
        Assertions.assertTrue(allSubscribers.contains(subscriber1));
        Assertions.assertTrue(allSubscribers.contains(subscriber2));
    }

    @Test
    public void testRegister() {
        TestSubscriber subscriber = new TestSubscriber();
        EventBus result = eventBus.register(subscriber);

        Assertions.assertSame(eventBus, result);
        Assertions.assertEquals(1, eventBus.getAllSubscribers().size());
    }

    @Test
    public void testRegisterWithEventId() {
        TestSubscriber subscriber = new TestSubscriber();
        EventBus result = eventBus.register(subscriber, "testEvent");

        Assertions.assertSame(eventBus, result);
        Assertions.assertEquals(1, eventBus.getSubscribers("testEvent", String.class).size());
    }

    @Test
    public void testRegisterWithThreadMode() {
        TestSubscriber subscriber = new TestSubscriber();
        EventBus result = eventBus.register(subscriber, ThreadMode.DEFAULT);

        Assertions.assertSame(eventBus, result);
        Assertions.assertEquals(1, eventBus.getAllSubscribers().size());
    }

    @Test
    public void testRegisterWithEventIdAndThreadMode() {
        TestSubscriber subscriber = new TestSubscriber();
        EventBus result = eventBus.register(subscriber, "testEvent", ThreadMode.DEFAULT);

        Assertions.assertSame(eventBus, result);
        Assertions.assertEquals(1, eventBus.getSubscribers("testEvent", String.class).size());
    }

    @Test
    public void testRegisterLambdaSubscriber() {
        AtomicReference<String> received = new AtomicReference<>();
        Subscriber<String> subscriber = event -> received.set(event);

        eventBus.register(subscriber, "lambdaEvent");
        eventBus.post("lambdaEvent", "Hello Lambda");

        Assertions.assertEquals("Hello Lambda", received.get());
    }

    @Test
    public void testRegisterLambdaSubscriberWithThreadMode() {
        AtomicReference<String> received = new AtomicReference<>();
        Subscriber<String> subscriber = event -> received.set(event);

        eventBus.register(subscriber, "lambdaEvent", ThreadMode.DEFAULT);
        eventBus.post("lambdaEvent", "Hello Lambda");

        Assertions.assertEquals("Hello Lambda", received.get());
    }

    @Test
    public void testRegisterThrowsExceptionForNoSubscriberMethods() {
        Object noMethodSubscriber = new Object();

        Assertions.assertThrows(RuntimeException.class, () -> {
            eventBus.register(noMethodSubscriber);
        });
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
    public void testUnregister() {
        TestSubscriber subscriber = new TestSubscriber();
        eventBus.register(subscriber);

        Assertions.assertEquals(1, eventBus.getAllSubscribers().size());

        EventBus result = eventBus.unregister(subscriber);
        Assertions.assertSame(eventBus, result);
        Assertions.assertEquals(0, eventBus.getAllSubscribers().size());
    }

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
    public void testPostStickyWithEventId() {
        TestStickySubscriber subscriber = new TestStickySubscriber();

        eventBus.postSticky("stickyEvent", "Sticky Message");
        eventBus.register(subscriber, "stickyEvent");

        Assertions.assertEquals(1, subscriber.receivedEvents.size());
        Assertions.assertEquals("Sticky Message", subscriber.receivedEvents.get(0));
    }

    @Test
    public void testRemoveStickyEvent() {
        String event = "Sticky Event";
        eventBus.postSticky(event);

        boolean removed = eventBus.removeStickyEvent(event);
        Assertions.assertTrue(removed);

        removed = eventBus.removeStickyEvent(event);
        Assertions.assertFalse(removed);
    }

    @Test
    public void testRemoveStickyEventWithEventId() {
        String event = "Sticky Event";
        eventBus.postSticky("eventId", event);

        boolean removed = eventBus.removeStickyEvent(event, "eventId");
        Assertions.assertTrue(removed);

        removed = eventBus.removeStickyEvent(event, "wrongId");
        Assertions.assertFalse(removed);
    }

    @Test
    public void testRemoveStickyEvents() {
        eventBus.postSticky("Event 1");
        eventBus.postSticky("Event 2");
        eventBus.postSticky(123);

        boolean removed = eventBus.removeStickyEvents(String.class);
        Assertions.assertTrue(removed);

        List<Object> remainingStrings = eventBus.getStickyEvents(String.class);
        Assertions.assertEquals(0, remainingStrings.size());

        List<Object> remainingIntegers = eventBus.getStickyEvents(Integer.class);
        Assertions.assertEquals(1, remainingIntegers.size());
    }

    @Test
    public void testRemoveStickyEventsWithEventId() {
        eventBus.postSticky("id1", "Event 1");
        eventBus.postSticky("id2", "Event 2");
        eventBus.postSticky("id1", 123);

        boolean removed = eventBus.removeStickyEvents("id1", String.class);
        Assertions.assertTrue(removed);

        List<Object> remaining = eventBus.getStickyEvents("id1", String.class);
        Assertions.assertEquals(0, remaining.size());

        remaining = eventBus.getStickyEvents("id2", String.class);
        Assertions.assertEquals(1, remaining.size());
    }

    @Test
    public void testRemoveStickyEventsWithEventId_1() {
        eventBus.postSticky("id1", "Event 1");
        eventBus.postSticky("id2", "Event 2");
        eventBus.postSticky("id1", "Event 1");

        boolean removed = eventBus.removeStickyEvents("id1", String.class);
        Assertions.assertTrue(removed);

        List<Object> remaining = eventBus.getStickyEvents("id1", String.class);
        Assertions.assertEquals(0, remaining.size());

        remaining = eventBus.getStickyEvents("id2", String.class);
        Assertions.assertEquals(1, remaining.size());
    }

    @Test
    public void testRemoveAllStickyEvents() {
        eventBus.postSticky("Event 1");
        eventBus.postSticky("Event 2");
        eventBus.postSticky(123);

        eventBus.removeAllStickyEvents();

        Assertions.assertEquals(0, eventBus.getStickyEvents(String.class).size());
        Assertions.assertEquals(0, eventBus.getStickyEvents(Integer.class).size());
    }

    @Test
    public void testGetStickyEvents() {
        eventBus.postSticky("Event 1");
        eventBus.postSticky("Event 2");
        eventBus.postSticky(123);

        List<Object> stringEvents = eventBus.getStickyEvents(String.class);
        Assertions.assertEquals(2, stringEvents.size());

        List<Object> integerEvents = eventBus.getStickyEvents(Integer.class);
        Assertions.assertEquals(1, integerEvents.size());
    }

    @Test
    public void testGetStickyEventsWithEventId() {
        eventBus.postSticky("id1", "Event 1");
        eventBus.postSticky("id2", "Event 2");
        eventBus.postSticky("id1", "Event 3");

        List<Object> events = eventBus.getStickyEvents("id1", String.class);
        Assertions.assertEquals(2, events.size());

        events = eventBus.getStickyEvents("id2", String.class);
        Assertions.assertEquals(1, events.size());
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
    public void testEventInterval() throws InterruptedException {
        AtomicInteger eventCount = new AtomicInteger(0);

        Object subscriber = new Object() {
            @Subscribe(interval = 100)
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
    public void testIsSupportedThreadMode() {
        EventBus bus = new EventBus();

        Assertions.assertTrue(bus.isSupportedThreadMode(null));
        Assertions.assertTrue(bus.isSupportedThreadMode(ThreadMode.DEFAULT));
        Assertions.assertTrue(bus.isSupportedThreadMode(ThreadMode.THREAD_POOL_EXECUTOR));
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

    public static class MultiMethodSubscriber {
        int stringCount = 0;
        int integerCount = 0;
        int doubleCount = 0;

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

    public static class HierarchySubscriber {
        int baseEventCount = 0;
        int subEventCount = 0;
        int subSubEventCount = 0;

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

    public static class BaseEvent {
    }

    public static class SubEvent extends BaseEvent {
    }

    public static class SubSubEvent extends SubEvent {
    }
}
