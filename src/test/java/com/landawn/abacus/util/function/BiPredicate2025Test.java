package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;

@Tag("2025")
public class BiPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        BiPredicate<String, Integer> predicate = (s, i) -> s.length() == i;

        assertTrue(predicate.test("hello", 5));
        assertFalse(predicate.test("hello", 3));
    }

    @Test
    public void testTestWithLambda() {
        BiPredicate<Integer, Integer> predicate = (a, b) -> a > b;

        assertTrue(predicate.test(10, 5));
        assertFalse(predicate.test(5, 10));
        assertFalse(predicate.test(5, 5));
    }

    @Test
    public void testTestWithMethodReference() {
        BiPredicate<String, String> predicate = String::equals;

        assertTrue(predicate.test("test", "test"));
        assertFalse(predicate.test("test", "other"));
    }

    @Test
    public void testNegate() {
        BiPredicate<String, Integer> predicate = (s, i) -> s.length() == i;
        BiPredicate<String, Integer> negated = predicate.negate();

        assertFalse(negated.test("hello", 5));
        assertTrue(negated.test("hello", 3));
    }

    @Test
    public void testAnd() {
        BiPredicate<Integer, Integer> greaterThan = (a, b) -> a > b;
        BiPredicate<Integer, Integer> lessThan100 = (a, b) -> a < 100;

        BiPredicate<Integer, Integer> combined = greaterThan.and(lessThan100);

        assertTrue(combined.test(50, 10)); // 50 > 10 && 50 < 100
        assertFalse(combined.test(5, 10)); // 5 > 10 is false
        assertFalse(combined.test(150, 10)); // 150 < 100 is false
    }

    @Test
    public void testAndShortCircuit() {
        BiPredicate<Integer, Integer> firstFalse = (a, b) -> false;
        BiPredicate<Integer, Integer> shouldNotExecute = (a, b) -> {
            throw new RuntimeException("Should not execute");
        };

        BiPredicate<Integer, Integer> combined = firstFalse.and(shouldNotExecute);

        assertFalse(combined.test(10, 5)); // Should not throw exception
    }

    @Test
    public void testOr() {
        BiPredicate<String, String> equals = (s1, s2) -> s1.equals(s2);
        BiPredicate<String, String> bothEmpty = (s1, s2) -> s1.isEmpty() && s2.isEmpty();

        BiPredicate<String, String> combined = equals.or(bothEmpty);

        assertTrue(combined.test("test", "test")); // equals
        assertTrue(combined.test("", "")); // both empty
        assertFalse(combined.test("test", "other")); // neither
    }

    @Test
    public void testOrShortCircuit() {
        BiPredicate<Integer, Integer> firstTrue = (a, b) -> true;
        BiPredicate<Integer, Integer> shouldNotExecute = (a, b) -> {
            throw new RuntimeException("Should not execute");
        };

        BiPredicate<Integer, Integer> combined = firstTrue.or(shouldNotExecute);

        assertTrue(combined.test(10, 5)); // Should not throw exception
    }

    @Test
    public void testComplexCombination() {
        BiPredicate<Integer, Integer> greaterThan = (a, b) -> a > b;
        BiPredicate<Integer, Integer> lessThan100 = (a, b) -> a < 100;
        BiPredicate<Integer, Integer> evenSum = (a, b) -> (a + b) % 2 == 0;

        BiPredicate<Integer, Integer> complex = greaterThan.and(lessThan100).and(evenSum);

        assertTrue(complex.test(50, 10)); // 50 > 10, 50 < 100, (50+10) is even
        assertFalse(complex.test(50, 11)); // 50 > 11, 50 < 100, but (50+11) is odd
    }

    @Test
    public void testNegateAfterAnd() {
        BiPredicate<Integer, Integer> greaterThan = (a, b) -> a > b;
        BiPredicate<Integer, Integer> lessThan100 = (a, b) -> a < 100;

        BiPredicate<Integer, Integer> combined = greaterThan.and(lessThan100).negate();

        assertFalse(combined.test(50, 10)); // negation of (50 > 10 && 50 < 100)
        assertTrue(combined.test(5, 10)); // negation of (5 > 10 && 5 < 100)
    }

    @Test
    public void testTestWithException() {
        BiPredicate<String, Integer> predicate = (s, i) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> predicate.test("test", 5));
    }

    @Test
    public void testToThrowable() {
        BiPredicate<String, Integer> predicate = (s, i) -> s.length() == i;
        Throwables.BiPredicate<String, Integer, ?> throwablePredicate = predicate.toThrowable();

        assertNotNull(throwablePredicate);
    }

    @Test
    public void testToThrowableWithExecution() throws Throwable {
        BiPredicate<Integer, Integer> predicate = (a, b) -> a.equals(b);
        Throwables.BiPredicate<Integer, Integer, ?> throwablePredicate = predicate.toThrowable();

        assertTrue(throwablePredicate.test(42, 42));
        assertFalse(throwablePredicate.test(42, 43));
    }

    @Test
    public void testAnonymousClass() {
        BiPredicate<String, String> predicate = new BiPredicate<String, String>() {
            @Override
            public boolean test(String s1, String s2) {
                return s1.length() > s2.length();
            }
        };

        assertTrue(predicate.test("hello", "hi"));
        assertFalse(predicate.test("hi", "hello"));
    }

    @Test
    public void testWithNullValues() {
        BiPredicate<String, String> predicate = (s1, s2) -> s1 == null && s2 == null;

        assertTrue(predicate.test(null, null));
        assertFalse(predicate.test("test", null));
        assertFalse(predicate.test(null, "test"));
    }

    @Test
    public void testWithDifferentTypes() {
        BiPredicate<Integer, String> predicate = (i, s) -> i == s.length();

        assertTrue(predicate.test(5, "hello"));
        assertFalse(predicate.test(5, "hi"));
    }
}
