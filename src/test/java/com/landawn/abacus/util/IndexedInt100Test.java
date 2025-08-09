package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IndexedInt100Test extends TestBase {

    @Test
    public void testOf_WithIntIndex() {
        int value = 42;
        int index = 5;
        
        IndexedInt indexedInt = IndexedInt.of(value, index);
        
        assertEquals(value, indexedInt.value());
        assertEquals(index, indexedInt.index());
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
    public void testOf_WithIntIndex_NegativeIndex() {
        int value = 42;
        int index = -1;
        
        assertThrows(IllegalArgumentException.class, () -> IndexedInt.of(value, index));
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
    public void testOf_WithLongIndex_NegativeIndex() {
        int value = 42;
        long index = -1L;
        
        assertThrows(IllegalArgumentException.class, () -> IndexedInt.of(value, index));
    }

    @Test
    public void testValue() {
        int[] testValues = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};
        
        for (int value : testValues) {
            IndexedInt indexedInt = IndexedInt.of(value, 0);
            assertEquals(value, indexedInt.value());
        }
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
        // Test the specific formula: (int) index + value * 31
        IndexedInt indexedInt = IndexedInt.of(10, 5);
        assertEquals(5 + 10 * 31, indexedInt.hashCode());
        
        IndexedInt indexedNegative = IndexedInt.of(-10, 5);
        assertEquals(5 + (-10) * 31, indexedNegative.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        IndexedInt indexedInt = IndexedInt.of(42, 5);
        
        assertTrue(indexedInt.equals(indexedInt));
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
    public void testEquals_Null() {
        IndexedInt indexedInt = IndexedInt.of(42, 5);
        
        assertFalse(indexedInt.equals(null));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedInt indexedInt = IndexedInt.of(42, 5);
        
        assertFalse(indexedInt.equals("not an IndexedInt"));
        assertFalse(indexedInt.equals(42));
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
    public void testToString() {
        int value = 42;
        int index = 5;
        
        IndexedInt indexedInt = IndexedInt.of(value, index);
        
        assertEquals("[5]=42", indexedInt.toString());
    }

    @Test
    public void testToString_NegativeValue() {
        int value = -42;
        int index = 10;
        
        IndexedInt indexedInt = IndexedInt.of(value, index);
        
        assertEquals("[10]=-42", indexedInt.toString());
    }

    @Test
    public void testToString_LongIndex() {
        int value = 100;
        long index = 1000000L;
        
        IndexedInt indexedInt = IndexedInt.of(value, index);
        
        assertEquals("[1000000]=100", indexedInt.toString());
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