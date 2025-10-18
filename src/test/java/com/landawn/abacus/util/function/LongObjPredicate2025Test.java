package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongObjPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        LongObjPredicate<String> predicate = (l, s) -> l > 5L && s.length() > 3;

        assertTrue(predicate.test(10L, "test"));
        assertFalse(predicate.test(3L, "test"));
        assertFalse(predicate.test(10L, "ab"));
        assertFalse(predicate.test(3L, "ab"));
    }

    @Test
    public void testTestWithLambda() {
        LongObjPredicate<Integer> predicate = (l, i) -> l > i;

        assertTrue(predicate.test(10L, 5));
        assertFalse(predicate.test(3L, 10));
        assertFalse(predicate.test(5L, 5));
    }

    @Test
    public void testTestWithAnonymousClass() {
        LongObjPredicate<String> predicate = new LongObjPredicate<String>() {
            @Override
            public boolean test(long t, String u) {
                return t >= 0 && u != null && !u.isEmpty();
            }
        };

        assertTrue(predicate.test(5L, "test"));
        assertFalse(predicate.test(-1L, "test"));
        assertFalse(predicate.test(5L, ""));
    }

    @Test
    public void testNegate() {
        LongObjPredicate<String> predicate = (l, s) -> l > 5L;
        LongObjPredicate<String> negated = predicate.negate();

        assertFalse(negated.test(10L, "test"));
        assertTrue(negated.test(3L, "test"));
    }

    @Test
    public void testAnd() {
        LongObjPredicate<String> predicate1 = (l, s) -> l > 5L;
        LongObjPredicate<String> predicate2 = (l, s) -> s.length() > 3;

        LongObjPredicate<String> combined = predicate1.and(predicate2);

        assertTrue(combined.test(10L, "test"));
        assertFalse(combined.test(3L, "test"));
        assertFalse(combined.test(10L, "ab"));
        assertFalse(combined.test(3L, "ab"));
    }

    @Test
    public void testAndShortCircuit() {
        final boolean[] secondCalled = {false};
        LongObjPredicate<String> predicate1 = (l, s) -> false;
        LongObjPredicate<String> predicate2 = (l, s) -> {
            secondCalled[0] = true;
            return true;
        };

        LongObjPredicate<String> combined = predicate1.and(predicate2);
        assertFalse(combined.test(5L, "test"));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testOr() {
        LongObjPredicate<String> predicate1 = (l, s) -> l > 5L;
        LongObjPredicate<String> predicate2 = (l, s) -> s.length() > 3;

        LongObjPredicate<String> combined = predicate1.or(predicate2);

        assertTrue(combined.test(10L, "test"));
        assertTrue(combined.test(3L, "test"));
        assertTrue(combined.test(10L, "ab"));
        assertFalse(combined.test(3L, "ab"));
    }

    @Test
    public void testOrShortCircuit() {
        final boolean[] secondCalled = {false};
        LongObjPredicate<String> predicate1 = (l, s) -> true;
        LongObjPredicate<String> predicate2 = (l, s) -> {
            secondCalled[0] = true;
            return false;
        };

        LongObjPredicate<String> combined = predicate1.or(predicate2);
        assertTrue(combined.test(5L, "test"));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testComplexCombination() {
        LongObjPredicate<String> p1 = (l, s) -> l > 5L;
        LongObjPredicate<String> p2 = (l, s) -> s.length() > 3;
        LongObjPredicate<String> p3 = (l, s) -> l < 20L;

        LongObjPredicate<String> combined = p1.and(p2).or(p3);

        assertTrue(combined.test(10L, "test"));
        assertTrue(combined.test(3L, "ab"));
        assertFalse(combined.test(25L, "ab"));
    }

    @Test
    public void testWithNegativeValues() {
        LongObjPredicate<String> predicate = (l, s) -> l < 0;

        assertTrue(predicate.test(-5L, "test"));
        assertFalse(predicate.test(5L, "test"));
    }

    @Test
    public void testWithBoundaryValues() {
        LongObjPredicate<String> predicate = (l, s) -> l != 0L;

        assertTrue(predicate.test(Long.MAX_VALUE, "test"));
        assertTrue(predicate.test(Long.MIN_VALUE, "test"));
        assertFalse(predicate.test(0L, "test"));
    }

    @Test
    public void testWithNullObject() {
        LongObjPredicate<String> predicate = (l, s) -> s != null;

        assertTrue(predicate.test(5L, "test"));
        assertFalse(predicate.test(5L, null));
    }

    @Test
    public void testWithDifferentObjectTypes() {
        LongObjPredicate<Integer> predicate = (l, i) -> l > i;

        assertTrue(predicate.test(10L, 5));
        assertFalse(predicate.test(3L, 10));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(LongObjPredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
