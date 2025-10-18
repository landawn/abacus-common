package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongTernaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsLong() {
        final LongTernaryOperator operator = (a, b, c) -> a + b + c;
        final long result = operator.applyAsLong(10L, 20L, 30L);
        assertEquals(60L, result);
    }

    @Test
    public void testApplyAsLong_withLambda() {
        final LongTernaryOperator operator = (a, b, c) -> a * b * c;
        final long result = operator.applyAsLong(2L, 3L, 4L);
        assertEquals(24L, result);
    }

    @Test
    public void testApplyAsLong_withAnonymousClass() {
        final LongTernaryOperator operator = new LongTernaryOperator() {
            @Override
            public long applyAsLong(final long a, final long b, final long c) {
                return (a + b + c) / 3;
            }
        };

        final long result = operator.applyAsLong(10L, 20L, 30L);
        assertEquals(20L, result);
    }

    @Test
    public void testApplyAsLong_sum() {
        final LongTernaryOperator sum = (a, b, c) -> a + b + c;
        assertEquals(60L, sum.applyAsLong(10L, 20L, 30L));
    }

    @Test
    public void testApplyAsLong_product() {
        final LongTernaryOperator product = (a, b, c) -> a * b * c;
        assertEquals(120L, product.applyAsLong(4L, 5L, 6L));
    }

    @Test
    public void testApplyAsLong_max() {
        final LongTernaryOperator max = (a, b, c) -> Math.max(a, Math.max(b, c));
        assertEquals(100L, max.applyAsLong(5L, 100L, 50L));
    }

    @Test
    public void testApplyAsLong_min() {
        final LongTernaryOperator min = (a, b, c) -> Math.min(a, Math.min(b, c));
        assertEquals(5L, min.applyAsLong(5L, 100L, 50L));
    }

    @Test
    public void testApplyAsLong_median() {
        final LongTernaryOperator median = (a, b, c) -> {
            if ((a >= b && a <= c) || (a <= b && a >= c)) return a;
            if ((b >= a && b <= c) || (b <= a && b >= c)) return b;
            return c;
        };
        assertEquals(50L, median.applyAsLong(5L, 100L, 50L));
    }

    @Test
    public void testApplyAsLong_withNegativeValues() {
        final LongTernaryOperator operator = (a, b, c) -> a + b + c;
        assertEquals(-60L, operator.applyAsLong(-10L, -20L, -30L));
    }

    @Test
    public void testApplyAsLong_withZeroValues() {
        final LongTernaryOperator operator = (a, b, c) -> a * b * c;
        assertEquals(0L, operator.applyAsLong(0L, 5L, 10L));
    }

    @Test
    public void testApplyAsLong_bitwiseAnd() {
        final LongTernaryOperator bitwiseAnd = (a, b, c) -> a & b & c;
        assertEquals(0L, bitwiseAnd.applyAsLong(12L, 10L, 5L));
    }

    @Test
    public void testApplyAsLong_bitwiseOr() {
        final LongTernaryOperator bitwiseOr = (a, b, c) -> a | b | c;
        assertEquals(15L, bitwiseOr.applyAsLong(12L, 10L, 5L));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongTernaryOperator operator = (a, b, c) -> a + b + c;
        assertNotNull(operator);
        assertEquals(60L, operator.applyAsLong(10L, 20L, 30L));
    }

    @Test
    public void testMethodReference() {
        final LongTernaryOperator operator = this::sumThree;
        assertEquals(30L, operator.applyAsLong(5L, 10L, 15L));
    }

    private long sumThree(final long a, final long b, final long c) {
        return a + b + c;
    }
}
