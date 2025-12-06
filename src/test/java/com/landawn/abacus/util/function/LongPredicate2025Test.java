package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        final LongPredicate predicate = val -> val > 0;
        assertTrue(predicate.test(10L));
        assertFalse(predicate.test(-10L));
    }

    @Test
    public void testTest_withLambda() {
        final LongPredicate predicate = val -> val % 2 == 0;
        assertTrue(predicate.test(4L));
        assertFalse(predicate.test(5L));
    }

    @Test
    public void testTest_withAnonymousClass() {
        final LongPredicate predicate = new LongPredicate() {
            @Override
            public boolean test(final long value) {
                return value > 100;
            }
        };

        assertTrue(predicate.test(101L));
        assertFalse(predicate.test(99L));
    }

    @Test
    public void testNegate() {
        final LongPredicate predicate = val -> val > 0;
        final LongPredicate negated = predicate.negate();

        assertFalse(negated.test(10L));
        assertTrue(negated.test(-10L));
    }

    @Test
    public void testAnd() {
        final LongPredicate isPositive = val -> val > 0;
        final LongPredicate isEven = val -> val % 2 == 0;

        final LongPredicate combined = isPositive.and(isEven);

        assertTrue(combined.test(10L));
        assertFalse(combined.test(-10L));
        assertFalse(combined.test(5L));
    }

    @Test
    public void testOr() {
        final LongPredicate isZero = val -> val == 0;
        final LongPredicate isNegative = val -> val < 0;

        final LongPredicate combined = isZero.or(isNegative);

        assertTrue(combined.test(0L));
        assertTrue(combined.test(-5L));
        assertFalse(combined.test(5L));
    }

    @Test
    public void testConstant_ALWAYS_TRUE() {
        assertTrue(LongPredicate.ALWAYS_TRUE.test(1L));
        assertTrue(LongPredicate.ALWAYS_TRUE.test(-1L));
        assertTrue(LongPredicate.ALWAYS_TRUE.test(0L));
    }

    @Test
    public void testConstant_ALWAYS_FALSE() {
        assertFalse(LongPredicate.ALWAYS_FALSE.test(1L));
        assertFalse(LongPredicate.ALWAYS_FALSE.test(-1L));
        assertFalse(LongPredicate.ALWAYS_FALSE.test(0L));
    }

    @Test
    public void testConstant_IS_ZERO() {
        assertTrue(LongPredicate.IS_ZERO.test(0L));
        assertFalse(LongPredicate.IS_ZERO.test(1L));
        assertFalse(LongPredicate.IS_ZERO.test(-1L));
    }

    @Test
    public void testConstant_NOT_ZERO() {
        assertTrue(LongPredicate.NOT_ZERO.test(1L));
        assertTrue(LongPredicate.NOT_ZERO.test(-1L));
        assertFalse(LongPredicate.NOT_ZERO.test(0L));
    }

    @Test
    public void testConstant_IS_POSITIVE() {
        assertTrue(LongPredicate.IS_POSITIVE.test(1L));
        assertFalse(LongPredicate.IS_POSITIVE.test(0L));
        assertFalse(LongPredicate.IS_POSITIVE.test(-1L));
    }

    @Test
    public void testConstant_NOT_POSITIVE() {
        assertTrue(LongPredicate.NOT_POSITIVE.test(0L));
        assertTrue(LongPredicate.NOT_POSITIVE.test(-1L));
        assertFalse(LongPredicate.NOT_POSITIVE.test(1L));
    }

    @Test
    public void testConstant_IS_NEGATIVE() {
        assertTrue(LongPredicate.IS_NEGATIVE.test(-1L));
        assertFalse(LongPredicate.IS_NEGATIVE.test(0L));
        assertFalse(LongPredicate.IS_NEGATIVE.test(1L));
    }

    @Test
    public void testConstant_NOT_NEGATIVE() {
        assertTrue(LongPredicate.NOT_NEGATIVE.test(0L));
        assertTrue(LongPredicate.NOT_NEGATIVE.test(1L));
        assertFalse(LongPredicate.NOT_NEGATIVE.test(-1L));
    }

    @Test
    public void testOf() {
        final LongPredicate predicate = val -> val > 10;
        final LongPredicate result = LongPredicate.of(predicate);
        assertNotNull(result);
        assertTrue(result.test(20L));
    }

    @Test
    public void testEqual() {
        final LongPredicate predicate = LongPredicate.equal(42L);
        assertTrue(predicate.test(42L));
        assertFalse(predicate.test(41L));
    }

    @Test
    public void testNotEqual() {
        final LongPredicate predicate = LongPredicate.notEqual(42L);
        assertTrue(predicate.test(41L));
        assertFalse(predicate.test(42L));
    }

    @Test
    public void testGreaterThan() {
        final LongPredicate predicate = LongPredicate.greaterThan(10L);
        assertTrue(predicate.test(11L));
        assertFalse(predicate.test(10L));
        assertFalse(predicate.test(9L));
    }

    @Test
    public void testGreaterEqual() {
        final LongPredicate predicate = LongPredicate.greaterEqual(10L);
        assertTrue(predicate.test(11L));
        assertTrue(predicate.test(10L));
        assertFalse(predicate.test(9L));
    }

    @Test
    public void testLessThan() {
        final LongPredicate predicate = LongPredicate.lessThan(10L);
        assertTrue(predicate.test(9L));
        assertFalse(predicate.test(10L));
        assertFalse(predicate.test(11L));
    }

    @Test
    public void testLessEqual() {
        final LongPredicate predicate = LongPredicate.lessEqual(10L);
        assertTrue(predicate.test(9L));
        assertTrue(predicate.test(10L));
        assertFalse(predicate.test(11L));
    }

    @Test
    public void testBetween() {
        final LongPredicate predicate = LongPredicate.between(10L, 20L);
        assertTrue(predicate.test(15L));
        assertFalse(predicate.test(10L));   // exclusive
        assertFalse(predicate.test(20L));   // exclusive
        assertFalse(predicate.test(5L));
        assertFalse(predicate.test(25L));
    }

    @Test
    public void testAndThen_shortCircuit() {
        final boolean[] firstCalled = { false };
        final boolean[] secondCalled = { false };

        final LongPredicate first = val -> {
            firstCalled[0] = true;
            return false;
        };
        final LongPredicate second = val -> {
            secondCalled[0] = true;
            return true;
        };

        final LongPredicate combined = first.and(second);
        assertFalse(combined.test(1L));

        assertTrue(firstCalled[0]);
        assertFalse(secondCalled[0]);   // Should not be called due to short-circuit
    }

    @Test
    public void testOr_shortCircuit() {
        final boolean[] firstCalled = { false };
        final boolean[] secondCalled = { false };

        final LongPredicate first = val -> {
            firstCalled[0] = true;
            return true;
        };
        final LongPredicate second = val -> {
            secondCalled[0] = true;
            return false;
        };

        final LongPredicate combined = first.or(second);
        assertTrue(combined.test(1L));

        assertTrue(firstCalled[0]);
        assertFalse(secondCalled[0]);   // Should not be called due to short-circuit
    }

    @Test
    public void testComplexChaining() {
        final LongPredicate predicate = LongPredicate.IS_POSITIVE.and(val -> val < 100).or(LongPredicate.IS_ZERO);

        assertTrue(predicate.test(50L));   // positive and < 100
        assertTrue(predicate.test(0L));   // is zero
        assertFalse(predicate.test(150L));   // positive but >= 100
        assertFalse(predicate.test(-10L));   // negative
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongPredicate predicate = val -> true;
        assertNotNull(predicate);
        assertTrue(predicate.test(1L));
    }

    @Test
    public void testMethodReference() {
        final LongPredicate predicate = this::isEven;
        assertTrue(predicate.test(4L));
        assertFalse(predicate.test(5L));
    }

    private boolean isEven(final long value) {
        return value % 2 == 0;
    }

    @Test
    public void testCompatibilityWithJavaUtilFunction() {
        final java.util.function.LongPredicate javaPredicate = val -> val > 0;
        final LongPredicate abacusPredicate = javaPredicate::test;

        assertTrue(abacusPredicate.test(10L));
        assertFalse(abacusPredicate.test(-10L));
    }
}
