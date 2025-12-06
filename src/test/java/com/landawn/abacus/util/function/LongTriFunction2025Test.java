package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongTriFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        final LongTriFunction<Long> function = (a, b, c) -> a + b + c;
        final Long result = function.apply(10L, 20L, 30L);
        assertEquals(60L, result);
    }

    @Test
    public void testApply_withLambda() {
        final LongTriFunction<String> function = (a, b, c) -> String.format("(%d,%d,%d)", a, b, c);
        final String result = function.apply(1L, 2L, 3L);
        assertEquals("(1,2,3)", result);
    }

    @Test
    public void testApply_withAnonymousClass() {
        final LongTriFunction<Long> function = new LongTriFunction<Long>() {
            @Override
            public Long apply(final long a, final long b, final long c) {
                return a * b * c;
            }
        };

        final Long result = function.apply(2L, 3L, 4L);
        assertEquals(24L, result);
    }

    @Test
    public void testAndThen() {
        final LongTriFunction<Long> function = (a, b, c) -> a + b + c;
        final java.util.function.Function<Long, String> after = val -> "Result: " + val;

        final LongTriFunction<String> composed = function.andThen(after);
        final String result = composed.apply(10L, 20L, 30L);

        assertEquals("Result: 60", result);
    }

    @Test
    public void testAndThen_multipleChains() {
        final LongTriFunction<Long> function = (a, b, c) -> a * b * c;
        final java.util.function.Function<Long, String> toString = Object::toString;
        final java.util.function.Function<String, Integer> toLength = String::length;

        final LongTriFunction<Integer> composed = function.andThen(toString).andThen(toLength);
        final Integer result = composed.apply(2L, 3L, 4L);   // 2*3*4 = 24, "24".length() = 2

        assertEquals(2, result);
    }

    @Test
    public void testApply_returnsNull() {
        final LongTriFunction<String> function = (a, b, c) -> null;
        final String result = function.apply(1L, 2L, 3L);
        assertNull(result);
    }

    @Test
    public void testApply_withNegativeValues() {
        final LongTriFunction<Long> function = (a, b, c) -> a + b + c;
        final Long result = function.apply(-10L, -20L, -30L);
        assertEquals(-60L, result);
    }

    @Test
    public void testApply_calculateAverage() {
        final LongTriFunction<Double> function = (a, b, c) -> (a + b + c) / 3.0;
        final Double result = function.apply(10L, 20L, 30L);
        assertEquals(20.0, result, 0.001);
    }

    @Test
    public void testApply_findMax() {
        final LongTriFunction<Long> function = (a, b, c) -> Math.max(a, Math.max(b, c));
        final Long result = function.apply(5L, 100L, 50L);
        assertEquals(100L, result);
    }

    @Test
    public void testApply_findMin() {
        final LongTriFunction<Long> function = (a, b, c) -> Math.min(a, Math.min(b, c));
        final Long result = function.apply(5L, 100L, 50L);
        assertEquals(5L, result);
    }

    @Test
    public void testApply_findMedian() {
        final LongTriFunction<Long> function = (a, b, c) -> {
            if ((a >= b && a <= c) || (a <= b && a >= c))
                return a;
            if ((b >= a && b <= c) || (b <= a && b >= c))
                return b;
            return c;
        };
        final Long result = function.apply(5L, 100L, 50L);
        assertEquals(50L, result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongTriFunction<String> function = (a, b, c) -> "test";
        assertNotNull(function.apply(1L, 2L, 3L));
    }

    @Test
    public void testMethodReference() {
        final LongTriFunction<Long> function = this::sumThree;
        final Long result = function.apply(5L, 10L, 15L);
        assertEquals(30L, result);
    }

    private Long sumThree(final long a, final long b, final long c) {
        return a + b + c;
    }
}
