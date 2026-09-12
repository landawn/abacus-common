package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class RangeFloatingDocsTest {
    @Test
    void nanUpperBoundIncludesOnlyValuesAtOrAboveTheLowerBound() {
        final var range = Range.closed(1.0, Double.NaN);
        assertFalse(range.contains(Double.NEGATIVE_INFINITY));
        assertFalse(range.contains(0.0));
        assertTrue(range.contains(1.0));
        assertTrue(range.contains(Double.POSITIVE_INFINITY));
        assertTrue(range.contains(Double.NaN));
        assertFalse(Range.just(0.0).contains(-0.0));
        assertThrows(IllegalArgumentException.class, () -> Range.closed(Double.NaN, 1.0));
        final var floats = Range.closed(1.0f, Float.NaN);
        assertFalse(floats.contains(Float.NEGATIVE_INFINITY));
        assertTrue(floats.contains(Float.POSITIVE_INFINITY));
        assertTrue(floats.contains(Float.NaN));
    }
}
