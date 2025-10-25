package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Predicate2025Test extends TestBase {

    @Test
    public void testTest() {
        Predicate<String> predicate = s -> s.length() > 3;
        assertTrue(predicate.test("hello"));
        assertFalse(predicate.test("hi"));
    }

    @Test
    public void testTest_WithAnonymousClass() {
        Predicate<Integer> predicate = new Predicate<Integer>() {
            @Override
            public boolean test(Integer value) {
                return value > 10;
            }
        };

        assertTrue(predicate.test(15));
        assertFalse(predicate.test(5));
    }

    @Test
    public void testNegate() {
        Predicate<String> predicate = s -> s.isEmpty();
        Predicate<String> negated = predicate.negate();

        assertFalse(negated.test(""));
        assertTrue(negated.test("hello"));
    }

    @Test
    public void testAnd() {
        Predicate<Integer> positive = n -> n > 0;
        Predicate<Integer> lessThan10 = n -> n < 10;

        Predicate<Integer> combined = positive.and(lessThan10);

        assertTrue(combined.test(5));
        assertFalse(combined.test(-1));
        assertFalse(combined.test(15));
    }

    @Test
    public void testAnd_ShortCircuit() {
        boolean[] called = new boolean[1];
        called[0] = false;

        Predicate<Integer> alwaysFalse = n -> false;
        Predicate<Integer> shouldNotBeCalled = n -> {
            called[0] = true;
            return true;
        };

        Predicate<Integer> combined = alwaysFalse.and(shouldNotBeCalled);
        assertFalse(combined.test(5));
        assertFalse(called[0]);
    }

    @Test
    public void testOr() {
        Predicate<Integer> negative = n -> n < 0;
        Predicate<Integer> greaterThan10 = n -> n > 10;

        Predicate<Integer> combined = negative.or(greaterThan10);

        assertTrue(combined.test(-1));
        assertTrue(combined.test(15));
        assertFalse(combined.test(5));
    }

    @Test
    public void testOr_ShortCircuit() {
        boolean[] called = new boolean[1];
        called[0] = false;

        Predicate<Integer> alwaysTrue = n -> true;
        Predicate<Integer> shouldNotBeCalled = n -> {
            called[0] = true;
            return false;
        };

        Predicate<Integer> combined = alwaysTrue.or(shouldNotBeCalled);
        assertTrue(combined.test(5));
        assertFalse(called[0]);
    }

    @Test
    public void testComplex_AndOrNegate() {
        Predicate<Integer> predicate = ((Predicate<Integer>) n -> n > 5).and(n -> n < 10).or(n -> n == 15).negate();

        assertFalse(predicate.test(7)); // (7>5 && 7<10) = true, negated = false
        assertFalse(predicate.test(15)); // (15==15) = true, negated = false
        assertTrue(predicate.test(3)); // (3>5 && 3<10) = false, (3==15) = false, negated = true
    }

    @Test
    public void testToThrowable() {
        Predicate<String> predicate = s -> s.isEmpty();
        com.landawn.abacus.util.Throwables.Predicate<String, ?> throwablePredicate = predicate.toThrowable();
        assertNotNull(throwablePredicate);
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        Predicate<String> predicate = s -> s.length() > 3;
        java.util.function.Predicate<String> javaPredicate = predicate;

        assertTrue(javaPredicate.test("hello"));
        assertFalse(javaPredicate.test("hi"));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        Predicate<String> lambda = s -> true;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.test("test"));
    }
}
