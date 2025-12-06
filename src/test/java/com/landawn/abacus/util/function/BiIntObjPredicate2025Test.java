package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;

@Tag("2025")
public class BiIntObjPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        BiIntObjPredicate<String> predicate = (i, j, s) -> (i + j) == s.length();

        assertTrue(predicate.test(2, 3, "hello"));   // 2 + 3 = 5
        assertFalse(predicate.test(2, 3, "hi"));   // 2 + 3 != 2
    }

    @Test
    public void testTestWithLambda() {
        BiIntObjPredicate<Integer> predicate = (i, j, obj) -> (i + j) == obj;

        assertTrue(predicate.test(10, 20, 30));
        assertFalse(predicate.test(10, 20, 25));
    }

    @Test
    public void testTestWithComparison() {
        BiIntObjPredicate<String> predicate = (i, j, s) -> s.startsWith("test") && i > j;

        assertTrue(predicate.test(10, 5, "test123"));
        assertFalse(predicate.test(5, 10, "test123"));   // i not > j
        assertFalse(predicate.test(10, 5, "other"));   // doesn't start with "test"
    }

    @Test
    public void testNegate() {
        BiIntObjPredicate<String> predicate = (i, j, s) -> (i + j) == s.length();
        BiIntObjPredicate<String> negated = predicate.negate();

        assertFalse(negated.test(2, 3, "hello"));   // original returns true, negated returns false
        assertTrue(negated.test(2, 3, "hi"));   // original returns false, negated returns true
    }

    @Test
    public void testAnd() {
        BiIntObjPredicate<String> greaterThan = (i, j, s) -> i > j;
        BiIntObjPredicate<String> lengthMatch = (i, j, s) -> s.length() == i;

        BiIntObjPredicate<String> combined = greaterThan.and(lengthMatch);

        assertTrue(combined.test(5, 3, "hello"));   // 5 > 3 && "hello".length() == 5
        assertFalse(combined.test(3, 5, "hello"));   // 3 > 5 is false
        assertFalse(combined.test(5, 3, "hi"));   // 5 > 3 but "hi".length() != 5
    }

    @Test
    public void testAndShortCircuit() {
        BiIntObjPredicate<String> firstFalse = (i, j, s) -> false;
        BiIntObjPredicate<String> shouldNotExecute = (i, j, s) -> {
            throw new RuntimeException("Should not execute");
        };

        BiIntObjPredicate<String> combined = firstFalse.and(shouldNotExecute);

        assertFalse(combined.test(10, 5, "test"));   // Should not throw exception
    }

    @Test
    public void testOr() {
        BiIntObjPredicate<String> sumEquals = (i, j, s) -> (i + j) == N.len(s);
        BiIntObjPredicate<String> isNull = (i, j, s) -> s == null;

        BiIntObjPredicate<String> combined = sumEquals.or(isNull);

        assertTrue(combined.test(2, 3, "hello"));   // sum equals length
        assertTrue(combined.test(1, 1, null));   // is null
        assertFalse(combined.test(1, 1, "hello"));   // neither condition
    }

    @Test
    public void testOrShortCircuit() {
        BiIntObjPredicate<String> firstTrue = (i, j, s) -> true;
        BiIntObjPredicate<String> shouldNotExecute = (i, j, s) -> {
            throw new RuntimeException("Should not execute");
        };

        BiIntObjPredicate<String> combined = firstTrue.or(shouldNotExecute);

        assertTrue(combined.test(10, 5, "test"));   // Should not throw exception
    }

    @Test
    public void testComplexCombination() {
        BiIntObjPredicate<String> sumPositive = (i, j, s) -> (i + j) > 0;
        BiIntObjPredicate<String> notEmpty = (i, j, s) -> s != null && !s.isEmpty();
        BiIntObjPredicate<String> iGreaterJ = (i, j, s) -> i > j;

        BiIntObjPredicate<String> complex = sumPositive.and(notEmpty).and(iGreaterJ);

        assertTrue(complex.test(10, 5, "test"));
        assertFalse(complex.test(10, 5, ""));   // empty string
        assertFalse(complex.test(5, 10, "test"));   // i not > j
        assertFalse(complex.test(-10, -5, "test"));   // sum not positive
    }

    @Test
    public void testNegateAfterAnd() {
        BiIntObjPredicate<Integer> sumEquals = (i, j, obj) -> (i + j) == obj;
        BiIntObjPredicate<Integer> positive = (i, j, obj) -> obj > 0;

        BiIntObjPredicate<Integer> combined = sumEquals.and(positive).negate();

        assertFalse(combined.test(10, 20, 30));   // negation of (true && true)
        assertTrue(combined.test(10, 20, 25));   // negation of (false && ...)
    }

    @Test
    public void testTestWithException() {
        BiIntObjPredicate<String> predicate = (i, j, s) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> predicate.test(5, 10, "test"));
    }

    @Test
    public void testAnonymousClass() {
        BiIntObjPredicate<String> predicate = new BiIntObjPredicate<String>() {
            @Override
            public boolean test(int i, int j, String s) {
                return s.length() > (i + j);
            }
        };

        assertTrue(predicate.test(2, 3, "hello world"));   // 11 > 5
        assertFalse(predicate.test(5, 5, "hello"));   // 5 not > 10
    }

    @Test
    public void testWithNullObject() {
        BiIntObjPredicate<String> predicate = (i, j, s) -> s == null;

        assertTrue(predicate.test(5, 10, null));
        assertFalse(predicate.test(5, 10, "test"));
    }

    @Test
    public void testWithNegativeInts() {
        BiIntObjPredicate<Integer> predicate = (i, j, obj) -> (i * j) == obj;

        assertTrue(predicate.test(-5, 7, -35));
        assertFalse(predicate.test(-5, 7, 35));
    }

    @Test
    public void testComplexCondition() {
        BiIntObjPredicate<String> predicate = (i, j, s) -> {
            if (s == null)
                return false;
            if (i < 0 || j < 0)
                return false;
            return s.length() >= i && s.length() <= j;
        };

        assertTrue(predicate.test(3, 7, "hello"));   // 5 is between 3 and 7
        assertFalse(predicate.test(6, 10, "hello"));   // 5 < 6
        assertFalse(predicate.test(1, 3, "hello"));   // 5 > 3
        assertFalse(predicate.test(-1, 10, "hello"));   // negative i
    }
}
