package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IndexedDouble2025Test extends TestBase {

    @Test
    public void testOfIntIndex() {
        IndexedDouble indexed = IndexedDouble.of(3.14159, 5);
        assertEquals(3.14159, indexed.value(), 0.00001);
        assertEquals(5, indexed.index());
    }

    @Test
    public void testOfLongIndex() {
        IndexedDouble indexed = IndexedDouble.of(2.71828, 1000000000L);
        assertEquals(2.71828, indexed.value(), 0.00001);
        assertEquals(1000000000L, indexed.longIndex());
    }

    @Test
    public void testOfNegativeIntIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedDouble.of(1.0, -1);
        });
    }

    @Test
    public void testOfNegativeLongIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedDouble.of(1.0, -1L);
        });
    }

    @Test
    public void testValue() {
        IndexedDouble indexed = IndexedDouble.of(123.456, 0);
        assertEquals(123.456, indexed.value(), 0.001);
    }

    @Test
    public void testValueNegative() {
        IndexedDouble indexed = IndexedDouble.of(-50.5, 0);
        assertEquals(-50.5, indexed.value(), 0.001);
    }

    @Test
    public void testHashCode() {
        IndexedDouble indexed1 = IndexedDouble.of(3.14, 5);
        IndexedDouble indexed2 = IndexedDouble.of(3.14, 5);
        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testEquals() {
        IndexedDouble indexed1 = IndexedDouble.of(3.14, 5);
        IndexedDouble indexed2 = IndexedDouble.of(3.14, 5);
        IndexedDouble indexed3 = IndexedDouble.of(2.71, 5);

        assertTrue(indexed1.equals(indexed2));
        assertFalse(indexed1.equals(indexed3));
        assertFalse(indexed1.equals(null));
    }

    @Test
    public void testToString() {
        IndexedDouble indexed = IndexedDouble.of(3.14159, 5);
        assertTrue(indexed.toString().contains("[5]="));
        assertTrue(indexed.toString().contains("3.14159"));
    }

    @Test
    public void testZeroValue() {
        IndexedDouble indexed = IndexedDouble.of(0.0, 0);
        assertEquals(0.0, indexed.value(), 0.0);
    }

    @Test
    public void testNaN() {
        IndexedDouble indexed = IndexedDouble.of(Double.NaN, 0);
        assertTrue(Double.isNaN(indexed.value()));
    }

    @Test
    public void testInfinity() {
        IndexedDouble indexed1 = IndexedDouble.of(Double.POSITIVE_INFINITY, 0);
        assertEquals(Double.POSITIVE_INFINITY, indexed1.value());

        IndexedDouble indexed2 = IndexedDouble.of(Double.NEGATIVE_INFINITY, 0);
        assertEquals(Double.NEGATIVE_INFINITY, indexed2.value());
    }
}
