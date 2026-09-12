package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class TriPredicateCoverageTest extends TestBase {

    @Test
    public void testAndOrNegate() {
        TriPredicate<Integer, Integer, Integer> allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
        TriPredicate<Integer, Integer, Integer> allEven = (a, b, c) -> a % 2 == 0 && b % 2 == 0 && c % 2 == 0;

        assertTrue(allPositive.and(allEven).test(2, 4, 6));
        assertFalse(allPositive.and(allEven).test(1, 3, 5));
        assertFalse(allPositive.and(allEven).test(2, 4, -1));
        assertThrows(IllegalArgumentException.class, () -> allPositive.and(null));

        assertTrue(allPositive.or(allEven).test(1, 3, 5));
        assertTrue(allPositive.or(allEven).test(-2, -4, -6));
        assertFalse(allPositive.or(allEven).test(-1, -3, -5));
        assertThrows(IllegalArgumentException.class, () -> allPositive.or(null));

        assertFalse(allPositive.negate().test(1, 2, 3));
        assertTrue(allPositive.negate().test(-1, 2, 3));
    }
}
