package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteTriPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        ByteTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;

        assertTrue(allPositive.test((byte) 1, (byte) 2, (byte) 3));
        assertFalse(allPositive.test((byte) -1, (byte) 2, (byte) 3));
        assertFalse(allPositive.test((byte) 1, (byte) -2, (byte) 3));
    }

    @Test
    public void testTestWithLambda() {
        ByteTriPredicate sumGreaterThan10 = (a, b, c) -> (a + b + c) > 10;

        assertTrue(sumGreaterThan10.test((byte) 5, (byte) 4, (byte) 3));
        assertFalse(sumGreaterThan10.test((byte) 2, (byte) 2, (byte) 2));
    }

    @Test
    public void testTestWithAnonymousClass() {
        ByteTriPredicate predicate = new ByteTriPredicate() {
            @Override
            public boolean test(byte a, byte b, byte c) {
                return a == b && b == c;
            }
        };

        assertTrue(predicate.test((byte) 5, (byte) 5, (byte) 5));
        assertFalse(predicate.test((byte) 5, (byte) 5, (byte) 6));
    }

    @Test
    public void testNegate() {
        ByteTriPredicate allEqual = (a, b, c) -> a == b && b == c;
        ByteTriPredicate notAllEqual = allEqual.negate();

        assertFalse(notAllEqual.test((byte) 5, (byte) 5, (byte) 5));
        assertTrue(notAllEqual.test((byte) 5, (byte) 5, (byte) 6));
    }

    @Test
    public void testAnd() {
        ByteTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
        ByteTriPredicate sumLessThan20 = (a, b, c) -> (a + b + c) < 20;
        ByteTriPredicate combined = allPositive.and(sumLessThan20);

        assertTrue(combined.test((byte) 3, (byte) 4, (byte) 5));
        assertFalse(combined.test((byte) -1, (byte) 4, (byte) 5));
        assertFalse(combined.test((byte) 10, (byte) 10, (byte) 10));
    }

    @Test
    public void testAndShortCircuit() {
        final boolean[] secondCalled = {false};
        ByteTriPredicate alwaysFalse = (a, b, c) -> false;
        ByteTriPredicate checkCalled = (a, b, c) -> {
            secondCalled[0] = true;
            return true;
        };

        ByteTriPredicate combined = alwaysFalse.and(checkCalled);
        assertFalse(combined.test((byte) 1, (byte) 2, (byte) 3));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testOr() {
        ByteTriPredicate hasNegative = (a, b, c) -> a < 0 || b < 0 || c < 0;
        ByteTriPredicate sumGreaterThan20 = (a, b, c) -> (a + b + c) > 20;
        ByteTriPredicate combined = hasNegative.or(sumGreaterThan20);

        assertTrue(combined.test((byte) -1, (byte) 2, (byte) 3));
        assertTrue(combined.test((byte) 10, (byte) 10, (byte) 10));
        assertFalse(combined.test((byte) 2, (byte) 3, (byte) 4));
    }

    @Test
    public void testOrShortCircuit() {
        final boolean[] secondCalled = {false};
        ByteTriPredicate alwaysTrue = (a, b, c) -> true;
        ByteTriPredicate checkCalled = (a, b, c) -> {
            secondCalled[0] = true;
            return false;
        };

        ByteTriPredicate combined = alwaysTrue.or(checkCalled);
        assertTrue(combined.test((byte) 1, (byte) 2, (byte) 3));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(ByteTriPredicate.ALWAYS_TRUE.test((byte) 1, (byte) 2, (byte) 3));
        assertTrue(ByteTriPredicate.ALWAYS_TRUE.test((byte) 0, (byte) 0, (byte) 0));
        assertTrue(ByteTriPredicate.ALWAYS_TRUE.test((byte) -5, (byte) 10, (byte) 15));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(ByteTriPredicate.ALWAYS_FALSE.test((byte) 1, (byte) 2, (byte) 3));
        assertFalse(ByteTriPredicate.ALWAYS_FALSE.test((byte) 0, (byte) 0, (byte) 0));
        assertFalse(ByteTriPredicate.ALWAYS_FALSE.test((byte) -5, (byte) 10, (byte) 15));
    }

    @Test
    public void testWithBoundaryValues() {
        ByteTriPredicate allEqual = (a, b, c) -> a == b && b == c;

        assertFalse(allEqual.test(Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE));
        assertTrue(allEqual.test(Byte.MAX_VALUE, Byte.MAX_VALUE, Byte.MAX_VALUE));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteTriPredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
