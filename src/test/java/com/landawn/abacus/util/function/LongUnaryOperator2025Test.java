package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongUnaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsLong() {
        final LongUnaryOperator operator = val -> val * 2;
        final long result = operator.applyAsLong(21L);
        assertEquals(42L, result);
    }

    @Test
    public void testApplyAsLong_withLambda() {
        final LongUnaryOperator operator = val -> val + 10;
        assertEquals(20L, operator.applyAsLong(10L));
    }

    @Test
    public void testApplyAsLong_withAnonymousClass() {
        final LongUnaryOperator operator = new LongUnaryOperator() {
            @Override
            public long applyAsLong(final long operand) {
                return operand * 3;
            }
        };

        assertEquals(30L, operator.applyAsLong(10L));
    }

    @Test
    public void testCompose() {
        final LongUnaryOperator multiplyBy3 = val -> val * 3;
        final LongUnaryOperator add10 = val -> val + 10;

        final LongUnaryOperator composed = multiplyBy3.compose(add10);
        final long result = composed.applyAsLong(5L);

        assertEquals(45L, result);   // (5 + 10) * 3
    }

    @Test
    public void testAndThen() {
        final LongUnaryOperator multiplyBy2 = val -> val * 2;
        final LongUnaryOperator subtract5 = val -> val - 5;

        final LongUnaryOperator composed = multiplyBy2.andThen(subtract5);
        final long result = composed.applyAsLong(10L);

        assertEquals(15L, result);   // (10 * 2) - 5
    }

    @Test
    public void testCompose_multipleChains() {
        final LongUnaryOperator add5 = val -> val + 5;
        final LongUnaryOperator multiplyBy2 = val -> val * 2;
        final LongUnaryOperator subtract3 = val -> val - 3;

        final LongUnaryOperator composed = subtract3.compose(multiplyBy2).compose(add5);
        final long result = composed.applyAsLong(10L);

        assertEquals(27L, result);   // ((10 + 5) * 2) - 3 = 27
    }

    @Test
    public void testAndThen_multipleChains() {
        final LongUnaryOperator add5 = val -> val + 5;
        final LongUnaryOperator multiplyBy2 = val -> val * 2;
        final LongUnaryOperator subtract3 = val -> val - 3;

        final LongUnaryOperator composed = add5.andThen(multiplyBy2).andThen(subtract3);
        final long result = composed.applyAsLong(10L);

        assertEquals(27L, result);   // ((10 + 5) * 2) - 3 = 27
    }

    @Test
    public void testIdentity() {
        final LongUnaryOperator identity = LongUnaryOperator.identity();
        assertEquals(42L, identity.applyAsLong(42L));
        assertEquals(0L, identity.applyAsLong(0L));
        assertEquals(-10L, identity.applyAsLong(-10L));
    }

    @Test
    public void testApplyAsLong_increment() {
        final LongUnaryOperator increment = val -> val + 1;
        assertEquals(11L, increment.applyAsLong(10L));
    }

    @Test
    public void testApplyAsLong_decrement() {
        final LongUnaryOperator decrement = val -> val - 1;
        assertEquals(9L, decrement.applyAsLong(10L));
    }

    @Test
    public void testApplyAsLong_negate() {
        final LongUnaryOperator negate = val -> -val;
        assertEquals(-10L, negate.applyAsLong(10L));
        assertEquals(10L, negate.applyAsLong(-10L));
    }

    @Test
    public void testApplyAsLong_absolute() {
        final LongUnaryOperator absolute = Math::abs;
        assertEquals(10L, absolute.applyAsLong(-10L));
        assertEquals(10L, absolute.applyAsLong(10L));
    }

    @Test
    public void testApplyAsLong_square() {
        final LongUnaryOperator square = val -> val * val;
        assertEquals(100L, square.applyAsLong(10L));
        assertEquals(25L, square.applyAsLong(-5L));
    }

    @Test
    public void testApplyAsLong_bitwiseNot() {
        final LongUnaryOperator bitwiseNot = val -> ~val;
        assertEquals(-11L, bitwiseNot.applyAsLong(10L));
    }

    @Test
    public void testApplyAsLong_leftShift() {
        final LongUnaryOperator leftShift = val -> val << 2;
        assertEquals(40L, leftShift.applyAsLong(10L));
    }

    @Test
    public void testApplyAsLong_rightShift() {
        final LongUnaryOperator rightShift = val -> val >> 2;
        assertEquals(2L, rightShift.applyAsLong(10L));
    }

    @Test
    public void testApplyAsLong_withZero() {
        final LongUnaryOperator operator = val -> val * 2;
        assertEquals(0L, operator.applyAsLong(0L));
    }

    @Test
    public void testApplyAsLong_withNegative() {
        final LongUnaryOperator operator = val -> val + 10;
        assertEquals(0L, operator.applyAsLong(-10L));
    }

    @Test
    public void testApplyAsLong_withMaxValue() {
        final LongUnaryOperator operator = val -> val;
        assertEquals(Long.MAX_VALUE, operator.applyAsLong(Long.MAX_VALUE));
    }

    @Test
    public void testApplyAsLong_withMinValue() {
        final LongUnaryOperator operator = val -> val;
        assertEquals(Long.MIN_VALUE, operator.applyAsLong(Long.MIN_VALUE));
    }

    @Test
    public void testCompose_withIdentity() {
        final LongUnaryOperator operator = val -> val * 2;
        final LongUnaryOperator composed = operator.compose(LongUnaryOperator.identity());
        assertEquals(20L, composed.applyAsLong(10L));
    }

    @Test
    public void testAndThen_withIdentity() {
        final LongUnaryOperator operator = val -> val * 2;
        final LongUnaryOperator composed = operator.andThen(LongUnaryOperator.identity());
        assertEquals(20L, composed.applyAsLong(10L));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongUnaryOperator operator = val -> val + 1;
        assertNotNull(operator);
        assertEquals(2L, operator.applyAsLong(1L));
    }

    @Test
    public void testMethodReference() {
        final LongUnaryOperator operator = this::doubleValue;
        assertEquals(20L, operator.applyAsLong(10L));
    }

    private long doubleValue(final long value) {
        return value * 2;
    }

    @Test
    public void testCompatibilityWithJavaUtilFunction() {
        final java.util.function.LongUnaryOperator javaOperator = val -> val * 2;
        final LongUnaryOperator abacusOperator = javaOperator::applyAsLong;

        assertEquals(20L, abacusOperator.applyAsLong(10L));
    }
}
