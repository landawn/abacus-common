package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BytePredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        BytePredicate isPositive = b -> b > 0;

        assertTrue(isPositive.test((byte) 5));
        assertFalse(isPositive.test((byte) -5));
        assertFalse(isPositive.test((byte) 0));
    }

    @Test
    public void testTestWithLambda() {
        BytePredicate isEven = b -> b % 2 == 0;

        assertTrue(isEven.test((byte) 4));
        assertFalse(isEven.test((byte) 3));
        assertTrue(isEven.test((byte) 0));
    }

    @Test
    public void testTestWithAnonymousClass() {
        BytePredicate greaterThan10 = new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return value > 10;
            }
        };

        assertTrue(greaterThan10.test((byte) 15));
        assertFalse(greaterThan10.test((byte) 5));
    }

    @Test
    public void testOf() {
        BytePredicate original = b -> b > 0;
        BytePredicate returned = BytePredicate.of(original);

        assertSame(original, returned);
    }

    @Test
    public void testNegate() {
        BytePredicate isPositive = b -> b > 0;
        BytePredicate isNotPositive = isPositive.negate();

        assertFalse(isNotPositive.test((byte) 5));
        assertTrue(isNotPositive.test((byte) -5));
        assertTrue(isNotPositive.test((byte) 0));
    }

    @Test
    public void testAnd() {
        BytePredicate isPositive = b -> b > 0;
        BytePredicate lessThan10 = b -> b < 10;
        BytePredicate combined = isPositive.and(lessThan10);

        assertTrue(combined.test((byte) 5));
        assertFalse(combined.test((byte) -5));
        assertFalse(combined.test((byte) 15));
    }

    @Test
    public void testAndShortCircuit() {
        final boolean[] secondCalled = { false };
        BytePredicate alwaysFalse = b -> false;
        BytePredicate checkCalled = b -> {
            secondCalled[0] = true;
            return true;
        };

        BytePredicate combined = alwaysFalse.and(checkCalled);
        assertFalse(combined.test((byte) 5));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testOr() {
        BytePredicate isNegative = b -> b < 0;
        BytePredicate isGreaterThan10 = b -> b > 10;
        BytePredicate combined = isNegative.or(isGreaterThan10);

        assertTrue(combined.test((byte) -5));
        assertTrue(combined.test((byte) 15));
        assertFalse(combined.test((byte) 5));
    }

    @Test
    public void testOrShortCircuit() {
        final boolean[] secondCalled = { false };
        BytePredicate alwaysTrue = b -> true;
        BytePredicate checkCalled = b -> {
            secondCalled[0] = true;
            return false;
        };

        BytePredicate combined = alwaysTrue.or(checkCalled);
        assertTrue(combined.test((byte) 5));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(BytePredicate.ALWAYS_TRUE.test((byte) 5));
        assertTrue(BytePredicate.ALWAYS_TRUE.test((byte) -5));
        assertTrue(BytePredicate.ALWAYS_TRUE.test((byte) 0));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(BytePredicate.ALWAYS_FALSE.test((byte) 5));
        assertFalse(BytePredicate.ALWAYS_FALSE.test((byte) -5));
        assertFalse(BytePredicate.ALWAYS_FALSE.test((byte) 0));
    }

    @Test
    public void testIsZero() {
        assertTrue(BytePredicate.IS_ZERO.test((byte) 0));
        assertFalse(BytePredicate.IS_ZERO.test((byte) 5));
        assertFalse(BytePredicate.IS_ZERO.test((byte) -5));
    }

    @Test
    public void testNotZero() {
        assertFalse(BytePredicate.NOT_ZERO.test((byte) 0));
        assertTrue(BytePredicate.NOT_ZERO.test((byte) 5));
        assertTrue(BytePredicate.NOT_ZERO.test((byte) -5));
    }

    @Test
    public void testIsPositive() {
        assertTrue(BytePredicate.IS_POSITIVE.test((byte) 5));
        assertFalse(BytePredicate.IS_POSITIVE.test((byte) 0));
        assertFalse(BytePredicate.IS_POSITIVE.test((byte) -5));
    }

    @Test
    public void testNotPositive() {
        assertFalse(BytePredicate.NOT_POSITIVE.test((byte) 5));
        assertTrue(BytePredicate.NOT_POSITIVE.test((byte) 0));
        assertTrue(BytePredicate.NOT_POSITIVE.test((byte) -5));
    }

    @Test
    public void testIsNegative() {
        assertFalse(BytePredicate.IS_NEGATIVE.test((byte) 5));
        assertFalse(BytePredicate.IS_NEGATIVE.test((byte) 0));
        assertTrue(BytePredicate.IS_NEGATIVE.test((byte) -5));
    }

    @Test
    public void testNotNegative() {
        assertTrue(BytePredicate.NOT_NEGATIVE.test((byte) 5));
        assertTrue(BytePredicate.NOT_NEGATIVE.test((byte) 0));
        assertFalse(BytePredicate.NOT_NEGATIVE.test((byte) -5));
    }

    @Test
    public void testEqual() {
        BytePredicate equalTo5 = BytePredicate.equal((byte) 5);

        assertTrue(equalTo5.test((byte) 5));
        assertFalse(equalTo5.test((byte) 6));
        assertFalse(equalTo5.test((byte) -5));
    }

    @Test
    public void testNotEqual() {
        BytePredicate notEqualTo5 = BytePredicate.notEqual((byte) 5);

        assertFalse(notEqualTo5.test((byte) 5));
        assertTrue(notEqualTo5.test((byte) 6));
        assertTrue(notEqualTo5.test((byte) -5));
    }

    @Test
    public void testGreaterThan() {
        BytePredicate greaterThan5 = BytePredicate.greaterThan((byte) 5);

        assertTrue(greaterThan5.test((byte) 10));
        assertFalse(greaterThan5.test((byte) 5));
        assertFalse(greaterThan5.test((byte) 3));
    }

    @Test
    public void testGreaterEqual() {
        BytePredicate greaterEqual5 = BytePredicate.greaterEqual((byte) 5);

        assertTrue(greaterEqual5.test((byte) 10));
        assertTrue(greaterEqual5.test((byte) 5));
        assertFalse(greaterEqual5.test((byte) 3));
    }

    @Test
    public void testLessThan() {
        BytePredicate lessThan5 = BytePredicate.lessThan((byte) 5);

        assertFalse(lessThan5.test((byte) 10));
        assertFalse(lessThan5.test((byte) 5));
        assertTrue(lessThan5.test((byte) 3));
    }

    @Test
    public void testLessEqual() {
        BytePredicate lessEqual5 = BytePredicate.lessEqual((byte) 5);

        assertFalse(lessEqual5.test((byte) 10));
        assertTrue(lessEqual5.test((byte) 5));
        assertTrue(lessEqual5.test((byte) 3));
    }

    @Test
    public void testBetween() {
        BytePredicate between5And10 = BytePredicate.between((byte) 5, (byte) 10);

        assertTrue(between5And10.test((byte) 7));
        assertFalse(between5And10.test((byte) 5));
        assertFalse(between5And10.test((byte) 10));
        assertFalse(between5And10.test((byte) 3));
        assertFalse(between5And10.test((byte) 12));
    }

    @Test
    public void testBoundaryValues() {
        BytePredicate isMax = BytePredicate.equal(Byte.MAX_VALUE);
        BytePredicate isMin = BytePredicate.equal(Byte.MIN_VALUE);

        assertTrue(isMax.test(Byte.MAX_VALUE));
        assertTrue(isMin.test(Byte.MIN_VALUE));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(BytePredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
