package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongObjFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        LongObjFunction<String, String> function = (l, s) -> l + ":" + s;

        String result1 = function.apply(5L, "test");
        String result2 = function.apply(10L, "value");

        assertEquals("5:test", result1);
        assertEquals("10:value", result2);
    }

    @Test
    public void testApplyWithLambda() {
        LongObjFunction<Integer, Long> function = (l, i) -> l * i;

        Long result = function.apply(5L, 4);

        assertEquals(20L, result);
    }

    @Test
    public void testApplyWithAnonymousClass() {
        LongObjFunction<String, String> function = new LongObjFunction<String, String>() {
            @Override
            public String apply(long t, String u) {
                return String.format("%d-%s", t, u);
            }
        };

        String result = function.apply(100L, "test");

        assertEquals("100-test", result);
    }

    @Test
    public void testAndThen() {
        LongObjFunction<String, String> function = (l, s) -> l + ":" + s;
        java.util.function.Function<String, String> after = s -> s.toUpperCase();

        LongObjFunction<String, String> combined = function.andThen(after);
        String result = combined.apply(5L, "test");

        assertEquals("5:TEST", result);
    }

    @Test
    public void testAndThenChaining() {
        LongObjFunction<Integer, Integer> function = (l, i) -> (int) (l + i);
        java.util.function.Function<Integer, Integer> after1 = i -> i * 2;
        java.util.function.Function<Integer, String> after2 = i -> "Result:" + i;

        LongObjFunction<Integer, String> combined = function.andThen(after1).andThen(after2);
        String result = combined.apply(5L, 10);

        assertEquals("Result:30", result);
    }

    @Test
    public void testWithNegativeValues() {
        LongObjFunction<String, String> function = (l, s) -> l + ":" + s;

        String result1 = function.apply(-5L, "negative");
        String result2 = function.apply(-100L, "value");

        assertEquals("-5:negative", result1);
        assertEquals("-100:value", result2);
    }

    @Test
    public void testWithBoundaryValues() {
        LongObjFunction<String, String> function = (l, s) -> l + ":" + s;

        String result1 = function.apply(Long.MAX_VALUE, "max");
        String result2 = function.apply(Long.MIN_VALUE, "min");
        String result3 = function.apply(0L, "zero");

        assertEquals(Long.MAX_VALUE + ":max", result1);
        assertEquals(Long.MIN_VALUE + ":min", result2);
        assertEquals("0:zero", result3);
    }

    @Test
    public void testWithNullObject() {
        LongObjFunction<String, String> function = (l, s) -> l + ":" + s;

        String result = function.apply(5L, null);

        assertEquals("5:null", result);
    }

    @Test
    public void testReturningNull() {
        LongObjFunction<String, String> function = (l, s) -> null;

        String result = function.apply(5L, "test");

        assertNull(result);
    }

    @Test
    public void testWithDifferentTypes() {
        LongObjFunction<Integer, Boolean> function = (l, i) -> l > i;

        Boolean result1 = function.apply(10L, 5);
        Boolean result2 = function.apply(3L, 10);

        assertEquals(true, result1);
        assertEquals(false, result2);
    }

    @Test
    public void testComplexCalculation() {
        LongObjFunction<Long, Long> function = (l1, l2) -> l1 * l2 + l1 / l2;

        Long result = function.apply(10L, 2L);

        assertEquals(25L, result);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(LongObjFunction.class.getAnnotation(FunctionalInterface.class));
    }
}
