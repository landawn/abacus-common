package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class NPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        NPredicate<Integer> predicate = args -> {
            for (Integer n : args) {
                if (n <= 0)
                    return false;
            }
            return true;
        };

        assertTrue(predicate.test(1, 2, 3, 4));
        assertFalse(predicate.test(1, -2, 3));
        assertFalse(predicate.test(-1, -2, -3));
    }

    @Test
    public void testTestWithNoArguments() {
        NPredicate<String> predicate = args -> args.length == 0;

        assertTrue(predicate.test());
    }

    @Test
    public void testTestWithSingleArgument() {
        NPredicate<String> predicate = args -> args.length == 1 && args[0].length() > 3;

        assertTrue(predicate.test("hello"));
        assertFalse(predicate.test("hi"));
    }

    @Test
    public void testTestWithLambda() {
        NPredicate<String> predicate = args -> {
            for (String s : args) {
                if (s.isEmpty())
                    return true;
            }
            return false;
        };

        assertTrue(predicate.test("hello", "", "world"));
        assertFalse(predicate.test("hello", "world"));
    }

    @Test
    public void testTestWithAnonymousClass() {
        NPredicate<Integer> predicate = new NPredicate<Integer>() {
            @Override
            public boolean test(Integer... args) {
                int sum = 0;
                for (Integer n : args) {
                    sum += n;
                }
                return sum > 10;
            }
        };

        assertTrue(predicate.test(5, 6, 7));
        assertFalse(predicate.test(1, 2, 3));
    }

    @Test
    public void testNegate() {
        NPredicate<Integer> predicate = args -> {
            for (Integer n : args) {
                if (n % 2 != 0)
                    return false;
            }
            return true;
        };
        NPredicate<Integer> negated = predicate.negate();

        assertTrue(negated.test(2, 4, 5));
        assertFalse(negated.test(2, 4, 6));
    }

    @Test
    public void testAnd() {
        NPredicate<Integer> predicate1 = args -> {
            for (Integer n : args) {
                if (n <= 0)
                    return false;
            }
            return true;
        };
        NPredicate<Integer> predicate2 = args -> {
            int sum = 0;
            for (Integer n : args) {
                sum += n;
            }
            return sum < 100;
        };

        NPredicate<Integer> combined = predicate1.and(predicate2);

        assertTrue(combined.test(10, 20, 30));
        assertFalse(combined.test(1, -2, 3));
        assertFalse(combined.test(50, 60, 70));
    }

    @Test
    public void testAndShortCircuit() {
        final boolean[] secondCalled = { false };
        NPredicate<String> predicate1 = args -> false;
        NPredicate<String> predicate2 = args -> {
            secondCalled[0] = true;
            return true;
        };

        NPredicate<String> combined = predicate1.and(predicate2);
        assertFalse(combined.test("a", "b"));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testOr() {
        NPredicate<String> predicate1 = args -> {
            for (String s : args) {
                if (s.isEmpty())
                    return true;
            }
            return false;
        };
        NPredicate<String> predicate2 = args -> args.length > 5;

        NPredicate<String> combined = predicate1.or(predicate2);

        assertTrue(combined.test("hello", "", "world"));
        assertTrue(combined.test("a", "b", "c", "d", "e", "f"));
        assertFalse(combined.test("hello", "world"));
    }

    @Test
    public void testOrShortCircuit() {
        final boolean[] secondCalled = { false };
        NPredicate<String> predicate1 = args -> true;
        NPredicate<String> predicate2 = args -> {
            secondCalled[0] = true;
            return false;
        };

        NPredicate<String> combined = predicate1.or(predicate2);
        assertTrue(combined.test("a", "b"));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testComplexCombination() {
        NPredicate<Integer> p1 = args -> args.length > 0;
        NPredicate<Integer> p2 = args -> args[0] > 5;
        NPredicate<Integer> p3 = args -> args.length < 10;

        NPredicate<Integer> combined = p1.and(p2).or(p3);

        assertTrue(combined.test(10, 20));
        assertTrue(combined.test(1, 2));
        assertTrue(combined.test());
    }

    @Test
    public void testWithNullValues() {
        NPredicate<String> predicate = args -> {
            for (String s : args) {
                if (s == null)
                    return true;
            }
            return false;
        };

        assertTrue(predicate.test("a", null, "b"));
        assertFalse(predicate.test("a", "b", "c"));
    }

    @Test
    public void testAllMatch() {
        NPredicate<String> predicate = args -> {
            for (String s : args) {
                if (s.length() <= 3)
                    return false;
            }
            return true;
        };

        assertTrue(predicate.test("hello", "world", "test"));
        assertFalse(predicate.test("hello", "hi", "test"));
    }

    @Test
    public void testAnyMatch() {
        NPredicate<Integer> predicate = args -> {
            for (Integer n : args) {
                if (n > 100)
                    return true;
            }
            return false;
        };

        assertTrue(predicate.test(10, 20, 150, 30));
        assertFalse(predicate.test(10, 20, 30));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(NPredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
