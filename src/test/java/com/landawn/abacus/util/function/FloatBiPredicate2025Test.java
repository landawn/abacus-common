package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatBiPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        final FloatBiPredicate predicate = (t, u) -> t > u;
        assertTrue(predicate.test(10.5f, 5.3f));
        assertFalse(predicate.test(5.3f, 10.5f));
    }

    @Test
    public void testTest_withLambda() {
        final FloatBiPredicate predicate = (a, b) -> Math.abs(a - b) < 0.1f;
        assertTrue(predicate.test(5.0f, 5.05f));
        assertFalse(predicate.test(5.0f, 6.0f));
    }

    @Test
    public void testNegate() {
        final FloatBiPredicate predicate = (t, u) -> t > u;
        final FloatBiPredicate negated = predicate.negate();

        assertFalse(negated.test(10.5f, 5.3f));
        assertTrue(negated.test(5.3f, 10.5f));
    }

    @Test
    public void testAnd() {
        final FloatBiPredicate isPositive = (t, u) -> t > 0 && u > 0;
        final FloatBiPredicate sumLessThan100 = (t, u) -> t + u < 100;

        final FloatBiPredicate combined = isPositive.and(sumLessThan100);

        assertTrue(combined.test(10.5f, 20.3f));
        assertFalse(combined.test(10.5f, 100.0f));
        assertFalse(combined.test(-10.5f, 20.3f));
    }

    @Test
    public void testOr() {
        final FloatBiPredicate isZero = (t, u) -> t == 0 || u == 0;
        final FloatBiPredicate isNegative = (t, u) -> t < 0 || u < 0;

        final FloatBiPredicate combined = isZero.or(isNegative);

        assertTrue(combined.test(0f, 10.5f));
        assertTrue(combined.test(-5.3f, 10.5f));
        assertFalse(combined.test(5.3f, 10.5f));
    }

    @Test
    public void testConstant_ALWAYS_TRUE() {
        assertTrue(FloatBiPredicate.ALWAYS_TRUE.test(1.0f, 2.0f));
        assertTrue(FloatBiPredicate.ALWAYS_TRUE.test(-1.0f, -2.0f));
    }

    @Test
    public void testConstant_ALWAYS_FALSE() {
        assertFalse(FloatBiPredicate.ALWAYS_FALSE.test(1.0f, 2.0f));
        assertFalse(FloatBiPredicate.ALWAYS_FALSE.test(-1.0f, -2.0f));
    }
}
