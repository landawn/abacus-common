package com.landawn.abacus.eventbus;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ThreadMode;

@Tag("new-test")
public class Subscribe100Test extends TestBase {

    @Test
    public void testDefaultValues() throws NoSuchMethodException {
        class TestClass {
            @Subscribe
            public void onEvent(String event) {
            }
        }

        Method method = TestClass.class.getMethod("onEvent", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);

        Assertions.assertNotNull(annotation);
        Assertions.assertEquals(ThreadMode.DEFAULT, annotation.threadMode());
        Assertions.assertFalse(annotation.strictEventType());
        Assertions.assertFalse(annotation.sticky());
        Assertions.assertEquals("", annotation.eventId());
        Assertions.assertEquals(0, annotation.interval());
        Assertions.assertFalse(annotation.deduplicate());
    }

    @Test
    public void testCustomValues() throws NoSuchMethodException {
        class TestClass {
            @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR, strictEventType = true, sticky = true, eventId = "testEvent", interval = 1000, deduplicate = true)
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
        Assertions.assertEquals(1000, annotation.interval());
        Assertions.assertTrue(annotation.deduplicate());
    }

    @Test
    public void testThreadModeAttribute() {
        EventBus eventBus = new EventBus();
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
        EventBus eventBus = new EventBus();
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
    public void testStickyAttribute() {
        EventBus eventBus = new EventBus();
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
    public void testEventIdAttribute() {
        EventBus eventBus = new EventBus();
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
    public void testIntervalAttribute() throws InterruptedException {
        EventBus eventBus = new EventBus();
        AtomicInteger eventCount = new AtomicInteger(0);

        class TestClass {
            @Subscribe(interval = 80)
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
    public void testDeduplicateAttribute() {
        EventBus eventBus = new EventBus();
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
    public void testMultipleAttributesCombined() throws InterruptedException {
        EventBus eventBus = new EventBus();
        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        class TestClass {
            @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR, eventId = "combined", interval = 50, deduplicate = true)
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
        EventBus eventBus = new EventBus();

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
        EventBus eventBus = new EventBus();
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
