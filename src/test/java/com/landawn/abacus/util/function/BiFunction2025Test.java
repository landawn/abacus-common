package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;

@Tag("2025")
public class BiFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        BiFunction<String, Integer, String> biFunction = (s, i) -> s + ":" + i;
        String result = biFunction.apply("value", 10);
        assertEquals("value:10", result);
    }

    @Test
    public void testApplyWithLambda() {
        BiFunction<Integer, Integer, Integer> biFunction = (a, b) -> a + b;
        Integer result = biFunction.apply(5, 10);
        assertEquals(15, result);
    }

    @Test
    public void testApplyWithMethodReference() {
        BiFunction<String, String, String> biFunction = String::concat;
        String result = biFunction.apply("Hello", "World");
        assertEquals("HelloWorld", result);
    }

    @Test
    public void testAndThen() {
        BiFunction<String, Integer, String> biFunction = (s, i) -> s + ":" + i;
        java.util.function.Function<String, Integer> afterFunction = String::length;

        BiFunction<String, Integer, Integer> chainedFunction = biFunction.andThen(afterFunction);
        Integer finalResult = chainedFunction.apply("value", 10);

        assertEquals(8, finalResult);   // "value:10" has length 8
    }

    @Test
    public void testAndThenMultipleChains() {
        BiFunction<Integer, Integer, Integer> biFunction = (a, b) -> a + b;
        java.util.function.Function<Integer, Integer> multiply = x -> x * 2;
        java.util.function.Function<Integer, String> toString = Object::toString;

        BiFunction<Integer, Integer, String> chainedFunction = biFunction.andThen(multiply).andThen(toString);
        String result = chainedFunction.apply(5, 10);

        assertEquals("30", result);   // (5 + 10) * 2 = 30
    }

    @Test
    public void testAndThenWithException() {
        BiFunction<String, Integer, String> biFunction = (s, i) -> s + ":" + i;
        java.util.function.Function<String, Integer> afterFunction = s -> {
            throw new RuntimeException("Test exception");
        };

        BiFunction<String, Integer, Integer> chainedFunction = biFunction.andThen(afterFunction);

        assertThrows(RuntimeException.class, () -> chainedFunction.apply("test", 5));
    }

    @Test
    public void testApplyWithException() {
        BiFunction<String, Integer, String> biFunction = (s, i) -> {
            throw new RuntimeException("Apply exception");
        };

        assertThrows(RuntimeException.class, () -> biFunction.apply("test", 5));
    }

    @Test
    public void testToThrowable() {
        BiFunction<String, Integer, String> biFunction = (s, i) -> s + i;
        Throwables.BiFunction<String, Integer, String, ?> throwableBiFunction = biFunction.toThrowable();

        assertNotNull(throwableBiFunction);
    }

    @Test
    public void testToThrowableWithExecution() throws Throwable {
        BiFunction<Integer, Integer, Integer> biFunction = (a, b) -> a * b;
        Throwables.BiFunction<Integer, Integer, Integer, ?> throwableBiFunction = biFunction.toThrowable();

        Integer result = throwableBiFunction.apply(7, 6);
        assertEquals(42, result);
    }

    @Test
    public void testAnonymousClass() {
        BiFunction<String, String, String> biFunction = new BiFunction<String, String, String>() {
            @Override
            public String apply(String s1, String s2) {
                return s1 + "-" + s2;
            }
        };

        String result = biFunction.apply("foo", "bar");
        assertEquals("foo-bar", result);
    }

    @Test
    public void testWithNullValues() {
        BiFunction<String, String, String> biFunction = (s1, s2) -> String.valueOf(s1) + String.valueOf(s2);
        String result = biFunction.apply(null, null);
        assertEquals("nullnull", result);
    }

    @Test
    public void testReturnsNull() {
        BiFunction<String, String, String> biFunction = (s1, s2) -> null;
        String result = biFunction.apply("test", "value");
        assertNull(result);
    }

    @Test
    public void testWithDifferentTypes() {
        BiFunction<Integer, String, Double> biFunction = (i, s) -> i + Double.parseDouble(s);
        Double result = biFunction.apply(10, "3.5");
        assertEquals(13.5, result, 0.001);
    }

    @Test
    public void testComplexOperation() {
        BiFunction<String, String, Integer> biFunction = (s1, s2) -> {
            if (s1 == null || s2 == null) {
                return 0;
            }
            return s1.length() + s2.length();
        };

        assertEquals(10, biFunction.apply("hello", "world"));
        assertEquals(5, biFunction.apply("hello", ""));
        assertEquals(0, biFunction.apply(null, "world"));
    }
}
