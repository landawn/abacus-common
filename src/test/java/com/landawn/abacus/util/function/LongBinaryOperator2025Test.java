package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongBinaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsLong() {
        final LongBinaryOperator operator = (left, right) -> left + right;
        final long result = operator.applyAsLong(10L, 20L);
        assertEquals(30L, result);
    }

    @Test
    public void testApplyAsLong_withLambda() {
        final LongBinaryOperator operator = (a, b) -> a * b;
        final long result = operator.applyAsLong(5L, 7L);
        assertEquals(35L, result);
    }

    @Test
    public void testApplyAsLong_withAnonymousClass() {
        final LongBinaryOperator operator = new LongBinaryOperator() {
            @Override
            public long applyAsLong(final long left, final long right) {
                return left - right;
            }
        };

        final long result = operator.applyAsLong(100L, 30L);
        assertEquals(70L, result);
    }

    @Test
    public void testApplyAsLong_addition() {
        final LongBinaryOperator add = Long::sum;
        assertEquals(15L, add.applyAsLong(10L, 5L));
    }

    @Test
    public void testApplyAsLong_subtraction() {
        final LongBinaryOperator subtract = (a, b) -> a - b;
        assertEquals(5L, subtract.applyAsLong(10L, 5L));
    }

    @Test
    public void testApplyAsLong_multiplication() {
        final LongBinaryOperator multiply = (a, b) -> a * b;
        assertEquals(50L, multiply.applyAsLong(10L, 5L));
    }

    @Test
    public void testApplyAsLong_division() {
        final LongBinaryOperator divide = (a, b) -> a / b;
        assertEquals(2L, divide.applyAsLong(10L, 5L));
    }

    @Test
    public void testApplyAsLong_modulo() {
        final LongBinaryOperator modulo = (a, b) -> a % b;
        assertEquals(1L, modulo.applyAsLong(10L, 3L));
    }

    @Test
    public void testApplyAsLong_max() {
        final LongBinaryOperator max = Math::max;
        assertEquals(20L, max.applyAsLong(10L, 20L));
        assertEquals(20L, max.applyAsLong(20L, 10L));
    }

    @Test
    public void testApplyAsLong_min() {
        final LongBinaryOperator min = Math::min;
        assertEquals(10L, min.applyAsLong(10L, 20L));
        assertEquals(10L, min.applyAsLong(20L, 10L));
    }

    @Test
    public void testApplyAsLong_bitwiseAnd() {
        final LongBinaryOperator and = (a, b) -> a & b;
        assertEquals(8L, and.applyAsLong(12L, 10L));   // 1100 & 1010 = 1000
    }

    @Test
    public void testApplyAsLong_bitwiseOr() {
        final LongBinaryOperator or = (a, b) -> a | b;
        assertEquals(14L, or.applyAsLong(12L, 10L));   // 1100 | 1010 = 1110
    }

    @Test
    public void testApplyAsLong_bitwiseXor() {
        final LongBinaryOperator xor = (a, b) -> a ^ b;
        assertEquals(6L, xor.applyAsLong(12L, 10L));   // 1100 ^ 1010 = 0110
    }

    @Test
    public void testApplyAsLong_withNegativeValues() {
        final LongBinaryOperator operator = (a, b) -> a + b;
        assertEquals(-30L, operator.applyAsLong(-10L, -20L));
        assertEquals(10L, operator.applyAsLong(-10L, 20L));
    }

    @Test
    public void testApplyAsLong_withZeroValues() {
        final LongBinaryOperator operator = (a, b) -> a * b;
        assertEquals(0L, operator.applyAsLong(0L, 100L));
        assertEquals(0L, operator.applyAsLong(100L, 0L));
    }

    @Test
    public void testApplyAsLong_withMaxValue() {
        final LongBinaryOperator max = Math::max;
        assertEquals(Long.MAX_VALUE, max.applyAsLong(Long.MAX_VALUE, 0L));
        assertEquals(Long.MAX_VALUE, max.applyAsLong(Long.MAX_VALUE, Long.MIN_VALUE));
    }

    @Test
    public void testApplyAsLong_withMinValue() {
        final LongBinaryOperator min = Math::min;
        assertEquals(Long.MIN_VALUE, min.applyAsLong(Long.MIN_VALUE, 0L));
        assertEquals(Long.MIN_VALUE, min.applyAsLong(Long.MAX_VALUE, Long.MIN_VALUE));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongBinaryOperator operator = (a, b) -> a + b;
        assertNotNull(operator);
        assertEquals(30L, operator.applyAsLong(10L, 20L));
    }

    @Test
    public void testMethodReference() {
        final LongBinaryOperator operator = this::sum;
        assertEquals(15L, operator.applyAsLong(5L, 10L));
    }

    private long sum(final long a, final long b) {
        return a + b;
    }

    @Test
    public void testStreamReduction() {
        final LongBinaryOperator sum = Long::sum;
        final long[] values = { 1L, 2L, 3L, 4L, 5L };
        long result = 0L;
        for (final long value : values) {
            result = sum.applyAsLong(result, value);
        }
        assertEquals(15L, result);
    }
}
