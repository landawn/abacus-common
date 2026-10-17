package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.*;
import com.landawn.abacus.util.stream.BaseStream;

/** The optional families share the same formatted-error and immutable-container contracts. */
public class OptionalFallbackContractTest extends TestBase {
    record Value(Object present, Object empty, Object value) {
        @Override
        public String toString() {
            return present.getClass().getSimpleName();
        }
    }

    static List<Value> values() {
        return List.of(new Value(OptionalBoolean.of(true), OptionalBoolean.empty(), true), new Value(OptionalChar.of('x'), OptionalChar.empty(), 'x'),
                new Value(OptionalByte.of((byte) 7), OptionalByte.empty(), (byte) 7), new Value(OptionalShort.of((short) 7), OptionalShort.empty(), (short) 7),
                new Value(OptionalInt.of(7), OptionalInt.empty(), 7), new Value(OptionalLong.of(7), OptionalLong.empty(), 7L),
                new Value(OptionalFloat.of(7), OptionalFloat.empty(), 7F), new Value(OptionalDouble.of(7), OptionalDouble.empty(), 7D),
                new Value(Optional.of("value"), Optional.empty(), "value"));
    }

    @ParameterizedTest
    @MethodSource("values")
    void messageOverloadsReturnThePresentValueOrFormatTheMissingValueError(Value value) throws Throwable {
        Class<?>[][] signatures = { { String.class }, { String.class, Object.class }, { String.class, Object.class, Object.class },
                { String.class, Object.class, Object.class, Object.class }, { String.class, Object[].class } };
        Object[][] arguments = { { "missing" }, { "missing {}", "a" }, { "missing {} {}", "a", "b" }, { "missing {} {} {}", "a", "b", "c" },
                { "missing {} {} {} {}", new Object[] { "a", "b", "c", "d" } } };
        String[] expected = { "missing", "missing a", "missing a b", "missing a b c", "missing a b c d" };
        for (int i = 0; i < signatures.length; i++) {
            Method method = value.present.getClass().getMethod("orElseThrow", signatures[i]);
            Object[] args = arguments[i];
            assertEquals(value.value, invoke(method, value.present, args));
            NoSuchElementException failure = assertThrows(NoSuchElementException.class, () -> invoke(method, value.empty, args));
            assertEquals(expected[i], failure.getMessage());
        }
    }

    @ParameterizedTest
    @MethodSource("values")
    @SuppressWarnings("unchecked")
    void immutableConversionsRepresentPresenceAndRejectMutation(Value value) throws Throwable {
        for (String name : List.of("toImmutableList", "toImmutableSet")) {
            Method method = value.present.getClass().getMethod(name);
            Collection<Object> present = (Collection<Object>) invoke(method, value.present);
            Collection<Object> empty = (Collection<Object>) invoke(method, value.empty);
            assertEquals(List.of(value.value), List.copyOf(present));
            assertTrue(empty.isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> present.add(value.value));
            assertThrows(UnsupportedOperationException.class, () -> empty.add(value.value));
        }
    }

    @ParameterizedTest
    @MethodSource("values")
    void emptyMappingsNeverInvokeTheirCallbacks(Value value) throws Throwable {
        for (Method method : value.empty.getClass().getMethods()) {
            if (method.getParameterCount() != 1 || !method.getName().matches("map(?:To.*)?|flatMap|filter"))
                continue;
            Class<?> callbackType = method.getParameterTypes()[0];
            Object callback = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[] { callbackType }, (proxy, called, args) -> {
                throw new AssertionError("An empty optional invoked " + called);
            });
            Object result = invoke(method, value.empty, callback);
            assertEquals(true, invoke(result.getClass().getMethod("isEmpty"), result), method.toString());
        }
    }

    @ParameterizedTest
    @MethodSource("values")
    void fallbackSuppliersRunOnlyWhenEmpty(Value value) throws Throwable {
        Method method = Arrays.stream(value.present.getClass().getMethods()).filter(m -> m.getName().equals("orElseGet")).findFirst().orElseThrow();
        Class<?> callbackType = method.getParameterTypes()[0];
        AtomicInteger calls = new AtomicInteger();
        Object supplier = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[] { callbackType }, (proxy, called, args) -> {
            calls.incrementAndGet();
            return value.value;
        });
        assertEquals(value.value, invoke(method, value.present, supplier));
        assertEquals(0, calls.get());
        assertEquals(value.value, invoke(method, value.empty, supplier));
        assertEquals(1, calls.get());

        Method or = value.present.getClass().getMethod("or", Supplier.class);
        Supplier<Object> alternative = () -> {
            calls.incrementAndGet();
            return value.present;
        };
        assertSame(value.present, invoke(or, value.present, alternative));
        assertEquals(1, calls.get());
        assertSame(value.present, invoke(or, value.empty, alternative));
        assertEquals(2, calls.get());
        assertThrows(NullPointerException.class, () -> invoke(or, value.empty, (Supplier<Object>) () -> null));
    }

    @ParameterizedTest
    @MethodSource("values")
    void ifPresentDispatchesTheValueExactlyOnce(Value value) throws Throwable {
        Method method = Arrays.stream(value.present.getClass().getMethods()).filter(m -> m.getName().equals("ifPresent")).findFirst().orElseThrow();
        Class<?> callbackType = method.getParameterTypes()[0];
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Object> seen = new AtomicReference<>();
        Object action = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[] { callbackType }, (proxy, called, args) -> {
            calls.incrementAndGet();
            seen.set(args[0]);
            return null;
        });
        invoke(method, value.empty, action);
        assertEquals(0, calls.get());
        invoke(method, value.present, action);
        assertEquals(1, calls.get());
        assertEquals(value.value, seen.get());
    }

    @ParameterizedTest
    @MethodSource("values")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void mutableContainersStreamsAndOrderingRespectPresence(Value value) throws Throwable {
        for (String name : List.of("toList", "toSet")) {
            Method method = value.present.getClass().getMethod(name);
            Collection<Object> empty = (Collection<Object>) invoke(method, value.empty);
            assertTrue(empty.isEmpty());
            assertTrue(empty.add(value.value));
            assertEquals(true, invoke(value.empty.getClass().getMethod("isEmpty"), value.empty));
            assertEquals(List.of(value.value), List.copyOf((Collection<?>) invoke(method, value.present)));
        }
        Method streamMethod = value.present.getClass().getMethod("stream");
        try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = (BaseStream<?, ?, ?, ?, ?, ?, ?, ?>) invoke(streamMethod, value.empty)) {
            assertEquals(0, stream.count());
        }
        try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = (BaseStream<?, ?, ?, ?, ?, ?, ?, ?>) invoke(streamMethod, value.present)) {
            assertEquals(1, stream.count());
        }
        assertNotEquals(value.present, value.empty);
        assertNotEquals(value.empty, value.present);
        assertNotEquals(value.present, "unrelated");
        assertNotEquals(value.present, null);
        assertEquals(value.present, value.present);
        if (value.present instanceof Comparable comparable) {
            assertTrue(comparable.compareTo(value.empty) > 0);
            assertTrue(((Comparable) value.empty).compareTo(value.present) < 0);
            assertEquals(0, comparable.compareTo(value.present));
            assertEquals(0, ((Comparable) value.empty).compareTo(value.empty));
        }
        Method get = value.present.getClass().getMethod("orElseThrow");
        assertEquals(value.value, invoke(get, value.present));
        assertThrows(NoSuchElementException.class, () -> invoke(get, value.empty));
    }

    private static Object invoke(Method method, Object receiver, Object... args) throws Throwable {
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
