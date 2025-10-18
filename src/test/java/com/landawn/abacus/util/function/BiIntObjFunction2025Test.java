package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BiIntObjFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        BiIntObjFunction<String, String> function = (i, j, s) -> s + ":" + (i + j);

        String result = function.apply(10, 20, "Sum");

        assertEquals("Sum:30", result);
    }

    @Test
    public void testApplyWithLambda() {
        BiIntObjFunction<String, Integer> function = (i, j, s) -> s.length() + i + j;

        Integer result = function.apply(5, 10, "hello");

        assertEquals(20, result);  // 5 + 5 + 10
    }

    @Test
    public void testApplyReturningString() {
        BiIntObjFunction<Integer, String> function = (i, j, obj) -> {
            return "i=" + i + ", j=" + j + ", obj=" + obj;
        };

        String result = function.apply(7, 3, 42);

        assertEquals("i=7, j=3, obj=42", result);
    }

    @Test
    public void testAndThen() {
        BiIntObjFunction<String, String> function = (i, j, s) -> s + (i + j);
        java.util.function.Function<String, Integer> afterFunction = String::length;

        BiIntObjFunction<String, Integer> chainedFunction = function.andThen(afterFunction);
        Integer result = chainedFunction.apply(5, 10, "value");

        assertEquals(7, result);  // "value15" has length 7
    }

    @Test
    public void testAndThenMultipleChains() {
        BiIntObjFunction<String, Integer> function = (i, j, s) -> s.length() + i + j;
        java.util.function.Function<Integer, Integer> doubleIt = x -> x * 2;
        java.util.function.Function<Integer, String> toString = Object::toString;

        BiIntObjFunction<String, String> chainedFunction = function.andThen(doubleIt).andThen(toString);
        String result = chainedFunction.apply(5, 10, "hi");

        assertEquals("34", result);  // (2 + 5 + 10) * 2 = 34
    }

    @Test
    public void testAndThenWithException() {
        BiIntObjFunction<String, String> function = (i, j, s) -> s + i;
        java.util.function.Function<String, Integer> afterFunction = s -> {
            throw new RuntimeException("Test exception");
        };

        BiIntObjFunction<String, Integer> chainedFunction = function.andThen(afterFunction);

        assertThrows(RuntimeException.class, () -> chainedFunction.apply(5, 10, "test"));
    }

    @Test
    public void testApplyWithException() {
        BiIntObjFunction<String, String> function = (i, j, s) -> {
            throw new RuntimeException("Apply exception");
        };

        assertThrows(RuntimeException.class, () -> function.apply(5, 10, "test"));
    }

    @Test
    public void testAnonymousClass() {
        BiIntObjFunction<String, String> function = new BiIntObjFunction<String, String>() {
            @Override
            public String apply(int i, int j, String s) {
                return s + ":" + i + "," + j;
            }
        };

        String result = function.apply(1, 2, "coords");

        assertEquals("coords:1,2", result);
    }

    @Test
    public void testWithNullObject() {
        BiIntObjFunction<String, String> function = (i, j, s) -> String.valueOf(s) + (i + j);

        String result = function.apply(5, 10, null);

        assertEquals("null15", result);
    }

    @Test
    public void testReturnsNull() {
        BiIntObjFunction<String, String> function = (i, j, s) -> null;

        String result = function.apply(5, 10, "test");

        assertNull(result);
    }

    @Test
    public void testWithNegativeInts() {
        BiIntObjFunction<String, Integer> function = (i, j, s) -> i * j;

        Integer result = function.apply(-5, 7, "ignored");

        assertEquals(-35, result);
    }

    @Test
    public void testComplexOperation() {
        BiIntObjFunction<String, String> function = (i, j, s) -> {
            if (s == null || s.isEmpty()) {
                return "Invalid";
            }
            int sum = i + j;
            return s + " sum=" + sum + " avg=" + (sum / 2.0);
        };

        assertEquals("test sum=15 avg=7.5", function.apply(5, 10, "test"));
        assertEquals("Invalid", function.apply(5, 10, ""));
        assertEquals("Invalid", function.apply(5, 10, null));
    }

    @Test
    public void testWithDifferentObjectTypes() {
        BiIntObjFunction<Integer, Double> function = (i, j, obj) -> (double) (i + j + obj);

        Double result = function.apply(10, 20, 5);

        assertEquals(35.0, result, 0.001);
    }
}
