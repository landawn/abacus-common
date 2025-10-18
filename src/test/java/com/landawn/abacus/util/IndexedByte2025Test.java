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
public class IndexedByte2025Test extends TestBase {

    @Test
    public void testOfIntIndex() {
        IndexedByte indexed = IndexedByte.of((byte) 42, 5);
        assertEquals((byte) 42, indexed.value());
        assertEquals(5, indexed.index());
        assertEquals(5L, indexed.longIndex());
    }

    @Test
    public void testOfLongIndex() {
        IndexedByte indexed = IndexedByte.of((byte) 100, 1000000000L);
        assertEquals((byte) 100, indexed.value());
        assertEquals(1000000000L, indexed.longIndex());
    }

    @Test
    public void testOfNegativeIntIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedByte.of((byte) 1, -1);
        });
    }

    @Test
    public void testOfNegativeLongIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedByte.of((byte) 1, -1L);
        });
    }

    @Test
    public void testValue() {
        IndexedByte indexed = IndexedByte.of((byte) 123, 0);
        assertEquals((byte) 123, indexed.value());
    }

    @Test
    public void testValueNegative() {
        IndexedByte indexed = IndexedByte.of((byte) -50, 0);
        assertEquals((byte) -50, indexed.value());
    }

    @Test
    public void testIndex() {
        IndexedByte indexed = IndexedByte.of((byte) 10, 42);
        assertEquals(42, indexed.index());
    }

    @Test
    public void testLongIndex() {
        long largeIndex = 5000000000L;
        IndexedByte indexed = IndexedByte.of((byte) 10, largeIndex);
        assertEquals(largeIndex, indexed.longIndex());
    }

    @Test
    public void testHashCode() {
        IndexedByte indexed1 = IndexedByte.of((byte) 10, 5);
        IndexedByte indexed2 = IndexedByte.of((byte) 10, 5);
        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testHashCodeDifferentValues() {
        IndexedByte indexed1 = IndexedByte.of((byte) 10, 5);
        IndexedByte indexed2 = IndexedByte.of((byte) 20, 5);
        assertNotEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testEquals() {
        IndexedByte indexed1 = IndexedByte.of((byte) 10, 5);
        IndexedByte indexed2 = IndexedByte.of((byte) 10, 5);
        IndexedByte indexed3 = IndexedByte.of((byte) 20, 5);
        IndexedByte indexed4 = IndexedByte.of((byte) 10, 10);

        assertTrue(indexed1.equals(indexed2));
        assertFalse(indexed1.equals(indexed3));
        assertFalse(indexed1.equals(indexed4));
        assertFalse(indexed1.equals(null));
        assertFalse(indexed1.equals("string"));
    }

    @Test
    public void testToString() {
        IndexedByte indexed = IndexedByte.of((byte) 42, 5);
        assertEquals("[5]=42", indexed.toString());
    }

    @Test
    public void testToStringNegativeValue() {
        IndexedByte indexed = IndexedByte.of((byte) -10, 3);
        assertEquals("[3]=-10", indexed.toString());
    }

    @Test
    public void testZeroValue() {
        IndexedByte indexed = IndexedByte.of((byte) 0, 0);
        assertEquals((byte) 0, indexed.value());
        assertEquals(0, indexed.index());
    }

    @Test
    public void testMaxByteValue() {
        IndexedByte indexed = IndexedByte.of(Byte.MAX_VALUE, 0);
        assertEquals(Byte.MAX_VALUE, indexed.value());
    }

    @Test
    public void testMinByteValue() {
        IndexedByte indexed = IndexedByte.of(Byte.MIN_VALUE, 0);
        assertEquals(Byte.MIN_VALUE, indexed.value());
    }
}
