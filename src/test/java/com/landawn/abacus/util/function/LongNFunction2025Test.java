package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongNFunction2025Test extends TestBase {

    @Test
    public void testApply_noArgs() {
        final LongNFunction<Integer> function = args -> args.length;
        final Integer result = function.apply();
        assertEquals(0, result);
    }

    @Test
    public void testApply_singleArg() {
        final LongNFunction<Long> function = args -> args[0];
        final Long result = function.apply(42L);
        assertEquals(42L, result);
    }

    @Test
    public void testApply_multipleArgs() {
        final LongNFunction<Long> function = args -> {
            long sum = 0;
            for (final long arg : args) {
                sum += arg;
            }
            return sum;
        };

        final Long result = function.apply(10L, 20L, 30L, 40L);
        assertEquals(100L, result);
    }

    @Test
    public void testApply_withLambda() {
        final LongNFunction<String> function = args -> {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(args[i]);
            }
            return sb.toString();
        };

        final String result = function.apply(1L, 2L, 3L);
        assertEquals("1,2,3", result);
    }

    @Test
    public void testApply_withAnonymousClass() {
        final LongNFunction<Integer> function = new LongNFunction<Integer>() {
            @Override
            public Integer apply(final long... args) {
                return args.length;
            }
        };

        final Integer result = function.apply(1L, 2L, 3L, 4L, 5L);
        assertEquals(5, result);
    }

    @Test
    public void testAndThen() {
        final LongNFunction<Long> function = args -> {
            long sum = 0;
            for (final long arg : args) {
                sum += arg;
            }
            return sum;
        };
        final java.util.function.Function<Long, String> after = val -> "Result: " + val;

        final LongNFunction<String> composed = function.andThen(after);
        final String result = composed.apply(10L, 20L, 30L);

        assertEquals("Result: 60", result);
    }

    @Test
    public void testAndThen_multipleChains() {
        final LongNFunction<Long> function = args -> {
            long product = 1;
            for (final long arg : args) {
                product *= arg;
            }
            return product;
        };
        final java.util.function.Function<Long, String> toString = Object::toString;
        final java.util.function.Function<String, Integer> toLength = String::length;

        final LongNFunction<Integer> composed = function.andThen(toString).andThen(toLength);
        final Integer result = composed.apply(2L, 3L, 4L); // 2*3*4 = 24, "24".length() = 2

        assertEquals(2, result);
    }

    @Test
    public void testApply_returnsNull() {
        final LongNFunction<String> function = args -> null;
        final String result = function.apply(1L, 2L, 3L);
        assertNull(result);
    }

    @Test
    public void testApply_withNegativeValues() {
        final LongNFunction<Long> function = args -> {
            long sum = 0;
            for (final long arg : args) {
                sum += arg;
            }
            return sum;
        };

        final Long result = function.apply(-10L, -20L, -30L);
        assertEquals(-60L, result);
    }

    @Test
    public void testApply_calculateAverage() {
        final LongNFunction<Double> function = args -> {
            if (args.length == 0) return 0.0;
            long sum = 0;
            for (final long arg : args) {
                sum += arg;
            }
            return (double) sum / args.length;
        };

        final Double result = function.apply(10L, 20L, 30L, 40L);
        assertEquals(25.0, result, 0.001);
    }

    @Test
    public void testApply_findMax() {
        final LongNFunction<Long> function = args -> {
            if (args.length == 0) return Long.MIN_VALUE;
            long max = args[0];
            for (final long arg : args) {
                if (arg > max) max = arg;
            }
            return max;
        };

        final Long result = function.apply(5L, 100L, 50L, 25L);
        assertEquals(100L, result);
    }

    @Test
    public void testApply_findMin() {
        final LongNFunction<Long> function = args -> {
            if (args.length == 0) return Long.MAX_VALUE;
            long min = args[0];
            for (final long arg : args) {
                if (arg < min) min = arg;
            }
            return min;
        };

        final Long result = function.apply(5L, 100L, 50L, 25L);
        assertEquals(5L, result);
    }

    @Test
    public void testApply_returningDifferentType() {
        final LongNFunction<String> function = args -> String.format("Count: %d", args.length);
        final String result = function.apply(1L, 2L, 3L, 4L, 5L);
        assertEquals("Count: 5", result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongNFunction<String> function = args -> "test";
        assertNotNull(function.apply(1L, 2L));
    }

    @Test
    public void testMethodReference() {
        final LongNFunction<Long> function = this::sumValues;
        final Long result = function.apply(5L, 10L, 15L);
        assertEquals(30L, result);
    }

    private Long sumValues(final long... values) {
        long sum = 0;
        for (final long value : values) {
            sum += value;
        }
        return sum;
    }

    @Test
    public void testApply_complexCalculation() {
        final LongNFunction<Double> function = args -> {
            if (args.length == 0) return 0.0;
            long sumSquares = 0;
            for (final long arg : args) {
                sumSquares += arg * arg;
            }
            return Math.sqrt(sumSquares);
        };

        final Double result = function.apply(3L, 4L); // sqrt(9 + 16) = 5
        assertEquals(5.0, result, 0.001);
    }
}
