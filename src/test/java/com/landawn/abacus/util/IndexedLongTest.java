package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IndexedLongTest extends TestBase {

    @Test
    public void testOf_WithIntIndex() {
        long value = 123456789L;
        int index = 5;

        IndexedLong indexedLong = IndexedLong.of(value, index);

        assertEquals(value, indexedLong.value());
        assertEquals(index, indexedLong.index());
    }

    @Test
    public void testOf_WithLongIndex() {
        long value = 123456789L;
        long index = 5000000000L;

        IndexedLong indexedLong = IndexedLong.of(value, index);

        assertEquals(value, indexedLong.value());
        assertEquals(index, indexedLong.longIndex());
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

    @Test
    public void testOf_WithIntIndex_Zero() {
        long value = 0L;
        int index = 0;

        IndexedLong indexedLong = IndexedLong.of(value, index);

        assertEquals(value, indexedLong.value());
        assertEquals(index, indexedLong.index());
    }

    @Test
    public void testOf_WithIntIndex_MaxValue() {
        long value = Long.MAX_VALUE;
        int index = Integer.MAX_VALUE;

        IndexedLong indexedLong = IndexedLong.of(value, index);

        assertEquals(value, indexedLong.value());
        assertEquals(index, indexedLong.index());
    }

    @Test
    public void testOf_WithIntIndex_MinValue() {
        long value = Long.MIN_VALUE;
        int index = 0;

        IndexedLong indexedLong = IndexedLong.of(value, index);

        assertEquals(value, indexedLong.value());
        assertEquals(index, indexedLong.index());
    }

    @Test
    public void testOf_WithLongIndex_Zero() {
        long value = -100L;
        long index = 0L;

        IndexedLong indexedLong = IndexedLong.of(value, index);

        assertEquals(value, indexedLong.value());
        assertEquals(index, indexedLong.index());
    }

    @Test
    public void testOf_WithLongIndex_MaxValue() {
        long value = Long.MIN_VALUE;
        long index = Long.MAX_VALUE;

        IndexedLong indexedLong = IndexedLong.of(value, index);

        assertEquals(value, indexedLong.value());
        assertEquals(index, indexedLong.longIndex());
    }

    @Test
    public void testOf_WithIntIndex_NegativeIndex() {
        long value = 123456789L;
        int index = -1;

        assertThrows(IllegalArgumentException.class, () -> IndexedLong.of(value, index));
    }

    @Test
    public void testOf_WithLongIndex_NegativeIndex() {
        long value = 123456789L;
        long index = -1L;

        assertThrows(IllegalArgumentException.class, () -> IndexedLong.of(value, index));
    }

    @Test
    public void testValue() {
        long[] testValues = { Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE };

        for (long value : testValues) {
            IndexedLong indexedLong = IndexedLong.of(value, 0);
            assertEquals(value, indexedLong.value());
        }
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
    public void testHashCode() {
        long value = 123456789L;
        int index = 5;

        IndexedLong indexedLong1 = IndexedLong.of(value, index);
        IndexedLong indexedLong2 = IndexedLong.of(value, index);

        assertEquals(indexedLong1.hashCode(), indexedLong2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        int index = 5;

        IndexedLong indexedLong1 = IndexedLong.of(123456789L, index);
        IndexedLong indexedLong2 = IndexedLong.of(987654321L, index);

        assertNotEquals(indexedLong1.hashCode(), indexedLong2.hashCode());
    }

    @Test
    public void testHashCode_DifferentIndices() {
        long value = 123456789L;

        IndexedLong indexedLong1 = IndexedLong.of(value, 5);
        IndexedLong indexedLong2 = IndexedLong.of(value, 6);

        assertNotEquals(indexedLong1.hashCode(), indexedLong2.hashCode());
    }

    @Test
    public void testHashCode_SpecificFormula() {
        IndexedLong indexedLong = IndexedLong.of(10L, 5);
        assertEquals(5 + (int) (10L * 31), indexedLong.hashCode());

        IndexedLong indexedNegative = IndexedLong.of(-10L, 5);
        assertEquals(5 + (int) (-10L * 31), indexedNegative.hashCode());
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
    public void testEquals_withDifferentClass() {
        IndexedLong indexed = IndexedLong.of(123456789L, 5);
        assertFalse(indexed.equals("123456789"));
        assertFalse(indexed.equals(Long.valueOf(123456789L)));
    }

    @Test
    public void testEquals_EqualObjects() {
        long value = 123456789L;
        int index = 5;

        IndexedLong indexedLong1 = IndexedLong.of(value, index);
        IndexedLong indexedLong2 = IndexedLong.of(value, index);

        assertTrue(indexedLong1.equals(indexedLong2));
        assertTrue(indexedLong2.equals(indexedLong1));
    }

    @Test
    public void testEquals_DifferentValues() {
        int index = 5;

        IndexedLong indexedLong1 = IndexedLong.of(123456789L, index);
        IndexedLong indexedLong2 = IndexedLong.of(987654321L, index);

        assertFalse(indexedLong1.equals(indexedLong2));
        assertFalse(indexedLong2.equals(indexedLong1));
    }

    @Test
    public void testEquals_DifferentIndices() {
        long value = 123456789L;

        IndexedLong indexedLong1 = IndexedLong.of(value, 5);
        IndexedLong indexedLong2 = IndexedLong.of(value, 6);

        assertFalse(indexedLong1.equals(indexedLong2));
        assertFalse(indexedLong2.equals(indexedLong1));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedLong indexedLong = IndexedLong.of(123456789L, 5);

        assertFalse(indexedLong.equals("not an IndexedLong"));
        assertFalse(indexedLong.equals(123456789L));
    }

    @Test
    public void testEquals_sameValues() {
        IndexedLong indexed1 = IndexedLong.of(123456789L, 5);
        IndexedLong indexed2 = IndexedLong.of(123456789L, 5);
        assertTrue(indexed1.equals(indexed2));
        assertTrue(indexed2.equals(indexed1));
    }

    @Test
    public void testEquals_withNull() {
        IndexedLong indexed = IndexedLong.of(123456789L, 5);
        assertFalse(indexed.equals(null));
    }

    @Test
    public void testEquals_withSelf() {
        IndexedLong indexed = IndexedLong.of(123456789L, 5);
        assertTrue(indexed.equals(indexed));
    }

    @Test
    public void testEquals_SameObject() {
        IndexedLong indexedLong = IndexedLong.of(123456789L, 5);

        assertTrue(indexedLong.equals(indexedLong));
    }

    @Test
    public void testEquals_Null() {
        IndexedLong indexedLong = IndexedLong.of(123456789L, 5);

        assertFalse(indexedLong.equals(null));
    }

    @Test
    public void testEquals_EdgeCases() {
        IndexedLong zero = IndexedLong.of(0L, 0);
        IndexedLong maxValue = IndexedLong.of(Long.MAX_VALUE, 0);
        IndexedLong minValue = IndexedLong.of(Long.MIN_VALUE, 0);

        assertTrue(zero.equals(IndexedLong.of(0L, 0)));
        assertTrue(maxValue.equals(IndexedLong.of(Long.MAX_VALUE, 0)));
        assertTrue(minValue.equals(IndexedLong.of(Long.MIN_VALUE, 0)));

        assertFalse(zero.equals(maxValue));
        assertFalse(maxValue.equals(minValue));
    }

    @Test
    public void testToString_LongIndex() {
        long value = 100L;
        long index = 1000000L;

        IndexedLong indexedLong = IndexedLong.of(value, index);

        assertEquals("[1000000]=100", indexedLong.toString());
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
    public void testToString_NegativeValue() {
        long value = -987654321L;
        int index = 10;

        IndexedLong indexedLong = IndexedLong.of(value, index);

        assertEquals("[10]=-987654321", indexedLong.toString());
    }

    @Test
    public void testToString_EdgeCases() {
        IndexedLong zero = IndexedLong.of(0L, 0L);
        IndexedLong maxValue = IndexedLong.of(Long.MAX_VALUE, Long.MAX_VALUE);
        IndexedLong minValue = IndexedLong.of(Long.MIN_VALUE, 0L);

        assertEquals("[0]=0", zero.toString());
        assertEquals("[" + Long.MAX_VALUE + "]=" + Long.MAX_VALUE, maxValue.toString());
        assertEquals("[0]=" + Long.MIN_VALUE, minValue.toString());
    }

}
