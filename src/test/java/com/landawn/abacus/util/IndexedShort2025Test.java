package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IndexedShort2025Test extends TestBase {

    @Test
    public void testOfIntIndex() {
        IndexedShort indexed = IndexedShort.of((short) 42, 5);
        assertEquals((short) 42, indexed.value());
        assertEquals(5, indexed.index());
    }

    @Test
    public void testOfLongIndex() {
        IndexedShort indexed = IndexedShort.of((short) 100, 1000000000L);
        assertEquals((short) 100, indexed.value());
        assertEquals(1000000000L, indexed.longIndex());
    }

    @Test
    public void testOfNegativeIntIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedShort.of((short) 1, -1);
        });
    }

    @Test
    public void testValue() {
        IndexedShort indexed = IndexedShort.of((short) 123, 0);
        assertEquals((short) 123, indexed.value());
    }

    @Test
    public void testHashCode() {
        IndexedShort indexed1 = IndexedShort.of((short) 10, 5);
        IndexedShort indexed2 = IndexedShort.of((short) 10, 5);
        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testEquals() {
        IndexedShort indexed1 = IndexedShort.of((short) 10, 5);
        IndexedShort indexed2 = IndexedShort.of((short) 10, 5);
        IndexedShort indexed3 = IndexedShort.of((short) 20, 5);

        assertTrue(indexed1.equals(indexed2));
        assertFalse(indexed1.equals(indexed3));
    }

    @Test
    public void testToString() {
        IndexedShort indexed = IndexedShort.of((short) 42, 5);
        assertEquals("[5]=42", indexed.toString());
    }

    @Test
    public void testZeroValue() {
        IndexedShort indexed = IndexedShort.of((short) 0, 0);
        assertEquals((short) 0, indexed.value());
    }

    @Test
    public void testMaxValue() {
        IndexedShort indexed = IndexedShort.of(Short.MAX_VALUE, 0);
        assertEquals(Short.MAX_VALUE, indexed.value());
    }

    @Test
    public void testMinValue() {
        IndexedShort indexed = IndexedShort.of(Short.MIN_VALUE, 0);
        assertEquals(Short.MIN_VALUE, indexed.value());
    }
}
