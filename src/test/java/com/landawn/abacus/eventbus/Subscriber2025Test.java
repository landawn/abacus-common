package com.landawn.abacus.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Subscriber2025Test extends TestBase {

    @Test
    public void testIsFunctionalInterface() {
        assertTrue(Subscriber.class.isAnnotationPresent(FunctionalInterface.class));
    }

    @Test
    public void testOnMethodExists() throws NoSuchMethodException {
        Method onMethod = Subscriber.class.getDeclaredMethod("on", Object.class);
        assertNotNull(onMethod);
        assertEquals(void.class, onMethod.getReturnType());
        assertEquals(1, onMethod.getParameterCount());
    }

    @Test
    public void testIsInterface() {
        assertTrue(Subscriber.class.isInterface());
    }

    @Test
    public void testLambdaImplementation() {
        AtomicReference<String> result = new AtomicReference<>();
        Subscriber<String> subscriber = event -> result.set(event);

        subscriber.on("test");
        assertEquals("test", result.get());
    }

    @Test
    public void testAnonymousClassImplementation() {
        AtomicReference<Integer> result = new AtomicReference<>();
        Subscriber<Integer> subscriber = new Subscriber<>() {
            @Override
            public void on(Integer event) {
                result.set(event * 2);
            }
        };

        subscriber.on(5);
        assertEquals(10, result.get());
    }

    @Test
    public void testMethodReferenceImplementation() {
        TestHandler handler = new TestHandler();
        Subscriber<String> subscriber = handler::handle;

        subscriber.on("hello");
        assertEquals("hello", handler.lastEvent);
    }

    @Test
    public void testNullEvent() {
        AtomicReference<String> result = new AtomicReference<>("initial");
        Subscriber<String> subscriber = event -> result.set(event);

        subscriber.on(null);
        assertNull(result.get());
    }

    @Test
    public void testMultipleInvocations() {
        AtomicReference<String> result = new AtomicReference<>();
        Subscriber<String> subscriber = event -> result.set(event);

        subscriber.on("first");
        assertEquals("first", result.get());

        subscriber.on("second");
        assertEquals("second", result.get());

        subscriber.on("third");
        assertEquals("third", result.get());
    }

    @Test
    public void testGenericTypeParameter() throws NoSuchMethodException {
        Method method = Subscriber.class.getDeclaredMethod("on", Object.class);
        Type[] paramTypes = method.getGenericParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
    }

    @Test
    public void testSingleAbstractMethod() {
        Method[] methods = Subscriber.class.getDeclaredMethods();
        long abstractMethods = java.util.Arrays.stream(methods).filter(m -> java.lang.reflect.Modifier.isAbstract(m.getModifiers())).count();
        assertEquals(1, abstractMethods);
    }

    static class TestHandler {
        String lastEvent;

        public void handle(String event) {
            this.lastEvent = event;
        }
    }
}
