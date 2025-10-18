package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjBiIntPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        ObjBiIntPredicate<String> predicate = (t, i, j) ->
            i >= 0 && j <= t.length() && i < j;

        assertTrue(predicate.test("Hello", 0, 5));
        assertFalse(predicate.test("Hello", -1, 5));
        assertFalse(predicate.test("Hello", 0, 10));
        assertFalse(predicate.test("Hello", 3, 2));
    }

    @Test
    public void testTestWithLambda() {
        ObjBiIntPredicate<String> predicate = (t, i, j) ->
            t.length() > i && t.length() >= j;

        assertTrue(predicate.test("Hello World", 5, 10));
        assertFalse(predicate.test("Hi", 5, 10));
    }

    @Test
    public void testTestWithAnonymousClass() {
        ObjBiIntPredicate<String> predicate = new ObjBiIntPredicate<String>() {
            @Override
            public boolean test(String t, int i, int j) {
                return t != null && i >= 0 && j > i;
            }
        };

        assertTrue(predicate.test("test", 1, 5));
        assertFalse(predicate.test("test", -1, 5));
        assertFalse(predicate.test("test", 5, 1));
    }

    @Test
    public void testNegate() {
        ObjBiIntPredicate<String> predicate = (t, i, j) -> i < j;
        ObjBiIntPredicate<String> negated = predicate.negate();

        assertTrue(negated.test("test", 5, 3));
        assertFalse(negated.test("test", 3, 5));
    }

    @Test
    public void testAnd() {
        ObjBiIntPredicate<String> predicate1 = (t, i, j) -> i >= 0 && j <= t.length();
        ObjBiIntPredicate<String> predicate2 = (t, i, j) -> i < j;

        ObjBiIntPredicate<String> combined = predicate1.and(predicate2);

        assertTrue(combined.test("Hello", 0, 5));
        assertFalse(combined.test("Hello", -1, 5));
        assertFalse(combined.test("Hello", 5, 3));
    }

    @Test
    public void testAndShortCircuit() {
        final boolean[] secondCalled = {false};
        ObjBiIntPredicate<String> predicate1 = (t, i, j) -> false;
        ObjBiIntPredicate<String> predicate2 = (t, i, j) -> {
            secondCalled[0] = true;
            return true;
        };

        ObjBiIntPredicate<String> combined = predicate1.and(predicate2);
        assertFalse(combined.test("test", 1, 2));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testOr() {
        ObjBiIntPredicate<String> predicate1 = (t, i, j) -> i == 0;
        ObjBiIntPredicate<String> predicate2 = (t, i, j) -> j == t.length();

        ObjBiIntPredicate<String> combined = predicate1.or(predicate2);

        assertTrue(combined.test("Hello", 0, 3));
        assertTrue(combined.test("Hello", 1, 5));
        assertFalse(combined.test("Hello", 1, 3));
    }

    @Test
    public void testOrShortCircuit() {
        final boolean[] secondCalled = {false};
        ObjBiIntPredicate<String> predicate1 = (t, i, j) -> true;
        ObjBiIntPredicate<String> predicate2 = (t, i, j) -> {
            secondCalled[0] = true;
            return false;
        };

        ObjBiIntPredicate<String> combined = predicate1.or(predicate2);
        assertTrue(combined.test("test", 1, 2));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testComplexCombination() {
        ObjBiIntPredicate<String> p1 = (t, i, j) -> i >= 0;
        ObjBiIntPredicate<String> p2 = (t, i, j) -> j <= t.length();
        ObjBiIntPredicate<String> p3 = (t, i, j) -> i < j;

        ObjBiIntPredicate<String> combined = p1.and(p2).and(p3);

        assertTrue(combined.test("Hello", 0, 5));
        assertFalse(combined.test("Hello", -1, 5));
        assertFalse(combined.test("Hello", 0, 10));
        assertFalse(combined.test("Hello", 5, 3));
    }

    @Test
    public void testWithNegativeInts() {
        ObjBiIntPredicate<String> predicate = (t, i, j) -> i < 0 && j < 0;

        assertTrue(predicate.test("test", -5, -10));
        assertFalse(predicate.test("test", 5, -10));
    }

    @Test
    public void testWithZeroInts() {
        ObjBiIntPredicate<String> predicate = (t, i, j) -> i == 0 && j == 0;

        assertTrue(predicate.test("test", 0, 0));
        assertFalse(predicate.test("test", 0, 1));
    }

    @Test
    public void testWithBoundaryValues() {
        ObjBiIntPredicate<String> predicate = (t, i, j) -> i < j;

        assertTrue(predicate.test("test", Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertFalse(predicate.test("test", Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    @Test
    public void testWithNullObject() {
        ObjBiIntPredicate<String> predicate = (t, i, j) -> t == null;

        assertTrue(predicate.test(null, 1, 2));
        assertFalse(predicate.test("test", 1, 2));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjBiIntPredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
