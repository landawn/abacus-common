package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongTriPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        final LongTriPredicate predicate = (a, b, c) -> a + b + c > 100;
        assertTrue(predicate.test(40L, 50L, 20L));
        assertFalse(predicate.test(10L, 20L, 30L));
    }

    @Test
    public void testTest_withLambda() {
        final LongTriPredicate predicate = (a, b, c) -> a > 0 && b > 0 && c > 0;
        assertTrue(predicate.test(1L, 2L, 3L));
        assertFalse(predicate.test(1L, -2L, 3L));
    }

    @Test
    public void testTest_withAnonymousClass() {
        final LongTriPredicate predicate = new LongTriPredicate() {
            @Override
            public boolean test(final long a, final long b, final long c) {
                return a * b * c > 100;
            }
        };

        assertTrue(predicate.test(5L, 10L, 3L));
        assertFalse(predicate.test(2L, 3L, 4L));
    }

    @Test
    public void testNegate() {
        final LongTriPredicate predicate = (a, b, c) -> a + b + c > 100;
        final LongTriPredicate negated = predicate.negate();

        assertFalse(negated.test(40L, 50L, 20L));
        assertTrue(negated.test(10L, 20L, 30L));
    }

    @Test
    public void testAnd() {
        final LongTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
        final LongTriPredicate sumLessThan100 = (a, b, c) -> a + b + c < 100;

        final LongTriPredicate combined = allPositive.and(sumLessThan100);

        assertTrue(combined.test(10L, 20L, 30L));
        assertFalse(combined.test(40L, 50L, 20L)); // sum >= 100
        assertFalse(combined.test(-10L, 20L, 30L)); // not all positive
    }

    @Test
    public void testOr() {
        final LongTriPredicate anyZero = (a, b, c) -> a == 0 || b == 0 || c == 0;
        final LongTriPredicate sumGreaterThan1000 = (a, b, c) -> a + b + c > 1000;

        final LongTriPredicate combined = anyZero.or(sumGreaterThan1000);

        assertTrue(combined.test(0L, 50L, 50L)); // has zero
        assertTrue(combined.test(400L, 400L, 300L)); // sum > 1000
        assertFalse(combined.test(10L, 20L, 30L)); // neither
    }

    @Test
    public void testConstant_ALWAYS_TRUE() {
        assertTrue(LongTriPredicate.ALWAYS_TRUE.test(1L, 2L, 3L));
        assertTrue(LongTriPredicate.ALWAYS_TRUE.test(-1L, -2L, -3L));
        assertTrue(LongTriPredicate.ALWAYS_TRUE.test(0L, 0L, 0L));
    }

    @Test
    public void testConstant_ALWAYS_FALSE() {
        assertFalse(LongTriPredicate.ALWAYS_FALSE.test(1L, 2L, 3L));
        assertFalse(LongTriPredicate.ALWAYS_FALSE.test(-1L, -2L, -3L));
        assertFalse(LongTriPredicate.ALWAYS_FALSE.test(0L, 0L, 0L));
    }

    @Test
    public void testTriangleInequality() {
        final LongTriPredicate isTriangle = (a, b, c) ->
            a + b > c && a + c > b && b + c > a;

        assertTrue(isTriangle.test(3L, 4L, 5L));
        assertFalse(isTriangle.test(1L, 2L, 10L));
    }

    @Test
    public void testAndThen_shortCircuit() {
        final boolean[] firstCalled = { false };
        final boolean[] secondCalled = { false };

        final LongTriPredicate first = (a, b, c) -> {
            firstCalled[0] = true;
            return false;
        };
        final LongTriPredicate second = (a, b, c) -> {
            secondCalled[0] = true;
            return true;
        };

        final LongTriPredicate combined = first.and(second);
        assertFalse(combined.test(1L, 2L, 3L));

        assertTrue(firstCalled[0]);
        assertFalse(secondCalled[0]); // Should not be called due to short-circuit
    }

    @Test
    public void testOr_shortCircuit() {
        final boolean[] firstCalled = { false };
        final boolean[] secondCalled = { false };

        final LongTriPredicate first = (a, b, c) -> {
            firstCalled[0] = true;
            return true;
        };
        final LongTriPredicate second = (a, b, c) -> {
            secondCalled[0] = true;
            return false;
        };

        final LongTriPredicate combined = first.or(second);
        assertTrue(combined.test(1L, 2L, 3L));

        assertTrue(firstCalled[0]);
        assertFalse(secondCalled[0]); // Should not be called due to short-circuit
    }

    @Test
    public void testComplexChaining() {
        final LongTriPredicate predicate = LongTriPredicate.ALWAYS_TRUE
            .and((a, b, c) -> a + b + c < 100)
            .or((a, b, c) -> a == b && b == c);

        assertTrue(predicate.test(10L, 20L, 30L)); // sum < 100
        assertTrue(predicate.test(50L, 50L, 50L)); // all equal
        assertFalse(predicate.test(40L, 50L, 20L)); // sum >= 100 and not all equal
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongTriPredicate predicate = (a, b, c) -> true;
        assertNotNull(predicate);
        assertTrue(predicate.test(1L, 2L, 3L));
    }

    @Test
    public void testMethodReference() {
        final LongTriPredicate predicate = this::allPositive;
        assertTrue(predicate.test(1L, 2L, 3L));
        assertFalse(predicate.test(1L, -2L, 3L));
    }

    private boolean allPositive(final long a, final long b, final long c) {
        return a > 0 && b > 0 && c > 0;
    }
}
