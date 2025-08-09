package com.landawn.abacus.eventbus;

import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ThreadMode;

public class Subscriber100Test extends TestBase {

    private EventBus eventBus;

    @BeforeEach
    public void setUp() {
        eventBus = new EventBus();
    }

    @Test
    public void testOnMethod() {
        AtomicReference<String> receivedEvent = new AtomicReference<>();

        Subscriber<String> subscriber = new Subscriber<String>() {
            @Override
            public void on(String event) {
                receivedEvent.set(event);
            }
        };

        // Must register with event ID for lambda/anonymous subscribers
        eventBus.register(subscriber, "testEvent");
        eventBus.post("testEvent", "Hello World");

        Assertions.assertEquals("Hello World", receivedEvent.get());
    }

    @Test
    public void testLambdaImplementation() {
        AtomicReference<String> receivedEvent = new AtomicReference<>();

        Subscriber<String> subscriber = event -> receivedEvent.set(event);

        eventBus.register(subscriber, "lambdaEvent");
        eventBus.post("lambdaEvent", "Lambda Test");

        Assertions.assertEquals("Lambda Test", receivedEvent.get());
    }

    @Test
    public void testMethodReference() {
        List<String> events = new ArrayList<>();

        Subscriber<String> subscriber = events::add;

        eventBus.register(subscriber, "methodRef");
        eventBus.post("methodRef", "Method Reference Test");

        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals("Method Reference Test", events.get(0));
    }

    @Test
    public void testGenericTypes() {
        AtomicReference<Integer> intReceived = new AtomicReference<>();
        AtomicReference<List<String>> listReceived = new AtomicReference<>();

        Subscriber<Integer> intSubscriber = intReceived::set;
        Subscriber<List<String>> listSubscriber = listReceived::set;

        eventBus.register(intSubscriber, "integers");
        eventBus.register(listSubscriber, "lists");

        eventBus.post("integers", 42);
        List<String> testList = List.of("a", "b", "c");
        eventBus.post("lists", testList);

        Assertions.assertEquals(42, intReceived.get());
        Assertions.assertEquals(testList, listReceived.get());
    }

    @Test
    public void testMultipleSubscribers() {
        List<String> subscriber1Events = new ArrayList<>();
        List<String> subscriber2Events = new ArrayList<>();

        Subscriber<String> subscriber1 = subscriber1Events::add;
        Subscriber<String> subscriber2 = subscriber2Events::add;

        eventBus.register(subscriber1, "shared");
        eventBus.register(subscriber2, "shared");

        eventBus.post("shared", "Shared Event");

        Assertions.assertEquals(1, subscriber1Events.size());
        Assertions.assertEquals(1, subscriber2Events.size());
        Assertions.assertEquals("Shared Event", subscriber1Events.get(0));
        Assertions.assertEquals("Shared Event", subscriber2Events.get(0));
    }

    @Test
    public void testNullEventHandling() {
        AtomicReference<String> receivedEvent = new AtomicReference<>("not null");

        Subscriber<String> subscriber = receivedEvent::set;

        eventBus.register(subscriber, "nullEvent");
        assertThrows(NullPointerException.class, () -> eventBus.post("nullEvent", null));
    }

    @Test
    public void testExceptionInSubscriber() {
        Subscriber<String> exceptionSubscriber = event -> {
            throw new RuntimeException("Test exception");
        };

        Subscriber<String> normalSubscriber = new Subscriber<String>() {
            String received = null;

            @Override
            public void on(String event) {
                received = event;
            }
        };

        eventBus.register(exceptionSubscriber, "test");
        eventBus.register(normalSubscriber, "test");

        // Should not throw exception to caller
        Assertions.assertDoesNotThrow(() -> {
            eventBus.post("test", "Test Event");
        });
    }

    @Test
    public void testSubscriberWithThreadMode() throws InterruptedException {
        AtomicReference<Thread> executionThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Subscriber<String> subscriber = event -> {
            executionThread.set(Thread.currentThread());
            latch.countDown();
        };

        eventBus.register(subscriber, "threadTest", ThreadMode.THREAD_POOL_EXECUTOR);
        eventBus.post("threadTest", "Thread Test");

        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assertions.assertNotEquals(Thread.currentThread(), executionThread.get());
    }

    @Test
    public void testSubscriberUnregistration() {
        AtomicReference<String> receivedEvent = new AtomicReference<>();

        Subscriber<String> subscriber = receivedEvent::set;

        eventBus.register(subscriber, "unregTest");
        eventBus.post("unregTest", "First Event");
        Assertions.assertEquals("First Event", receivedEvent.get());

        eventBus.unregister(subscriber);
        receivedEvent.set(null);

        eventBus.post("unregTest", "Second Event");
        Assertions.assertNull(receivedEvent.get());
    }

    @Test
    public void testAnonymousClassImplementation() {
        final List<String> events = new ArrayList<>();

        Subscriber<String> subscriber = new Subscriber<String>() {
            @Override
            public void on(String event) {
                events.add("Received: " + event);
            }
        };

        eventBus.register(subscriber, "anonTest");
        eventBus.post("anonTest", "Anonymous Test");

        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals("Received: Anonymous Test", events.get(0));
    }

    @Test
    public void testSubscriberChaining() {
        StringBuilder result = new StringBuilder();

        Subscriber<String> firstSubscriber = event -> result.append("First:").append(event);
        Subscriber<String> secondSubscriber = event -> result.append("|Second:").append(event);

        eventBus.register(firstSubscriber, "chain");
        eventBus.register(secondSubscriber, "chain");

        eventBus.post("chain", "Test");

        Assertions.assertTrue(result.toString().contains("First:Test"));
        Assertions.assertTrue(result.toString().contains("Second:Test"));
    }

    @Test
    public void testCustomEventTypes() {
        AtomicReference<CustomEvent> receivedEvent = new AtomicReference<>();

        Subscriber<CustomEvent> subscriber = receivedEvent::set;

        eventBus.register(subscriber, "customEvent");

        CustomEvent event = new CustomEvent("Test Data");
        eventBus.post("customEvent", event);

        Assertions.assertNotNull(receivedEvent.get());
        Assertions.assertEquals("Test Data", receivedEvent.get().data);
    }

    @Test
    public void testSubscriberRequiresEventId() {
        Subscriber<Object> generalSubscriber = event -> {
        };

        // Lambda subscribers must be registered with event ID
        Assertions.assertThrows(RuntimeException.class, () -> {
            eventBus.register(generalSubscriber);
        });

        // Should work with event ID
        Assertions.assertDoesNotThrow(() -> {
            eventBus.register(generalSubscriber, "required");
        });
    }

    @Test
    public void testComplexTypeSubscriber() {
        AtomicReference<List<CustomEvent>> receivedList = new AtomicReference<>();

        Subscriber<List<CustomEvent>> complexSubscriber = receivedList::set;

        eventBus.register(complexSubscriber, "complexType");

        List<CustomEvent> eventList = List.of(new CustomEvent("Event 1"), new CustomEvent("Event 2"), new CustomEvent("Event 3"));

        eventBus.post("complexType", eventList);

        Assertions.assertNotNull(receivedList.get());
        Assertions.assertEquals(3, receivedList.get().size());
        Assertions.assertEquals("Event 2", receivedList.get().get(1).data);
    }

    // Helper class for testing
    private static class CustomEvent {
        final String data;

        CustomEvent(String data) {
            this.data = data;
        }
    }
}
