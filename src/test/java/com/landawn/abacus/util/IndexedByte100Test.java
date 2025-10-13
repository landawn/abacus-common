package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class IndexedByte100Test extends TestBase {

    @Test
    public void testOf_WithIntIndex() {
        byte value = (byte) 42;
        int index = 5;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals(value, indexedByte.value());
        assertEquals(index, indexedByte.index());
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
    public void testOf_WithIntIndex_NegativeIndex() {
        byte value = (byte) 42;
        int index = -1;

        assertThrows(IllegalArgumentException.class, () -> IndexedByte.of(value, index));
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
    public void testOf_WithLongIndex_NegativeIndex() {
        byte value = (byte) 42;
        long index = -1L;

        assertThrows(IllegalArgumentException.class, () -> IndexedByte.of(value, index));
    }

    @Test
    public void testValue() {
        byte[] testValues = { Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE };

        for (byte value : testValues) {
            IndexedByte indexedByte = IndexedByte.of(value, 0);
            assertEquals(value, indexedByte.value());
        }
    }

    @Test
    public void testHashCode() {
        byte value = (byte) 42;
        int index = 5;

        IndexedByte indexedByte1 = IndexedByte.of(value, index);
        IndexedByte indexedByte2 = IndexedByte.of(value, index);

        assertEquals(indexedByte1.hashCode(), indexedByte2.hashCode());
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
    public void testEquals_SameObject() {
        IndexedByte indexedByte = IndexedByte.of((byte) 42, 5);

        assertTrue(indexedByte.equals(indexedByte));
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
    public void testEquals_Null() {
        IndexedByte indexedByte = IndexedByte.of((byte) 42, 5);

        assertFalse(indexedByte.equals(null));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedByte indexedByte = IndexedByte.of((byte) 42, 5);

        assertFalse(indexedByte.equals("not an IndexedByte"));
        assertFalse(indexedByte.equals(42));
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
        byte value = (byte) 42;
        int index = 5;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals("[5]=42", indexedByte.toString());
    }

    @Test
    public void testToString_NegativeValue() {
        byte value = (byte) -42;
        int index = 10;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals("[10]=-42", indexedByte.toString());
    }

    @Test
    public void testToString_LongIndex() {
        byte value = (byte) 100;
        long index = 1000000L;

        IndexedByte indexedByte = IndexedByte.of(value, index);

        assertEquals("[1000000]=100", indexedByte.toString());
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
