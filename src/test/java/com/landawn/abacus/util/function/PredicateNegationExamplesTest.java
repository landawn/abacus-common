package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
class PredicateNegationExamplesTest {
    @ParameterizedTest
    @CsvSource({
            "1, 2, true, true",
            "2, 2, true, true",
            "3, 2, false, false",
            "-0.0, 0.0, true, true",
            "Infinity, Infinity, true, true",
            "-Infinity, Infinity, true, true",
            "Infinity, -Infinity, false, false",
            "NaN, 2, true, false",
            "2, NaN, true, false",
            "NaN, NaN, true, false"
    })
    void priceNotAboveIncludesUnorderedComparisons(double price, double threshold, boolean expectedNegation, boolean expectedAtMost) {
        ObjDoublePredicate<Double> isPriceAbove = (value, limit) -> value > limit;
        assertEquals(expectedNegation, isPriceAbove.negate().test(price, threshold));
        assertEquals(expectedAtMost, price <= threshold);
    }

    @ParameterizedTest
    @CsvSource({ "1, 2, 3, 4, false", "0, 2, 3, 4, true", "-1, 2, 3, 4, true", "1, 2, 3, 0, true" })
    void notAllPositiveAlsoIncludesZero(int a, int b, int c, int d, boolean expected) {
        QuadPredicate<Integer, Integer, Integer, Integer> allPositive = (w, x, y, z) -> w > 0 && x > 0 && y > 0 && z > 0;
        assertEquals(expected, allPositive.negate().test(a, b, c, d));
    }

    @Test
    void nullObjectStillUsesTheOriginalPredicateContract() {
        ObjDoublePredicate<Double> isPriceAbove = (value, threshold) -> value > threshold;
        assertThrows(NullPointerException.class, () -> isPriceAbove.negate().test(null, 0));
    }

    @Test
    void emptyAndUnicodeObjectsRetainTheirComparisonSemantics() {
        ObjDoublePredicate<String> lengthAbove = (value, threshold) -> value.codePointCount(0, value.length()) > threshold;
        assertTrue(lengthAbove.negate().test("", 0));
        assertFalse(lengthAbove.negate().test("🙂中", 1));
        assertTrue(lengthAbove.negate().test("🙂中", Double.NaN));
    }
}
