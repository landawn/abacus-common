package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BiObjIntPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        BiObjIntPredicate<String, String> predicate = (t, u, i) -> (t.length() + u.length()) == i;

        assertTrue(predicate.test("Hello", "World", 10));  // 5 + 5 = 10
        assertFalse(predicate.test("Hi", "There", 10));     // 2 + 5 != 10
    }

    @Test
    public void testTestWithLambda() {
        BiObjIntPredicate<Integer, Integer> predicate = (t, u, i) -> (t + u) == i;

        assertTrue(predicate.test(10, 20, 30));
        assertFalse(predicate.test(10, 20, 25));
    }

    @Test
    public void testTestWithComparison() {
        BiObjIntPredicate<String, String> predicate = (t, u, i) -> t.equals(u) && i > 0;

        assertTrue(predicate.test("test", "test", 5));
        assertFalse(predicate.test("test", "other", 5));  // not equal
        assertFalse(predicate.test("test", "test", 0));   // i not > 0
    }

    @Test
    public void testNegate() {
        BiObjIntPredicate<String, String> predicate = (t, u, i) -> (t.length() + u.length()) == i;
        BiObjIntPredicate<String, String> negated = predicate.negate();

        assertFalse(negated.test("Hello", "World", 10));  // original returns true
        assertTrue(negated.test("Hi", "There", 10));       // original returns false
    }

    @Test
    public void testAnd() {
        BiObjIntPredicate<String, String> lengthMatch = (t, u, i) -> (t.length() + u.length()) == i;
        BiObjIntPredicate<String, String> positiveInt = (t, u, i) -> i > 0;

        BiObjIntPredicate<String, String> combined = lengthMatch.and(positiveInt);

        assertTrue(combined.test("Hello", "World", 10));   // both conditions true
        assertFalse(combined.test("Hi", "There", 10));     // first condition false
        assertFalse(combined.test("Hi", "!", 0));          // second condition false (3 == 0 is false anyway)
    }

    @Test
    public void testAndShortCircuit() {
        BiObjIntPredicate<String, String> firstFalse = (t, u, i) -> false;
        BiObjIntPredicate<String, String> shouldNotExecute = (t, u, i) -> {
            throw new RuntimeException("Should not execute");
        };

        BiObjIntPredicate<String, String> combined = firstFalse.and(shouldNotExecute);

        assertFalse(combined.test("test", "value", 5)); // Should not throw exception
    }

    @Test
    public void testOr() {
        BiObjIntPredicate<String, String> equals = (t, u, i) -> t.equals(u);
        BiObjIntPredicate<String, String> intZero = (t, u, i) -> i == 0;

        BiObjIntPredicate<String, String> combined = equals.or(intZero);

        assertTrue(combined.test("test", "test", 5));   // equals
        assertTrue(combined.test("test", "other", 0));  // int is zero
        assertFalse(combined.test("test", "other", 5)); // neither condition
    }

    @Test
    public void testOrShortCircuit() {
        BiObjIntPredicate<String, String> firstTrue = (t, u, i) -> true;
        BiObjIntPredicate<String, String> shouldNotExecute = (t, u, i) -> {
            throw new RuntimeException("Should not execute");
        };

        BiObjIntPredicate<String, String> combined = firstTrue.or(shouldNotExecute);

        assertTrue(combined.test("test", "value", 5)); // Should not throw exception
    }

    @Test
    public void testComplexCombination() {
        BiObjIntPredicate<String, String> notEmpty = (t, u, i) -> !t.isEmpty() && !u.isEmpty();
        BiObjIntPredicate<String, String> positiveInt = (t, u, i) -> i > 0;
        BiObjIntPredicate<String, String> lengthSum = (t, u, i) -> (t.length() + u.length()) >= i;

        BiObjIntPredicate<String, String> complex = notEmpty.and(positiveInt).and(lengthSum);

        assertTrue(complex.test("Hello", "World", 5));    // all conditions true
        assertFalse(complex.test("", "World", 5));        // first condition false
        assertFalse(complex.test("Hello", "World", 0));   // second condition false
        assertFalse(complex.test("Hi", "!", 10));         // third condition false (3 >= 10 is false)
    }

    @Test
    public void testNegateAfterAnd() {
        BiObjIntPredicate<Integer, Integer> sumEquals = (t, u, i) -> (t + u) == i;
        BiObjIntPredicate<Integer, Integer> positive = (t, u, i) -> i > 0;

        BiObjIntPredicate<Integer, Integer> combined = sumEquals.and(positive).negate();

        assertFalse(combined.test(10, 20, 30));  // negation of (true && true)
        assertTrue(combined.test(10, 20, 25));   // negation of (false && ...)
    }

    @Test
    public void testTestWithException() {
        BiObjIntPredicate<String, String> predicate = (t, u, i) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> predicate.test("test", "value", 5));
    }

    @Test
    public void testAnonymousClass() {
        BiObjIntPredicate<String, String> predicate = new BiObjIntPredicate<String, String>() {
            @Override
            public boolean test(String t, String u, int i) {
                return (t + u).length() > i;
            }
        };

        assertTrue(predicate.test("Hello", "World", 5));   // 10 > 5
        assertFalse(predicate.test("Hi", "!", 10));        // 3 not > 10
    }

    @Test
    public void testWithNullObjects() {
        BiObjIntPredicate<String, String> predicate = (t, u, i) -> t == null && u == null;

        assertTrue(predicate.test(null, null, 5));
        assertFalse(predicate.test("test", null, 5));
        assertFalse(predicate.test(null, "value", 5));
    }

    @Test
    public void testWithNegativeInt() {
        BiObjIntPredicate<String, String> predicate = (t, u, i) -> i < 0;

        assertTrue(predicate.test("test", "value", -5));
        assertFalse(predicate.test("test", "value", 5));
    }

    @Test
    public void testComplexCondition() {
        BiObjIntPredicate<String, String> predicate = (t, u, i) -> {
            if (t == null || u == null) return false;
            if (i < 0) return false;
            int totalLength = t.length() + u.length();
            return totalLength >= i;
        };

        assertTrue(predicate.test("Hello", "World", 10));  // 10 >= 10
        assertTrue(predicate.test("Hello", "World", 5));   // 10 >= 5
        assertFalse(predicate.test("Hi", "!", 10));        // 3 < 10
        assertFalse(predicate.test(null, "World", 5));     // null check
        assertFalse(predicate.test("Hello", "World", -1)); // negative check
    }

    @Test
    public void testWithDifferentTypes() {
        BiObjIntPredicate<Integer, Double> predicate = (t, u, i) -> (t + u) == i;

        assertTrue(predicate.test(10, 5.0, 15));
        assertFalse(predicate.test(10, 5.5, 15));
    }
}
