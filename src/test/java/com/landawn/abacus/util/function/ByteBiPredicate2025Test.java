package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteBiPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        ByteBiPredicate predicate = (t, u) -> t > u;

        assertTrue(predicate.test((byte) 10, (byte) 5));
        assertFalse(predicate.test((byte) 5, (byte) 10));
        assertFalse(predicate.test((byte) 5, (byte) 5));
    }

    @Test
    public void testTestWithLambda() {
        ByteBiPredicate sumGreaterThan10 = (t, u) -> (t + u) > 10;

        assertTrue(sumGreaterThan10.test((byte) 6, (byte) 5));
        assertFalse(sumGreaterThan10.test((byte) 3, (byte) 4));
    }

    @Test
    public void testTestWithAnonymousClass() {
        ByteBiPredicate predicate = new ByteBiPredicate() {
            @Override
            public boolean test(byte t, byte u) {
                return t == u;
            }
        };

        assertTrue(predicate.test((byte) 5, (byte) 5));
        assertFalse(predicate.test((byte) 5, (byte) 6));
    }

    @Test
    public void testNegate() {
        ByteBiPredicate equal = (t, u) -> t == u;
        ByteBiPredicate notEqual = equal.negate();

        assertFalse(notEqual.test((byte) 5, (byte) 5));
        assertTrue(notEqual.test((byte) 5, (byte) 6));
    }

    @Test
    public void testAnd() {
        ByteBiPredicate firstGreater = (t, u) -> t > u;
        ByteBiPredicate sumLessThan20 = (t, u) -> (t + u) < 20;
        ByteBiPredicate combined = firstGreater.and(sumLessThan20);

        assertTrue(combined.test((byte) 10, (byte) 5));
        assertFalse(combined.test((byte) 5, (byte) 10));
        assertFalse(combined.test((byte) 15, (byte) 10));
    }

    @Test
    public void testAndShortCircuit() {
        final boolean[] secondCalled = { false };
        ByteBiPredicate alwaysFalse = (t, u) -> false;
        ByteBiPredicate checkCalled = (t, u) -> {
            secondCalled[0] = true;
            return true;
        };

        ByteBiPredicate combined = alwaysFalse.and(checkCalled);
        assertFalse(combined.test((byte) 5, (byte) 3));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testOr() {
        ByteBiPredicate equal = (t, u) -> t == u;
        ByteBiPredicate sumGreaterThan10 = (t, u) -> (t + u) > 10;
        ByteBiPredicate combined = equal.or(sumGreaterThan10);

        assertTrue(combined.test((byte) 5, (byte) 5));
        assertTrue(combined.test((byte) 6, (byte) 5));
        assertFalse(combined.test((byte) 3, (byte) 4));
    }

    @Test
    public void testOrShortCircuit() {
        final boolean[] secondCalled = { false };
        ByteBiPredicate alwaysTrue = (t, u) -> true;
        ByteBiPredicate checkCalled = (t, u) -> {
            secondCalled[0] = true;
            return false;
        };

        ByteBiPredicate combined = alwaysTrue.or(checkCalled);
        assertTrue(combined.test((byte) 5, (byte) 3));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(ByteBiPredicate.ALWAYS_TRUE.test((byte) 5, (byte) 10));
        assertTrue(ByteBiPredicate.ALWAYS_TRUE.test((byte) 0, (byte) 0));
        assertTrue(ByteBiPredicate.ALWAYS_TRUE.test((byte) -5, (byte) 5));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(ByteBiPredicate.ALWAYS_FALSE.test((byte) 5, (byte) 10));
        assertFalse(ByteBiPredicate.ALWAYS_FALSE.test((byte) 0, (byte) 0));
        assertFalse(ByteBiPredicate.ALWAYS_FALSE.test((byte) -5, (byte) 5));
    }

    @Test
    public void testEqual() {
        assertTrue(ByteBiPredicate.EQUAL.test((byte) 5, (byte) 5));
        assertFalse(ByteBiPredicate.EQUAL.test((byte) 5, (byte) 6));
        assertTrue(ByteBiPredicate.EQUAL.test((byte) 0, (byte) 0));
    }

    @Test
    public void testNotEqual() {
        assertFalse(ByteBiPredicate.NOT_EQUAL.test((byte) 5, (byte) 5));
        assertTrue(ByteBiPredicate.NOT_EQUAL.test((byte) 5, (byte) 6));
        assertFalse(ByteBiPredicate.NOT_EQUAL.test((byte) 0, (byte) 0));
    }

    @Test
    public void testGreaterThan() {
        assertTrue(ByteBiPredicate.GREATER_THAN.test((byte) 10, (byte) 5));
        assertFalse(ByteBiPredicate.GREATER_THAN.test((byte) 5, (byte) 10));
        assertFalse(ByteBiPredicate.GREATER_THAN.test((byte) 5, (byte) 5));
    }

    @Test
    public void testGreaterEqual() {
        assertTrue(ByteBiPredicate.GREATER_EQUAL.test((byte) 10, (byte) 5));
        assertFalse(ByteBiPredicate.GREATER_EQUAL.test((byte) 5, (byte) 10));
        assertTrue(ByteBiPredicate.GREATER_EQUAL.test((byte) 5, (byte) 5));
    }

    @Test
    public void testLessThan() {
        assertFalse(ByteBiPredicate.LESS_THAN.test((byte) 10, (byte) 5));
        assertTrue(ByteBiPredicate.LESS_THAN.test((byte) 5, (byte) 10));
        assertFalse(ByteBiPredicate.LESS_THAN.test((byte) 5, (byte) 5));
    }

    @Test
    public void testLessEqual() {
        assertFalse(ByteBiPredicate.LESS_EQUAL.test((byte) 10, (byte) 5));
        assertTrue(ByteBiPredicate.LESS_EQUAL.test((byte) 5, (byte) 10));
        assertTrue(ByteBiPredicate.LESS_EQUAL.test((byte) 5, (byte) 5));
    }

    @Test
    public void testWithBoundaryValues() {
        assertTrue(ByteBiPredicate.EQUAL.test(Byte.MAX_VALUE, Byte.MAX_VALUE));
        assertTrue(ByteBiPredicate.EQUAL.test(Byte.MIN_VALUE, Byte.MIN_VALUE));
        assertTrue(ByteBiPredicate.LESS_THAN.test(Byte.MIN_VALUE, Byte.MAX_VALUE));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteBiPredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
