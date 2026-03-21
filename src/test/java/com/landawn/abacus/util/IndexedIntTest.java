package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IndexedIntTest extends TestBase {

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
    public void testOf_WithIntIndex() {
        int value = 42;
        int index = 5;

        IndexedInt indexedInt = IndexedInt.of(value, index);

        assertEquals(value, indexedInt.value());
        assertEquals(index, indexedInt.index());
    }

    @Test
    public void testOf_WithLongIndex() {
        int value = 42;
        long index = 5000000000L;

        IndexedInt indexedInt = IndexedInt.of(value, index);

        assertEquals(value, indexedInt.value());
        assertEquals(index, indexedInt.longIndex());
    }

    @Test
    public void testOf_withZeroIndex() {
        IndexedInt indexed = IndexedInt.of(100, 0);
        assertEquals(100, indexed.value());
        assertEquals(0, indexed.index());
    }

    @Test
    public void testIndex() {
        IndexedInt indexed = IndexedInt.of(42, 10);
        assertEquals(10, indexed.index());

        IndexedInt largeIndex = IndexedInt.of(100, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, largeIndex.index());
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

    @Test
    public void testOf_WithIntIndex_Zero() {
        int value = 0;
        int index = 0;

        IndexedInt indexedInt = IndexedInt.of(value, index);

        assertEquals(value, indexedInt.value());
        assertEquals(index, indexedInt.index());
    }

    @Test
    public void testOf_WithIntIndex_MaxValue() {
        int value = Integer.MAX_VALUE;
        int index = Integer.MAX_VALUE;

        IndexedInt indexedInt = IndexedInt.of(value, index);

        assertEquals(value, indexedInt.value());
        assertEquals(index, indexedInt.index());
    }

    @Test
    public void testOf_WithIntIndex_MinValue() {
        int value = Integer.MIN_VALUE;
        int index = 0;

        IndexedInt indexedInt = IndexedInt.of(value, index);

        assertEquals(value, indexedInt.value());
        assertEquals(index, indexedInt.index());
    }

    @Test
    public void testOf_WithLongIndex_Zero() {
        int value = -100;
        long index = 0L;

        IndexedInt indexedInt = IndexedInt.of(value, index);

        assertEquals(value, indexedInt.value());
        assertEquals(index, indexedInt.index());
    }

    @Test
    public void testOf_WithLongIndex_MaxValue() {
        int value = Integer.MIN_VALUE;
        long index = Long.MAX_VALUE;

        IndexedInt indexedInt = IndexedInt.of(value, index);

        assertEquals(value, indexedInt.value());
        assertEquals(index, indexedInt.longIndex());
    }

    @Test
    public void testOf_withNegativeIndex() {
        assertThrows(IllegalArgumentException.class, () -> IndexedInt.of(42, -1));
        assertThrows(IllegalArgumentException.class, () -> IndexedInt.of(42, -100));
        assertThrows(IllegalArgumentException.class, () -> IndexedInt.of(42, -1L));
    }

    @Test
    public void testOf_WithIntIndex_NegativeIndex() {
        int value = 42;
        int index = -1;

        assertThrows(IllegalArgumentException.class, () -> IndexedInt.of(value, index));
    }

    @Test
    public void testOf_WithLongIndex_NegativeIndex() {
        int value = 42;
        long index = -1L;

        assertThrows(IllegalArgumentException.class, () -> IndexedInt.of(value, index));
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
    public void testHashCode() {
        int value = 42;
        int index = 5;

        IndexedInt indexedInt1 = IndexedInt.of(value, index);
        IndexedInt indexedInt2 = IndexedInt.of(value, index);

        assertEquals(indexedInt1.hashCode(), indexedInt2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        int index = 5;

        IndexedInt indexedInt1 = IndexedInt.of(42, index);
        IndexedInt indexedInt2 = IndexedInt.of(43, index);

        assertNotEquals(indexedInt1.hashCode(), indexedInt2.hashCode());
    }

    @Test
    public void testHashCode_DifferentIndices() {
        int value = 42;

        IndexedInt indexedInt1 = IndexedInt.of(value, 5);
        IndexedInt indexedInt2 = IndexedInt.of(value, 6);

        assertNotEquals(indexedInt1.hashCode(), indexedInt2.hashCode());
    }

    @Test
    public void testHashCode_SpecificFormula() {
        IndexedInt indexedInt = IndexedInt.of(10, 5);
        assertEquals(5 + 10 * 31, indexedInt.hashCode());

        IndexedInt indexedNegative = IndexedInt.of(-10, 5);
        assertEquals(5 + (-10) * 31, indexedNegative.hashCode());
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
    public void testEquals_withDifferentClass() {
        IndexedInt indexed = IndexedInt.of(42, 5);
        assertFalse(indexed.equals("42"));
        assertFalse(indexed.equals(Integer.valueOf(42)));
    }

    @Test
    public void testEquals_EqualObjects() {
        int value = 42;
        int index = 5;

        IndexedInt indexedInt1 = IndexedInt.of(value, index);
        IndexedInt indexedInt2 = IndexedInt.of(value, index);

        assertTrue(indexedInt1.equals(indexedInt2));
        assertTrue(indexedInt2.equals(indexedInt1));
    }

    @Test
    public void testEquals_DifferentValues() {
        int index = 5;

        IndexedInt indexedInt1 = IndexedInt.of(42, index);
        IndexedInt indexedInt2 = IndexedInt.of(43, index);

        assertFalse(indexedInt1.equals(indexedInt2));
        assertFalse(indexedInt2.equals(indexedInt1));
    }

    @Test
    public void testEquals_DifferentIndices() {
        int value = 42;

        IndexedInt indexedInt1 = IndexedInt.of(value, 5);
        IndexedInt indexedInt2 = IndexedInt.of(value, 6);

        assertFalse(indexedInt1.equals(indexedInt2));
        assertFalse(indexedInt2.equals(indexedInt1));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedInt indexedInt = IndexedInt.of(42, 5);

        assertFalse(indexedInt.equals("not an IndexedInt"));
        assertFalse(indexedInt.equals(42));
    }

    @Test
    public void testEquals_sameValues() {
        IndexedInt indexed1 = IndexedInt.of(42, 5);
        IndexedInt indexed2 = IndexedInt.of(42, 5);
        assertTrue(indexed1.equals(indexed2));
        assertTrue(indexed2.equals(indexed1));
    }

    @Test
    public void testEquals_withNull() {
        IndexedInt indexed = IndexedInt.of(42, 5);
        assertFalse(indexed.equals(null));
    }

    @Test
    public void testEquals_withSelf() {
        IndexedInt indexed = IndexedInt.of(42, 5);
        assertTrue(indexed.equals(indexed));
    }

    @Test
    public void testEquals_SameObject() {
        IndexedInt indexedInt = IndexedInt.of(42, 5);

        assertTrue(indexedInt.equals(indexedInt));
    }

    @Test
    public void testEquals_Null() {
        IndexedInt indexedInt = IndexedInt.of(42, 5);

        assertFalse(indexedInt.equals(null));
    }

    @Test
    public void testEquals_EdgeCases() {
        IndexedInt zero = IndexedInt.of(0, 0);
        IndexedInt maxValue = IndexedInt.of(Integer.MAX_VALUE, 0);
        IndexedInt minValue = IndexedInt.of(Integer.MIN_VALUE, 0);

        assertTrue(zero.equals(IndexedInt.of(0, 0)));
        assertTrue(maxValue.equals(IndexedInt.of(Integer.MAX_VALUE, 0)));
        assertTrue(minValue.equals(IndexedInt.of(Integer.MIN_VALUE, 0)));

        assertFalse(zero.equals(maxValue));
        assertFalse(maxValue.equals(minValue));
    }

    @Test
    public void testToString_LongIndex() {
        int value = 100;
        long index = 1000000L;

        IndexedInt indexedInt = IndexedInt.of(value, index);

        assertEquals("[1000000]=100", indexedInt.toString());
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
    public void testToString_NegativeValue() {
        int value = -42;
        int index = 10;

        IndexedInt indexedInt = IndexedInt.of(value, index);

        assertEquals("[10]=-42", indexedInt.toString());
    }

    @Test
    public void testToString_EdgeCases() {
        IndexedInt zero = IndexedInt.of(0, 0);
        IndexedInt maxValue = IndexedInt.of(Integer.MAX_VALUE, Long.MAX_VALUE);
        IndexedInt minValue = IndexedInt.of(Integer.MIN_VALUE, 0);

        assertEquals("[0]=0", zero.toString());
        assertEquals("[" + Long.MAX_VALUE + "]=" + Integer.MAX_VALUE, maxValue.toString());
        assertEquals("[0]=" + Integer.MIN_VALUE, minValue.toString());
    }

}
