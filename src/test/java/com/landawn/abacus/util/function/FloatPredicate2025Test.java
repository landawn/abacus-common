package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        final FloatPredicate predicate = val -> val > 0;
        assertTrue(predicate.test(10.5f));
        assertFalse(predicate.test(-10.5f));
    }

    @Test
    public void testTest_withLambda() {
        final FloatPredicate predicate = val -> val > 5.0f && val < 10.0f;
        assertTrue(predicate.test(7.5f));
        assertFalse(predicate.test(12.5f));
    }

    @Test
    public void testNegate() {
        final FloatPredicate predicate = val -> val > 0;
        final FloatPredicate negated = predicate.negate();

        assertFalse(negated.test(10.5f));
        assertTrue(negated.test(-10.5f));
    }

    @Test
    public void testAnd() {
        final FloatPredicate isPositive = val -> val > 0;
        final FloatPredicate isSmall = val -> val < 100;

        final FloatPredicate combined = isPositive.and(isSmall);

        assertTrue(combined.test(10.5f));
        assertFalse(combined.test(-10.5f));
        assertFalse(combined.test(150.5f));
    }

    @Test
    public void testOr() {
        final FloatPredicate isZero = val -> val == 0;
        final FloatPredicate isNegative = val -> val < 0;

        final FloatPredicate combined = isZero.or(isNegative);

        assertTrue(combined.test(0f));
        assertTrue(combined.test(-5.3f));
        assertFalse(combined.test(5.3f));
    }

    @Test
    public void testConstant_ALWAYS_TRUE() {
        assertTrue(FloatPredicate.ALWAYS_TRUE.test(1.0f));
        assertTrue(FloatPredicate.ALWAYS_TRUE.test(-1.0f));
        assertTrue(FloatPredicate.ALWAYS_TRUE.test(0f));
    }

    @Test
    public void testConstant_ALWAYS_FALSE() {
        assertFalse(FloatPredicate.ALWAYS_FALSE.test(1.0f));
        assertFalse(FloatPredicate.ALWAYS_FALSE.test(-1.0f));
        assertFalse(FloatPredicate.ALWAYS_FALSE.test(0f));
    }

    @Test
    public void testConstant_IS_ZERO() {
        assertTrue(FloatPredicate.IS_ZERO.test(0f));
        assertFalse(FloatPredicate.IS_ZERO.test(1.0f));
    }

    @Test
    public void testConstant_NOT_ZERO() {
        assertTrue(FloatPredicate.NOT_ZERO.test(1.0f));
        assertFalse(FloatPredicate.NOT_ZERO.test(0f));
    }

    @Test
    public void testConstant_IS_POSITIVE() {
        assertTrue(FloatPredicate.IS_POSITIVE.test(1.0f));
        assertFalse(FloatPredicate.IS_POSITIVE.test(0f));
        assertFalse(FloatPredicate.IS_POSITIVE.test(-1.0f));
    }

    @Test
    public void testConstant_NOT_POSITIVE() {
        assertTrue(FloatPredicate.NOT_POSITIVE.test(0f));
        assertTrue(FloatPredicate.NOT_POSITIVE.test(-1.0f));
        assertFalse(FloatPredicate.NOT_POSITIVE.test(1.0f));
    }

    @Test
    public void testConstant_IS_NEGATIVE() {
        assertTrue(FloatPredicate.IS_NEGATIVE.test(-1.0f));
        assertFalse(FloatPredicate.IS_NEGATIVE.test(0f));
        assertFalse(FloatPredicate.IS_NEGATIVE.test(1.0f));
    }

    @Test
    public void testConstant_NOT_NEGATIVE() {
        assertTrue(FloatPredicate.NOT_NEGATIVE.test(0f));
        assertTrue(FloatPredicate.NOT_NEGATIVE.test(1.0f));
        assertFalse(FloatPredicate.NOT_NEGATIVE.test(-1.0f));
    }

    @Test
    public void testOf() {
        final FloatPredicate predicate = val -> val > 10;
        final FloatPredicate result = FloatPredicate.of(predicate);
        assertNotNull(result);
        assertTrue(result.test(20.5f));
    }

    @Test
    public void testEqual() {
        final FloatPredicate predicate = FloatPredicate.equal(42.5f);
        assertTrue(predicate.test(42.5f));
        assertFalse(predicate.test(41.5f));
    }

    @Test
    public void testNotEqual() {
        final FloatPredicate predicate = FloatPredicate.notEqual(42.5f);
        assertTrue(predicate.test(41.5f));
        assertFalse(predicate.test(42.5f));
    }

    @Test
    public void testGreaterThan() {
        final FloatPredicate predicate = FloatPredicate.greaterThan(10.0f);
        assertTrue(predicate.test(11.0f));
        assertFalse(predicate.test(10.0f));
    }

    @Test
    public void testGreaterEqual() {
        final FloatPredicate predicate = FloatPredicate.greaterEqual(10.0f);
        assertTrue(predicate.test(11.0f));
        assertTrue(predicate.test(10.0f));
        assertFalse(predicate.test(9.0f));
    }

    @Test
    public void testLessThan() {
        final FloatPredicate predicate = FloatPredicate.lessThan(10.0f);
        assertTrue(predicate.test(9.0f));
        assertFalse(predicate.test(10.0f));
    }

    @Test
    public void testLessEqual() {
        final FloatPredicate predicate = FloatPredicate.lessEqual(10.0f);
        assertTrue(predicate.test(9.0f));
        assertTrue(predicate.test(10.0f));
        assertFalse(predicate.test(11.0f));
    }

    @Test
    public void testBetween() {
        final FloatPredicate predicate = FloatPredicate.between(10.0f, 20.0f);
        assertTrue(predicate.test(15.0f));
        assertFalse(predicate.test(10.0f));   // exclusive
        assertFalse(predicate.test(20.0f));   // exclusive
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatPredicate predicate = val -> true;
        assertNotNull(predicate);
        assertTrue(predicate.test(1.0f));
    }
}
