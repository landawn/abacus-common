package com.landawn.abacus.eventbus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ThreadMode;

public class SubscribeTest extends TestBase {

    static class TestHandler {
        @Subscribe
        public void handleDefault(String event) {
        }

        @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
        public void handleAsync(String event) {
        }

        @Subscribe(eventId = "myEvents")
        public void handleWithEventId(String event) {
        }

        @Subscribe(sticky = true)
        public void handleSticky(String event) {
        }

        @Subscribe(strictEventType = true)
        public void handleStrict(String event) {
        }

        @Subscribe(intervalMillis = 1000)
        public void handleWithInterval(String event) {
        }

        @Subscribe(deduplicate = true)
        public void handleDeduplicate(String event) {
        }

        @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR, eventId = "testId", sticky = true, strictEventType = true, intervalMillis = 5000, deduplicate = true)
        public void handleAll(String event) {
        }
    }

    @Test
    public void testDefaultValues() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleDefault", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertNotNull(annotation);
        assertEquals(ThreadMode.DEFAULT, annotation.threadMode());
        assertFalse(annotation.strictEventType());
        assertFalse(annotation.sticky());
        assertEquals("", annotation.eventId());
        assertEquals(0, annotation.intervalMillis());
        assertFalse(annotation.deduplicate());
    }

    @Test
    public void testThreadMode() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleAsync", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertEquals(ThreadMode.THREAD_POOL_EXECUTOR, annotation.threadMode());
    }

    @Test
    public void testAllAttributes() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleAll", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertNotNull(annotation);
        assertEquals(ThreadMode.THREAD_POOL_EXECUTOR, annotation.threadMode());
        assertEquals("testId", annotation.eventId());
        assertTrue(annotation.sticky());
        assertTrue(annotation.strictEventType());
        assertEquals(5000, annotation.intervalMillis());
        assertTrue(annotation.deduplicate());
    }

    @Test
    public void testCustomValues() throws NoSuchMethodException {
        class TestClass {
            @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR, strictEventType = true, sticky = true, eventId = "testEvent", intervalMillis = 1000, deduplicate = true)
            public void onEvent(String event) {
            }
        }

        Method method = TestClass.class.getMethod("onEvent", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);

        Assertions.assertNotNull(annotation);
        Assertions.assertEquals(ThreadMode.THREAD_POOL_EXECUTOR, annotation.threadMode());
        Assertions.assertTrue(annotation.strictEventType());
        Assertions.assertTrue(annotation.sticky());
        Assertions.assertEquals("testEvent", annotation.eventId());
        Assertions.assertEquals(1000, annotation.intervalMillis());
        Assertions.assertTrue(annotation.deduplicate());
    }

    @Test
    public void testThreadModeAttribute() {
        EventBus eventBus = EventBus.create();
        AtomicReference<Thread> executionThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        class TestClass {
            @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
            public void onEvent(String event) {
                executionThread.set(Thread.currentThread());
                latch.countDown();
            }
        }

        TestClass subscriber = new TestClass();
        eventBus.register(subscriber);
        eventBus.post("test");

        try {
            Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
            Assertions.assertNotEquals(Thread.currentThread(), executionThread.get());
        } catch (InterruptedException e) {
            Assertions.fail("Interrupted while waiting for event");
        }
    }

    @Test
    public void testStrictEventTypeAttribute() {
        EventBus eventBus = EventBus.create();
        AtomicInteger normalCount = new AtomicInteger(0);
        AtomicInteger strictCount = new AtomicInteger(0);

        class TestClass {
            @Subscribe
            public void onNormalEvent(CharSequence event) {
                normalCount.incrementAndGet();
            }

            @Subscribe(strictEventType = true)
            public void onStrictEvent(CharSequence event) {
                strictCount.incrementAndGet();
            }
        }

        TestClass subscriber = new TestClass();
        eventBus.register(subscriber);

        eventBus.post("String implements CharSequence");
        eventBus.post(new StringBuilder("StringBuilder implements CharSequence"));

        Assertions.assertEquals(2, normalCount.get());
        Assertions.assertEquals(0, strictCount.get());
    }

    @Test
    public void testStrictEventType() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleStrict", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertTrue(annotation.strictEventType());
    }

    @Test
    public void testStickyAttribute() {
        EventBus eventBus = EventBus.create();
        AtomicReference<String> receivedEvent = new AtomicReference<>();

        eventBus.postSticky("Sticky Event");

        class TestClass {
            @Subscribe(sticky = true)
            public void onEvent(String event) {
                receivedEvent.set(event);
            }
        }

        TestClass subscriber = new TestClass();
        eventBus.register(subscriber);

        Assertions.assertEquals("Sticky Event", receivedEvent.get());
    }

    @Test
    public void testSticky() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleSticky", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertTrue(annotation.sticky());
    }

    @Test
    public void testEventIdAttribute() {
        EventBus eventBus = EventBus.create();
        AtomicInteger event1Count = new AtomicInteger(0);
        AtomicInteger event2Count = new AtomicInteger(0);

        class TestClass {
            @Subscribe(eventId = "event1")
            public void onEvent1(String event) {
                event1Count.incrementAndGet();
            }

            @Subscribe(eventId = "event2")
            public void onEvent2(String event) {
                event2Count.incrementAndGet();
            }
        }

        TestClass subscriber = new TestClass();
        eventBus.register(subscriber);

        eventBus.post("event1", "Message 1");
        eventBus.post("event2", "Message 2");
        eventBus.post("No event ID");

        Assertions.assertEquals(1, event1Count.get());
        Assertions.assertEquals(1, event2Count.get());
    }

    @Test
    public void testEventId() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleWithEventId", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertEquals("myEvents", annotation.eventId());
    }

    @Test
    public void testInterval() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleWithInterval", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertEquals(1000, annotation.intervalMillis());
    }

    @Test
    public void testDeduplicateAttribute() {
        EventBus eventBus = EventBus.create();
        AtomicInteger eventCount = new AtomicInteger(0);
        AtomicReference<String> lastEvent = new AtomicReference<>();

        class TestClass {
            @Subscribe(deduplicate = true)
            public void onEvent(String event) {
                eventCount.incrementAndGet();
                lastEvent.set(event);
            }
        }

        TestClass subscriber = new TestClass();
        eventBus.register(subscriber);

        eventBus.post("Event A");
        eventBus.post("Event A");
        eventBus.post("Event B");
        eventBus.post("Event B");
        eventBus.post("Event A");

        Assertions.assertEquals(3, eventCount.get());
        Assertions.assertEquals("Event A", lastEvent.get());
    }

    @Test
    public void testDeduplicate() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleDeduplicate", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertTrue(annotation.deduplicate());
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleDefault", String.class);
        assertTrue(method.isAnnotationPresent(Subscribe.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = Subscribe.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = Subscribe.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, target.value());
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(Subscribe.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleDefault", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertNotNull(annotation);
        assertEquals(Subscribe.class, annotation.annotationType());
    }

    @Test
    public void testIntervalAttribute() throws InterruptedException {
        EventBus eventBus = EventBus.create();
        AtomicInteger eventCount = new AtomicInteger(0);

        class TestClass {
            @Subscribe(intervalMillis = 80)
            public void onEvent(String event) {
                eventCount.incrementAndGet();
            }
        }

        TestClass subscriber = new TestClass();
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
    public void testMultipleAttributesCombined() throws InterruptedException {
        EventBus eventBus = EventBus.create();
        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        class TestClass {
            @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR, eventId = "combined", intervalMillis = 50, deduplicate = true)
            public void onEvent(String event) {
                eventCount.incrementAndGet();
                latch.countDown();
            }
        }

        TestClass subscriber = new TestClass();
        eventBus.register(subscriber);

        eventBus.post("combined", "Event 1");

        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(1, eventCount.get());

        eventBus.post("combined", "Event 1");
        Thread.sleep(10);
        Assertions.assertEquals(1, eventCount.get());

        eventBus.post("combined", "Event 2");
        Thread.sleep(10);
        Assertions.assertEquals(1, eventCount.get());

        Thread.sleep(60);

        eventBus.post("combined", "Event 3");
        Thread.sleep(100);
        Assertions.assertEquals(2, eventCount.get());
    }

    @Test
    public void testMethodRequirements() {
        EventBus eventBus = EventBus.create();

        class InvalidSubscriber {
            @Subscribe
            public void noParameters() {
            }

            @Subscribe
            public void twoParameters(String a, String b) {
            }

            @Subscribe
            private void privateMethod(String event) {
            }
        }

        Assertions.assertThrows(RuntimeException.class, () -> {
            eventBus.register(new InvalidSubscriber());
        });
    }

    @Test
    public void testAnnotationOnValidMethod() {
        EventBus eventBus = EventBus.create();
        AtomicReference<String> received = new AtomicReference<>();

        class ValidSubscriber {
            @Subscribe
            public void validMethod(String event) {
                received.set(event);
            }
        }

        ValidSubscriber subscriber = new ValidSubscriber();
        eventBus.register(subscriber);
        eventBus.post("Test Event");

        Assertions.assertEquals("Test Event", received.get());
    }

}
