package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

/** Applies the same copy, range, and empty-input contracts to every primitive overload. */
public class PrimitiveArrayEmptyRangeContractTest extends TestBase {
    static java.util.stream.Stream<Class<?>> components() {
        return java.util.stream.Stream.of(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class);
    }

    private static Object array(Class<?> component, int... values) {
        Object result = java.lang.reflect.Array.newInstance(component, values.length);
        for (int i = 0; i < values.length; i++) {
            Object value;
            if (component == boolean.class) {
                value = values[i] != 0;
            } else if (component == char.class) {
                value = (char) values[i];
            } else if (component == byte.class) {
                value = (byte) values[i];
            } else if (component == short.class) {
                value = (short) values[i];
            } else if (component == int.class) {
                value = values[i];
            } else if (component == long.class) {
                value = (long) values[i];
            } else if (component == float.class) {
                value = (float) values[i];
            } else {
                value = (double) values[i];
            }
            java.lang.reflect.Array.set(result, i, value);
        }
        return result;
    }

    private static List<Object> boxed(Object array) {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < java.lang.reflect.Array.getLength(array); i++) {
            values.add(java.lang.reflect.Array.get(array, i));
        }
        return values;
    }

    private static Object call(String name, Class<?>[] types, Object... args) throws Throwable {
        Method method = N.class.getMethod(name, types);
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static void assertContents(Object expected, Object actual) {
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(boxed(expected), boxed(actual));
    }

    @ParameterizedTest
    @MethodSource("components")
    void concatenatingOneArrayStillCopiesIt(Class<?> component) throws Throwable {
        Object original = array(component, 0, 1, 0);
        Object group = java.lang.reflect.Array.newInstance(original.getClass(), 1);
        java.lang.reflect.Array.set(group, 0, original);
        Object result = call("concat", new Class<?>[] { group.getClass() }, group);
        assertContents(original, result);
        assertNotSame(original, result);

        java.lang.reflect.Array.set(group, 0, null);
        assertContents(array(component), call("concat", new Class<?>[] { group.getClass() }, group));
        java.lang.reflect.Array.set(group, 0, array(component));
        assertContents(array(component), call("concat", new Class<?>[] { group.getClass() }, group));
    }

    @ParameterizedTest
    @MethodSource("components")
    void removingNothingReturnsAnIndependentCopy(Class<?> component) throws Throwable {
        Object original = array(component, 0, 1, 0);
        Class<?> type = original.getClass();
        for (Object result : new Object[] { call("removeAt", new Class<?>[] { type, int[].class }, original, new int[0]),
                call("removeRange", new Class<?>[] { type, int.class, int.class }, original, 1, 1) }) {
            assertContents(original, result);
            assertNotSame(original, result);
        }
        assertContents(array(component, 1), call("removeAt", new Class<?>[] { type, int[].class }, original, new int[] { 2, 0, 2 }));
        assertContents(array(component), call("removeRange", new Class<?>[] { type, int.class, int.class }, null, 0, 0));
        assertContents(array(component), call("removeRange", new Class<?>[] { type, int.class, int.class }, original, 0, 3));
        assertContents(array(component, 0, 1, 0), original);
    }

    @ParameterizedTest
    @MethodSource("components")
    void removingValuesAndReplacingRangesHandleEmptyInputs(Class<?> component) throws Throwable {
        Object original = array(component, 0, 1, 0);
        Object empty = array(component);
        Class<?> type = original.getClass();
        assertContents(empty, call("removeAll", new Class<?>[] { type, type }, null, original));
        assertContents(empty, call("removeAll", new Class<?>[] { type, type }, empty, original));
        assertContents(array(component, 1), call("removeAll", new Class<?>[] { type, type }, original, array(component, 0)));
        assertContents(array(component, 0, 0), call("replaceRange", new Class<?>[] { type, int.class, int.class, type }, original, 1, 2, empty));
        Object copy = call("replaceRange", new Class<?>[] { type, int.class, int.class, type }, null, 0, 0, original);
        assertContents(original, copy);
        assertNotSame(original, copy);
        assertContents(empty, call("replaceRange", new Class<?>[] { type, int.class, int.class, type }, null, 0, 0, empty));
        assertContents(array(component, 0, 1, 0), original);
    }

    @ParameterizedTest
    @MethodSource("components")
    void duplicateRemovalHandlesEmptyAndSingletonRanges(Class<?> component) throws Throwable {
        Object empty = array(component);
        Object original = array(component, 0, 1, 1);
        Class<?> type = original.getClass();
        assertContents(empty, call("removeDuplicates", new Class<?>[] { type }, empty));
        assertContents(empty, call("removeDuplicates", new Class<?>[] { type }, new Object[] { null }));
        assertContents(array(component, 0, 1), call("removeDuplicates", new Class<?>[] { type }, original));
        if (component != boolean.class) {
            for (boolean sorted : new boolean[] { false, true }) {
                assertContents(empty, call("removeDuplicates", new Class<?>[] { type, boolean.class }, empty, sorted));
                assertContents(array(component, 1),
                        call("removeDuplicates", new Class<?>[] { type, int.class, int.class, boolean.class }, original, 1, 2, sorted));
                assertContents(empty, call("removeDuplicates", new Class<?>[] { type, int.class, int.class, boolean.class }, original, 1, 1, sorted));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("components")
    void emptyCollectionConversionsStillHonorTheFactory(Class<?> component) throws Throwable {
        Object original = array(component, 0, 1, 0);
        Class<?> type = original.getClass();
        Class<?>[] range = { type, int.class, int.class };
        assertEquals(List.of(), call("toList", range, original, 1, 1));
        assertEquals(Set.of(), call("toSet", range, original, 1, 1));
        AtomicInteger capacity = new AtomicInteger(-1);
        List<Object> result = new ArrayList<>();
        IntFunction<List<Object>> factory = n -> {
            assertEquals(-1, capacity.getAndSet(n), "factory must be invoked once");
            return result;
        };
        Class<?>[] factoryRange = { type, int.class, int.class, IntFunction.class };
        assertSame(result, call("toCollection", factoryRange, original, 1, 1, factory));
        assertEquals(0, capacity.get());
        assertTrue(result.isEmpty());
        IntFunction<List<Object>> nullFactory = n -> null;
        assertThrows(NullPointerException.class, () -> call("toCollection", factoryRange, original, 1, 1, nullFactory));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> call("checkArgNotEmpty", new Class<?>[] { type, String.class }, array(component), "values"));
        assertTrue(failure.getMessage().contains("values"));
        assertSame(original, call("checkArgNotEmpty", new Class<?>[] { type, String.class }, original, "values"));
    }

    @ParameterizedTest
    @MethodSource("components")
    void emptyAndSingletonRangeMutationsDoNotTouchNeighbors(Class<?> component) throws Throwable {
        Object original = array(component, 1, 0, 1);
        Class<?> type = original.getClass();
        for (String operation : List.of("reverse", "sort")) {
            call(operation, new Class<?>[] { type, int.class, int.class }, original, 1, 1);
            assertContents(array(component, 1, 0, 1), original);
            call(operation, new Class<?>[] { type, int.class, int.class }, original, 1, 2);
            assertContents(array(component, 1, 0, 1), original);
        }
        call("rotate", new Class<?>[] { type, int.class, int.class, int.class }, original, 1, 1, Integer.MIN_VALUE);
        assertContents(array(component, 1, 0, 1), original);
    }
}
