package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleObjPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        DoubleObjPredicate<String> predicate = (d, s) -> d > 5.0 && s.length() > 3;

        assertTrue(predicate.test(10.0, "test"));
        assertFalse(predicate.test(3.0, "test"));
        assertFalse(predicate.test(10.0, "ab"));
        assertFalse(predicate.test(3.0, "ab"));
    }

    @Test
    public void testTestWithLambda() {
        DoubleObjPredicate<Integer> predicate = (d, i) -> d > i;

        assertTrue(predicate.test(10.5, 5));
        assertFalse(predicate.test(3.5, 10));
        assertFalse(predicate.test(5.0, 5));
    }

    @Test
    public void testTestWithAnonymousClass() {
        DoubleObjPredicate<String> predicate = new DoubleObjPredicate<String>() {
            @Override
            public boolean test(double t, String u) {
                return t >= 0 && u != null && !u.isEmpty();
            }
        };

        assertTrue(predicate.test(5.0, "test"));
        assertFalse(predicate.test(-1.0, "test"));
        assertFalse(predicate.test(5.0, ""));
    }

    @Test
    public void testNegate() {
        DoubleObjPredicate<String> predicate = (d, s) -> d > 5.0;
        DoubleObjPredicate<String> negated = predicate.negate();

        assertFalse(negated.test(10.0, "test"));
        assertTrue(negated.test(3.0, "test"));
    }

    @Test
    public void testAnd() {
        DoubleObjPredicate<String> predicate1 = (d, s) -> d > 5.0;
        DoubleObjPredicate<String> predicate2 = (d, s) -> s.length() > 3;

        DoubleObjPredicate<String> combined = predicate1.and(predicate2);

        assertTrue(combined.test(10.0, "test"));
        assertFalse(combined.test(3.0, "test"));
        assertFalse(combined.test(10.0, "ab"));
        assertFalse(combined.test(3.0, "ab"));
    }

    @Test
    public void testAndShortCircuit() {
        final boolean[] secondCalled = { false };
        DoubleObjPredicate<String> predicate1 = (d, s) -> false;
        DoubleObjPredicate<String> predicate2 = (d, s) -> {
            secondCalled[0] = true;
            return true;
        };

        DoubleObjPredicate<String> combined = predicate1.and(predicate2);
        assertFalse(combined.test(5.0, "test"));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testOr() {
        DoubleObjPredicate<String> predicate1 = (d, s) -> d > 5.0;
        DoubleObjPredicate<String> predicate2 = (d, s) -> s.length() > 3;

        DoubleObjPredicate<String> combined = predicate1.or(predicate2);

        assertTrue(combined.test(10.0, "test"));
        assertTrue(combined.test(3.0, "test"));
        assertTrue(combined.test(10.0, "ab"));
        assertFalse(combined.test(3.0, "ab"));
    }

    @Test
    public void testOrShortCircuit() {
        final boolean[] secondCalled = { false };
        DoubleObjPredicate<String> predicate1 = (d, s) -> true;
        DoubleObjPredicate<String> predicate2 = (d, s) -> {
            secondCalled[0] = true;
            return false;
        };

        DoubleObjPredicate<String> combined = predicate1.or(predicate2);
        assertTrue(combined.test(5.0, "test"));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testComplexCombination() {
        DoubleObjPredicate<String> p1 = (d, s) -> d > 5.0;
        DoubleObjPredicate<String> p2 = (d, s) -> s.length() > 3;
        DoubleObjPredicate<String> p3 = (d, s) -> d < 20.0;

        DoubleObjPredicate<String> combined = p1.and(p2).or(p3);

        assertTrue(combined.test(10.0, "test"));
        assertTrue(combined.test(3.0, "ab"));
        assertFalse(combined.test(25.0, "ab"));
    }

    @Test
    public void testWithNegativeValues() {
        DoubleObjPredicate<String> predicate = (d, s) -> d < 0;

        assertTrue(predicate.test(-5.5, "test"));
        assertFalse(predicate.test(5.5, "test"));
    }

    @Test
    public void testWithBoundaryValues() {
        DoubleObjPredicate<String> predicate = (d, s) -> d != 0.0;

        assertTrue(predicate.test(Double.MAX_VALUE, "test"));
        assertTrue(predicate.test(Double.MIN_VALUE, "test"));
        assertFalse(predicate.test(0.0, "test"));
    }

    @Test
    public void testWithSpecialValues() {
        DoubleObjPredicate<String> predicate = (d, s) -> !Double.isNaN(d) && !Double.isInfinite(d);

        assertTrue(predicate.test(5.0, "test"));
        assertFalse(predicate.test(Double.NaN, "test"));
        assertFalse(predicate.test(Double.POSITIVE_INFINITY, "test"));
        assertFalse(predicate.test(Double.NEGATIVE_INFINITY, "test"));
    }

    @Test
    public void testWithNullObject() {
        DoubleObjPredicate<String> predicate = (d, s) -> s != null;

        assertTrue(predicate.test(5.0, "test"));
        assertFalse(predicate.test(5.0, null));
    }

    @Test
    public void testWithDifferentObjectTypes() {
        DoubleObjPredicate<Integer> predicate = (d, i) -> d > i;

        assertTrue(predicate.test(10.5, 5));
        assertFalse(predicate.test(3.5, 10));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(DoubleObjPredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
