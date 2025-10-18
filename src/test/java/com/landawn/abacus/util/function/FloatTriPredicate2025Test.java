package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatTriPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        final FloatTriPredicate predicate = (a, b, c) -> a + b + c > 100;
        assertTrue(predicate.test(40.0f, 50.0f, 20.0f));
        assertFalse(predicate.test(10.0f, 20.0f, 30.0f));
    }

    @Test
    public void testNegate() {
        final FloatTriPredicate predicate = (a, b, c) -> a + b + c > 100;
        final FloatTriPredicate negated = predicate.negate();

        assertFalse(negated.test(40.0f, 50.0f, 20.0f));
        assertTrue(negated.test(10.0f, 20.0f, 30.0f));
    }

    @Test
    public void testAnd() {
        final FloatTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
        final FloatTriPredicate sumLessThan100 = (a, b, c) -> a + b + c < 100;

        final FloatTriPredicate combined = allPositive.and(sumLessThan100);

        assertTrue(combined.test(10.0f, 20.0f, 30.0f));
        assertFalse(combined.test(40.0f, 50.0f, 20.0f));
    }

    @Test
    public void testOr() {
        final FloatTriPredicate anyZero = (a, b, c) -> a == 0 || b == 0 || c == 0;
        final FloatTriPredicate sumGreaterThan1000 = (a, b, c) -> a + b + c > 1000;

        final FloatTriPredicate combined = anyZero.or(sumGreaterThan1000);

        assertTrue(combined.test(0f, 50.0f, 50.0f));
        assertFalse(combined.test(10.0f, 20.0f, 30.0f));
    }

    @Test
    public void testConstant_ALWAYS_TRUE() {
        assertTrue(FloatTriPredicate.ALWAYS_TRUE.test(1.0f, 2.0f, 3.0f));
    }

    @Test
    public void testConstant_ALWAYS_FALSE() {
        assertFalse(FloatTriPredicate.ALWAYS_FALSE.test(1.0f, 2.0f, 3.0f));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatTriPredicate predicate = (a, b, c) -> true;
        assertNotNull(predicate);
        assertTrue(predicate.test(1.0f, 2.0f, 3.0f));
    }
}
