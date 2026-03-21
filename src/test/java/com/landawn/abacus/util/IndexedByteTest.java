package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IndexedByteTest extends TestBase {

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
    public void testOf_WithIntIndex() {
        byte value = (byte) 42;
        int index = 5;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals(value, indexedByte.value());
        assertEquals(index, indexedByte.index());
    }

    @Test
    public void testOf_WithLongIndex() {
        byte value = (byte) 42;
        long index = 5000000000L;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals(value, indexedByte.value());
        assertEquals(index, indexedByte.longIndex());
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

    @Test
    public void testOf_WithIntIndex_Zero() {
        byte value = (byte) 0;
        int index = 0;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals(value, indexedByte.value());
        assertEquals(index, indexedByte.index());
    }

    @Test
    public void testOf_WithIntIndex_MaxValue() {
        byte value = Byte.MAX_VALUE;
        int index = Integer.MAX_VALUE;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals(value, indexedByte.value());
        assertEquals(index, indexedByte.index());
    }

    @Test
    public void testOf_WithIntIndex_MinValue() {
        byte value = Byte.MIN_VALUE;
        int index = 0;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals(value, indexedByte.value());
        assertEquals(index, indexedByte.index());
    }

    @Test
    public void testOf_WithLongIndex_Zero() {
        byte value = (byte) -100;
        long index = 0L;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals(value, indexedByte.value());
        assertEquals(index, indexedByte.index());
    }

    @Test
    public void testOf_WithLongIndex_MaxValue() {
        byte value = Byte.MIN_VALUE;
        long index = Long.MAX_VALUE;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals(value, indexedByte.value());
        assertEquals(index, indexedByte.longIndex());
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
    public void testOf_WithIntIndex_NegativeIndex() {
        byte value = (byte) 42;
        int index = -1;

        assertThrows(IllegalArgumentException.class, () -> IndexedByte.of(value, index));
    }

    @Test
    public void testOf_WithLongIndex_NegativeIndex() {
        byte value = (byte) 42;
        long index = -1L;

        assertThrows(IllegalArgumentException.class, () -> IndexedByte.of(value, index));
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
    public void testHashCode_DifferentValues() {
        int index = 5;

        IndexedByte indexedByte1 = IndexedByte.of((byte) 42, index);
        IndexedByte indexedByte2 = IndexedByte.of((byte) 43, index);

        assertNotEquals(indexedByte1.hashCode(), indexedByte2.hashCode());
    }

    @Test
    public void testHashCode_DifferentIndices() {
        byte value = (byte) 42;

        IndexedByte indexedByte1 = IndexedByte.of(value, 5);
        IndexedByte indexedByte2 = IndexedByte.of(value, 6);

        assertNotEquals(indexedByte1.hashCode(), indexedByte2.hashCode());
    }

    @Test
    public void testHashCode_SpecificFormula() {
        IndexedByte indexedByte = IndexedByte.of((byte) 10, 5);
        assertEquals(5 + 10 * 31, indexedByte.hashCode());

        IndexedByte indexedNegative = IndexedByte.of((byte) -10, 5);
        assertEquals(5 + (-10) * 31, indexedNegative.hashCode());
    }

    @Test
    public void testEquals_EqualObjects() {
        byte value = (byte) 42;
        int index = 5;

        IndexedByte indexedByte1 = IndexedByte.of(value, index);
        IndexedByte indexedByte2 = IndexedByte.of(value, index);

        assertTrue(indexedByte1.equals(indexedByte2));
        assertTrue(indexedByte2.equals(indexedByte1));
    }

    @Test
    public void testEquals_DifferentValues() {
        int index = 5;

        IndexedByte indexedByte1 = IndexedByte.of((byte) 42, index);
        IndexedByte indexedByte2 = IndexedByte.of((byte) 43, index);

        assertFalse(indexedByte1.equals(indexedByte2));
        assertFalse(indexedByte2.equals(indexedByte1));
    }

    @Test
    public void testEquals_DifferentIndices() {
        byte value = (byte) 42;

        IndexedByte indexedByte1 = IndexedByte.of(value, 5);
        IndexedByte indexedByte2 = IndexedByte.of(value, 6);

        assertFalse(indexedByte1.equals(indexedByte2));
        assertFalse(indexedByte2.equals(indexedByte1));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedByte indexedByte = IndexedByte.of((byte) 42, 5);

        assertFalse(indexedByte.equals("not an IndexedByte"));
        assertFalse(indexedByte.equals(42));
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
    public void testEquals_SameObject() {
        IndexedByte indexedByte = IndexedByte.of((byte) 42, 5);

        assertTrue(indexedByte.equals(indexedByte));
    }

    @Test
    public void testEquals_Null() {
        IndexedByte indexedByte = IndexedByte.of((byte) 42, 5);

        assertFalse(indexedByte.equals(null));
    }

    @Test
    public void testEquals_EdgeCases() {
        IndexedByte zero = IndexedByte.of((byte) 0, 0);
        IndexedByte maxValue = IndexedByte.of(Byte.MAX_VALUE, 0);
        IndexedByte minValue = IndexedByte.of(Byte.MIN_VALUE, 0);

        assertTrue(zero.equals(IndexedByte.of((byte) 0, 0)));
        assertTrue(maxValue.equals(IndexedByte.of(Byte.MAX_VALUE, 0)));
        assertTrue(minValue.equals(IndexedByte.of(Byte.MIN_VALUE, 0)));

        assertFalse(zero.equals(maxValue));
        assertFalse(maxValue.equals(minValue));
    }

    @Test
    public void testToString() {
        IndexedByte indexed = IndexedByte.of((byte) 42, 5);
        assertEquals("[5]=42", indexed.toString());
    }

    @Test
    public void testToString_LongIndex() {
        byte value = (byte) 100;
        long index = 1000000L;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals("[1000000]=100", indexedByte.toString());
    }

    @Test
    public void testToStringNegativeValue() {
        IndexedByte indexed = IndexedByte.of((byte) -10, 3);
        assertEquals("[3]=-10", indexed.toString());
    }

    @Test
    public void testToString_NegativeValue() {
        byte value = (byte) -42;
        int index = 10;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals("[10]=-42", indexedByte.toString());
    }

    @Test
    public void testToString_EdgeCases() {
        IndexedByte zero = IndexedByte.of((byte) 0, 0);
        IndexedByte maxValue = IndexedByte.of(Byte.MAX_VALUE, Long.MAX_VALUE);
        IndexedByte minValue = IndexedByte.of(Byte.MIN_VALUE, 0);

        assertEquals("[0]=0", zero.toString());
        assertEquals("[" + Long.MAX_VALUE + "]=" + Byte.MAX_VALUE, maxValue.toString());
        assertEquals("[0]=" + Byte.MIN_VALUE, minValue.toString());
    }

}
