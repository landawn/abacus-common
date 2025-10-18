package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IndexedInt2025Test extends TestBase {

    @Test
    public void testOf_withIntIndex() {
        IndexedInt indexed = IndexedInt.of(42, 5);
        assertEquals(42, indexed.value());
        assertEquals(5, indexed.index());
    }

    @Test
    public void testOf_withLongIndex() {
        IndexedInt indexed = IndexedInt.of(42, 5000000000L);
        assertEquals(42, indexed.value());
        assertEquals(5000000000L, indexed.longIndex());
    }

    @Test
    public void testOf_withZeroIndex() {
        IndexedInt indexed = IndexedInt.of(100, 0);
        assertEquals(100, indexed.value());
        assertEquals(0, indexed.index());
    }

    @Test
    public void testOf_withNegativeIndex() {
        assertThrows(IllegalArgumentException.class, () -> IndexedInt.of(42, -1));
        assertThrows(IllegalArgumentException.class, () -> IndexedInt.of(42, -100));
        assertThrows(IllegalArgumentException.class, () -> IndexedInt.of(42, -1L));
    }

    @Test
    public void testValue() {
        IndexedInt indexed = IndexedInt.of(123, 1);
        assertEquals(123, indexed.value());

        IndexedInt negativeValue = IndexedInt.of(-456, 2);
        assertEquals(-456, negativeValue.value());

        IndexedInt zero = IndexedInt.of(0, 3);
        assertEquals(0, zero.value());
    }

    @Test
    public void testIndex() {
        IndexedInt indexed = IndexedInt.of(42, 10);
        assertEquals(10, indexed.index());

        IndexedInt largeIndex = IndexedInt.of(100, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, largeIndex.index());
    }

    @Test
    public void testEquals_sameValues() {
        IndexedInt indexed1 = IndexedInt.of(42, 5);
        IndexedInt indexed2 = IndexedInt.of(42, 5);
        assertTrue(indexed1.equals(indexed2));
        assertTrue(indexed2.equals(indexed1));
    }

    @Test
    public void testEquals_differentValues() {
        IndexedInt indexed1 = IndexedInt.of(42, 5);
        IndexedInt indexed2 = IndexedInt.of(43, 5);
        assertFalse(indexed1.equals(indexed2));
    }

    @Test
    public void testEquals_differentIndices() {
        IndexedInt indexed1 = IndexedInt.of(42, 5);
        IndexedInt indexed2 = IndexedInt.of(42, 6);
        assertFalse(indexed1.equals(indexed2));
    }

    @Test
    public void testEquals_withNull() {
        IndexedInt indexed = IndexedInt.of(42, 5);
        assertFalse(indexed.equals(null));
    }

    @Test
    public void testEquals_withDifferentClass() {
        IndexedInt indexed = IndexedInt.of(42, 5);
        assertFalse(indexed.equals("42"));
        assertFalse(indexed.equals(Integer.valueOf(42)));
    }

    @Test
    public void testEquals_withSelf() {
        IndexedInt indexed = IndexedInt.of(42, 5);
        assertTrue(indexed.equals(indexed));
    }

    @Test
    public void testHashCode_consistency() {
        IndexedInt indexed1 = IndexedInt.of(42, 5);
        IndexedInt indexed2 = IndexedInt.of(42, 5);
        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testHashCode_differentForDifferentObjects() {
        IndexedInt indexed1 = IndexedInt.of(42, 5);
        IndexedInt indexed2 = IndexedInt.of(43, 5);
        // Hash codes should be different (although not guaranteed by contract)
        assertNotEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testToString() {
        IndexedInt indexed = IndexedInt.of(42, 5);
        assertEquals("[5]=42", indexed.toString());

        IndexedInt negativeValue = IndexedInt.of(-100, 10);
        assertEquals("[10]=-100", negativeValue.toString());

        IndexedInt zero = IndexedInt.of(0, 0);
        assertEquals("[0]=0", zero.toString());
    }

    @Test
    public void testWithLargeValues() {
        IndexedInt maxInt = IndexedInt.of(Integer.MAX_VALUE, 1);
        assertEquals(Integer.MAX_VALUE, maxInt.value());
        assertEquals("[1]=" + Integer.MAX_VALUE, maxInt.toString());

        IndexedInt minInt = IndexedInt.of(Integer.MIN_VALUE, 2);
        assertEquals(Integer.MIN_VALUE, minInt.value());
        assertEquals("[2]=" + Integer.MIN_VALUE, minInt.toString());
    }
}
