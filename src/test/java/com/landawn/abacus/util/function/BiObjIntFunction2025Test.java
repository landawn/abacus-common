package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BiObjIntFunction2025Test extends TestBase {
    @Test
    public void testAndThenWithException() {
        BiObjIntFunction<String, String, String> function = (t, u, i) -> t + u;
        java.util.function.Function<String, Integer> afterFunction = s -> {
            throw new RuntimeException("Test exception");
        };

        BiObjIntFunction<String, String, Integer> chainedFunction = function.andThen(afterFunction);

        assertThrows(RuntimeException.class, () -> chainedFunction.apply("test", "value", 5));
    }

    @Test
    public void testApplyWithException() {
        BiObjIntFunction<String, String, String> function = (t, u, i) -> {
            throw new RuntimeException("Apply exception");
        };

        assertThrows(RuntimeException.class, () -> function.apply("test", "value", 5));
    }

    @Test
    public void testAnonymousClass() {
        BiObjIntFunction<String, String, String> function = new BiObjIntFunction<String, String, String>() {
            @Override
            public String apply(String t, String u, int i) {
                return t + "-" + u + "-" + i;
            }
        };

        String result = function.apply("foo", "bar", 99);

        assertEquals("foo-bar-99", result);
    }

    @Test
    public void testWithNullObjects() {
        BiObjIntFunction<String, String, String> function = (t, u, i) ->
            String.valueOf(t) + "," + String.valueOf(u) + "," + i;

        String result = function.apply(null, null, 42);

        assertEquals("null,null,42", result);
    }

    @Test
    public void testReturnsNull() {
        BiObjIntFunction<String, String, String> function = (t, u, i) -> null;

        String result = function.apply("test", "value", 5);

        assertNull(result);
    }

    @Test
    public void testWithNegativeInt() {
        BiObjIntFunction<String, String, String> function = (t, u, i) -> i + ":" + t + u;

        String result = function.apply("A", "B", -10);

        assertEquals("-10:AB", result);
    }

    @Test
    public void testComplexOperation() {
        BiObjIntFunction<String, String, String> function = (t, u, i) -> {
            if (t == null || u == null) {
                return "Invalid";
            }
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < i; j++) {
                sb.append(t).append(u);
                if (j < i - 1) sb.append(", ");
            }
            return sb.toString();
        };

        assertEquals("AB, AB, AB", function.apply("A", "B", 3));
        assertEquals("", function.apply("X", "Y", 0));
        assertEquals("Invalid", function.apply(null, "Y", 2));
    }

    @Test
    public void testWithDifferentTypes() {
        BiObjIntFunction<Integer, Double, String> function = (t, u, i) ->
            "sum=" + (t + u + i);

        String result = function.apply(10, 5.5, 20);

        assertEquals("sum=35.5", result);
    }

    @Test
    public void testReturningDifferentType() {
        BiObjIntFunction<String, String, Boolean> function = (t, u, i) ->
            (t.length() + u.length()) == i;

        assertEquals(true, function.apply("Hello", "World", 10));
        assertEquals(false, function.apply("Hi", "There", 10));
    }
}
