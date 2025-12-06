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
public class IndexedBoolean2025Test extends TestBase {

    @Test
    public void testOfIntIndex() {
        IndexedBoolean indexed = IndexedBoolean.of(true, 5);
        assertTrue(indexed.value());
        assertEquals(5, indexed.index());
        assertEquals(5L, indexed.longIndex());
    }

    @Test
    public void testOfLongIndex() {
        IndexedBoolean indexed = IndexedBoolean.of(false, 1000000000L);
        assertFalse(indexed.value());
        assertEquals(1000000000L, indexed.longIndex());
    }

    @Test
    public void testOfNegativeIntIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedBoolean.of(true, -1);
        });
    }

    @Test
    public void testOfNegativeLongIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedBoolean.of(true, -1L);
        });
    }

    @Test
    public void testValue() {
        IndexedBoolean indexed1 = IndexedBoolean.of(true, 0);
        assertTrue(indexed1.value());

        IndexedBoolean indexed2 = IndexedBoolean.of(false, 0);
        assertFalse(indexed2.value());
    }

    @Test
    public void testIndex() {
        IndexedBoolean indexed = IndexedBoolean.of(true, 42);
        assertEquals(42, indexed.index());
    }

    @Test
    public void testLongIndex() {
        long largeIndex = 5000000000L;
        IndexedBoolean indexed = IndexedBoolean.of(true, largeIndex);
        assertEquals(largeIndex, indexed.longIndex());
    }

    @Test
    public void testIndexOverflow() {
        assertThrows(ArithmeticException.class, () -> {
            IndexedBoolean indexed = IndexedBoolean.of(true, Long.MAX_VALUE);
            indexed.index();   // Should throw ArithmeticException
        });
    }

    @Test
    public void testHashCode() {
        IndexedBoolean indexed1 = IndexedBoolean.of(true, 5);
        IndexedBoolean indexed2 = IndexedBoolean.of(true, 5);
        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testHashCodeDifferentValues() {
        IndexedBoolean indexed1 = IndexedBoolean.of(true, 5);
        IndexedBoolean indexed2 = IndexedBoolean.of(false, 5);
        assertNotEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testHashCodeDifferentIndices() {
        IndexedBoolean indexed1 = IndexedBoolean.of(true, 5);
        IndexedBoolean indexed2 = IndexedBoolean.of(true, 10);
        assertNotEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testEquals() {
        IndexedBoolean indexed1 = IndexedBoolean.of(true, 5);
        IndexedBoolean indexed2 = IndexedBoolean.of(true, 5);
        IndexedBoolean indexed3 = IndexedBoolean.of(false, 5);
        IndexedBoolean indexed4 = IndexedBoolean.of(true, 10);

        assertTrue(indexed1.equals(indexed2));
        assertFalse(indexed1.equals(indexed3));
        assertFalse(indexed1.equals(indexed4));
        assertFalse(indexed1.equals(null));
        assertFalse(indexed1.equals("string"));
    }

    @Test
    public void testEqualsSameInstance() {
        IndexedBoolean indexed = IndexedBoolean.of(true, 5);
        assertTrue(indexed.equals(indexed));
    }

    @Test
    public void testToString() {
        IndexedBoolean indexed1 = IndexedBoolean.of(true, 5);
        assertEquals("[5]=true", indexed1.toString());

        IndexedBoolean indexed2 = IndexedBoolean.of(false, 10);
        assertEquals("[10]=false", indexed2.toString());
    }

    @Test
    public void testToStringZeroIndex() {
        IndexedBoolean indexed = IndexedBoolean.of(true, 0);
        assertEquals("[0]=true", indexed.toString());
    }

    @Test
    public void testToStringLargeIndex() {
        IndexedBoolean indexed = IndexedBoolean.of(false, 999999);
        assertEquals("[999999]=false", indexed.toString());
    }

    @Test
    public void testZeroIndex() {
        IndexedBoolean indexed = IndexedBoolean.of(true, 0);
        assertEquals(0, indexed.index());
        assertEquals(0L, indexed.longIndex());
    }

    @Test
    public void testMaxIntIndex() {
        IndexedBoolean indexed = IndexedBoolean.of(true, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, indexed.index());
        assertEquals(Integer.MAX_VALUE, indexed.longIndex());
    }
}
