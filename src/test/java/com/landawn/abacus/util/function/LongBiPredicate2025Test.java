package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongBiPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        final LongBiPredicate predicate = (t, u) -> t > u;
        assertTrue(predicate.test(10L, 5L));
        assertFalse(predicate.test(5L, 10L));
    }

    @Test
    public void testTest_withLambda() {
        final LongBiPredicate predicate = (a, b) -> a == b;
        assertTrue(predicate.test(5L, 5L));
        assertFalse(predicate.test(5L, 7L));
    }

    @Test
    public void testTest_withAnonymousClass() {
        final LongBiPredicate predicate = new LongBiPredicate() {
            @Override
            public boolean test(final long t, final long u) {
                return t + u > 100L;
            }
        };

        assertTrue(predicate.test(60L, 50L));
        assertFalse(predicate.test(40L, 50L));
    }

    @Test
    public void testNegate() {
        final LongBiPredicate predicate = (t, u) -> t > u;
        final LongBiPredicate negated = predicate.negate();

        assertFalse(negated.test(10L, 5L));
        assertTrue(negated.test(5L, 10L));
    }

    @Test
    public void testAnd() {
        final LongBiPredicate isPositive = (t, u) -> t > 0 && u > 0;
        final LongBiPredicate sumLessThan100 = (t, u) -> t + u < 100;

        final LongBiPredicate combined = isPositive.and(sumLessThan100);

        assertTrue(combined.test(10L, 20L));
        assertFalse(combined.test(10L, 100L));   // sum not less than 100
        assertFalse(combined.test(-10L, 20L));   // not all positive
    }

    @Test
    public void testOr() {
        final LongBiPredicate isZero = (t, u) -> t == 0 || u == 0;
        final LongBiPredicate isNegative = (t, u) -> t < 0 || u < 0;

        final LongBiPredicate combined = isZero.or(isNegative);

        assertTrue(combined.test(0L, 10L));   // has zero
        assertTrue(combined.test(-5L, 10L));   // has negative
        assertFalse(combined.test(5L, 10L));   // neither
    }

    @Test
    public void testConstant_ALWAYS_TRUE() {
        assertTrue(LongBiPredicate.ALWAYS_TRUE.test(1L, 2L));
        assertTrue(LongBiPredicate.ALWAYS_TRUE.test(-1L, -2L));
        assertTrue(LongBiPredicate.ALWAYS_TRUE.test(0L, 0L));
    }

    @Test
    public void testConstant_ALWAYS_FALSE() {
        assertFalse(LongBiPredicate.ALWAYS_FALSE.test(1L, 2L));
        assertFalse(LongBiPredicate.ALWAYS_FALSE.test(-1L, -2L));
        assertFalse(LongBiPredicate.ALWAYS_FALSE.test(0L, 0L));
    }

    @Test
    public void testConstant_EQUAL() {
        assertTrue(LongBiPredicate.EQUAL.test(5L, 5L));
        assertFalse(LongBiPredicate.EQUAL.test(5L, 6L));
    }

    @Test
    public void testConstant_NOT_EQUAL() {
        assertTrue(LongBiPredicate.NOT_EQUAL.test(5L, 6L));
        assertFalse(LongBiPredicate.NOT_EQUAL.test(5L, 5L));
    }

    @Test
    public void testConstant_GREATER_THAN() {
        assertTrue(LongBiPredicate.GREATER_THAN.test(10L, 5L));
        assertFalse(LongBiPredicate.GREATER_THAN.test(5L, 10L));
        assertFalse(LongBiPredicate.GREATER_THAN.test(5L, 5L));
    }

    @Test
    public void testConstant_GREATER_EQUAL() {
        assertTrue(LongBiPredicate.GREATER_EQUAL.test(10L, 5L));
        assertTrue(LongBiPredicate.GREATER_EQUAL.test(5L, 5L));
        assertFalse(LongBiPredicate.GREATER_EQUAL.test(5L, 10L));
    }

    @Test
    public void testConstant_LESS_THAN() {
        assertTrue(LongBiPredicate.LESS_THAN.test(5L, 10L));
        assertFalse(LongBiPredicate.LESS_THAN.test(10L, 5L));
        assertFalse(LongBiPredicate.LESS_THAN.test(5L, 5L));
    }

    @Test
    public void testConstant_LESS_EQUAL() {
        assertTrue(LongBiPredicate.LESS_EQUAL.test(5L, 10L));
        assertTrue(LongBiPredicate.LESS_EQUAL.test(5L, 5L));
        assertFalse(LongBiPredicate.LESS_EQUAL.test(10L, 5L));
    }

    @Test
    public void testAndThen_shortCircuit() {
        final boolean[] firstCalled = { false };
        final boolean[] secondCalled = { false };

        final LongBiPredicate first = (t, u) -> {
            firstCalled[0] = true;
            return false;
        };
        final LongBiPredicate second = (t, u) -> {
            secondCalled[0] = true;
            return true;
        };

        final LongBiPredicate combined = first.and(second);
        assertFalse(combined.test(1L, 2L));

        assertTrue(firstCalled[0]);
        assertFalse(secondCalled[0]);   // Should not be called due to short-circuit
    }

    @Test
    public void testOr_shortCircuit() {
        final boolean[] firstCalled = { false };
        final boolean[] secondCalled = { false };

        final LongBiPredicate first = (t, u) -> {
            firstCalled[0] = true;
            return true;
        };
        final LongBiPredicate second = (t, u) -> {
            secondCalled[0] = true;
            return false;
        };

        final LongBiPredicate combined = first.or(second);
        assertTrue(combined.test(1L, 2L));

        assertTrue(firstCalled[0]);
        assertFalse(secondCalled[0]);   // Should not be called due to short-circuit
    }

    @Test
    public void testComplexChaining() {
        final LongBiPredicate predicate = LongBiPredicate.GREATER_THAN.and((t, u) -> t + u < 100).or((t, u) -> t == u);

        assertTrue(predicate.test(10L, 5L));   // 10 > 5 AND 15 < 100
        assertTrue(predicate.test(5L, 5L));   // 5 == 5
        assertFalse(predicate.test(5L, 10L));   // 5 < 10 AND 5 != 10
    }

    @Test
    public void testWithNegativeValues() {
        final LongBiPredicate predicate = (t, u) -> Math.abs(t - u) < 10;
        assertTrue(predicate.test(-5L, -2L));
        assertFalse(predicate.test(-20L, -5L));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongBiPredicate predicate = (t, u) -> true;
        assertNotNull(predicate);
        assertTrue(predicate.test(1L, 2L));
    }
}
