package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IndexedLong2025Test extends TestBase {
    @Test
    public void testEquals_sameValues() {
        IndexedLong indexed1 = IndexedLong.of(123456789L, 5);
        IndexedLong indexed2 = IndexedLong.of(123456789L, 5);
        assertTrue(indexed1.equals(indexed2));
        assertTrue(indexed2.equals(indexed1));
    }

    @Test
    public void testEquals_differentValues() {
        IndexedLong indexed1 = IndexedLong.of(123456789L, 5);
        IndexedLong indexed2 = IndexedLong.of(987654321L, 5);
        assertFalse(indexed1.equals(indexed2));
    }

    @Test
    public void testEquals_differentIndices() {
        IndexedLong indexed1 = IndexedLong.of(123456789L, 5);
        IndexedLong indexed2 = IndexedLong.of(123456789L, 6);
        assertFalse(indexed1.equals(indexed2));
    }

    @Test
    public void testEquals_withNull() {
        IndexedLong indexed = IndexedLong.of(123456789L, 5);
        assertFalse(indexed.equals(null));
    }

    @Test
    public void testEquals_withDifferentClass() {
        IndexedLong indexed = IndexedLong.of(123456789L, 5);
        assertFalse(indexed.equals("123456789"));
        assertFalse(indexed.equals(Long.valueOf(123456789L)));
    }

    @Test
    public void testEquals_withSelf() {
        IndexedLong indexed = IndexedLong.of(123456789L, 5);
        assertTrue(indexed.equals(indexed));
    }

    @Test
    public void testHashCode_consistency() {
        IndexedLong indexed1 = IndexedLong.of(123456789L, 5);
        IndexedLong indexed2 = IndexedLong.of(123456789L, 5);
        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testHashCode_differentForDifferentObjects() {
        IndexedLong indexed1 = IndexedLong.of(123456789L, 5);
        IndexedLong indexed2 = IndexedLong.of(987654321L, 5);
        // Hash codes should be different (although not guaranteed by contract)
        assertNotEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testToString() {
        IndexedLong indexed = IndexedLong.of(123456789L, 5);
        assertEquals("[5]=123456789", indexed.toString());

        IndexedLong negativeValue = IndexedLong.of(-100L, 10);
        assertEquals("[10]=-100", negativeValue.toString());

        IndexedLong zero = IndexedLong.of(0L, 0);
        assertEquals("[0]=0", zero.toString());
    }

    @Test
    public void testWithLargeValues() {
        IndexedLong maxLong = IndexedLong.of(Long.MAX_VALUE, 1);
        assertEquals(Long.MAX_VALUE, maxLong.value());
        assertEquals("[1]=" + Long.MAX_VALUE, maxLong.toString());

        IndexedLong minLong = IndexedLong.of(Long.MIN_VALUE, 2);
        assertEquals(Long.MIN_VALUE, minLong.value());
        assertEquals("[2]=" + Long.MIN_VALUE, minLong.toString());
    }
}
