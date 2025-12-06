package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongBiFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        final LongBiFunction<String> function = (t, u) -> (t + u) + "";
        final String result = function.apply(10L, 20L);
        assertEquals("30", result);
    }

    @Test
    public void testApply_withLambda() {
        final LongBiFunction<Long> function = (a, b) -> a * b;
        final Long result = function.apply(5L, 7L);
        assertEquals(35L, result);
    }

    @Test
    public void testApply_withAnonymousClass() {
        final LongBiFunction<String> function = new LongBiFunction<String>() {
            @Override
            public String apply(final long t, final long u) {
                return "Values: " + t + ", " + u;
            }
        };

        final String result = function.apply(100L, 200L);
        assertEquals("Values: 100, 200", result);
    }

    @Test
    public void testAndThen() {
        final LongBiFunction<Long> function = (t, u) -> t + u;
        final java.util.function.Function<Long, String> after = val -> "Result: " + val;

        final LongBiFunction<String> composed = function.andThen(after);
        final String result = composed.apply(10L, 20L);

        assertEquals("Result: 30", result);
    }

    @Test
    public void testAndThen_multipleChains() {
        final LongBiFunction<Long> function = (t, u) -> t - u;
        final java.util.function.Function<Long, Long> multiply = val -> val * 2;
        final java.util.function.Function<Long, String> toString = Object::toString;

        final LongBiFunction<String> composed = function.andThen(multiply).andThen(toString);
        final String result = composed.apply(20L, 5L);

        assertEquals("30", result);   // (20 - 5) * 2 = 30
    }

    @Test
    public void testApply_returnsNull() {
        final LongBiFunction<String> function = (t, u) -> null;
        final String result = function.apply(1L, 2L);
        assertEquals(null, result);
    }

    @Test
    public void testApply_withNegativeValues() {
        final LongBiFunction<Long> function = (t, u) -> t + u;
        final Long result = function.apply(-10L, -20L);
        assertEquals(-30L, result);
    }

    @Test
    public void testApply_withZeroValues() {
        final LongBiFunction<Long> function = (t, u) -> t * u;
        final Long result = function.apply(0L, 100L);
        assertEquals(0L, result);
    }

    @Test
    public void testApply_withMaxValues() {
        final LongBiFunction<Long> function = Math::max;
        final Long result = function.apply(Long.MAX_VALUE, Long.MIN_VALUE);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    public void testApply_returningDifferentType() {
        final LongBiFunction<Double> function = (t, u) -> (double) (t + u) / 2;
        final Double result = function.apply(10L, 20L);
        assertEquals(15.0, result, 0.001);
    }

    @Test
    public void testApply_complexObject() {
        final LongBiFunction<String> function = (start, end) -> String.format("Range [%d, %d]", start, end);
        final String result = function.apply(1L, 100L);
        assertEquals("Range [1, 100]", result);
    }

    @Test
    public void testAndThen_transformationType() {
        final LongBiFunction<String> function = (t, u) -> t + "," + u;
        final java.util.function.Function<String, Integer> after = String::length;

        final LongBiFunction<Integer> composed = function.andThen(after);
        final Integer result = composed.apply(123L, 456L);

        assertEquals(7, result);   // "123,456".length()
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongBiFunction<String> function = (t, u) -> "test";
        assertNotNull(function.apply(1L, 2L));
    }

    @Test
    public void testMethodReference() {
        final LongBiFunction<String> function = this::combineToString;
        final String result = function.apply(5L, 10L);
        assertEquals("5+10", result);
    }

    private String combineToString(final long a, final long b) {
        return a + "+" + b;
    }
}
