package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IndexedBoolean100Test extends TestBase {

    @Test
    public void testOf_WithIntIndex() {
        boolean value = true;
        int index = 5;
        
        IndexedBoolean indexedBoolean = IndexedBoolean.of(value, index);
        
        assertEquals(value, indexedBoolean.value());
        assertEquals(index, indexedBoolean.index());
    }

    @Test
    public void testOf_WithIntIndex_Zero() {
        boolean value = false;
        int index = 0;
        
        IndexedBoolean indexedBoolean = IndexedBoolean.of(value, index);
        
        assertEquals(value, indexedBoolean.value());
        assertEquals(index, indexedBoolean.index());
    }

    @Test
    public void testOf_WithIntIndex_MaxValue() {
        boolean value = true;
        int index = Integer.MAX_VALUE;
        
        IndexedBoolean indexedBoolean = IndexedBoolean.of(value, index);
        
        assertEquals(value, indexedBoolean.value());
        assertEquals(index, indexedBoolean.index());
    }

    @Test
    public void testOf_WithIntIndex_NegativeIndex() {
        boolean value = true;
        int index = -1;
        
        assertThrows(IllegalArgumentException.class, () -> IndexedBoolean.of(value, index));
    }

    @Test
    public void testOf_WithLongIndex() {
        boolean value = true;
        long index = 5000000000L;
        
        IndexedBoolean indexedBoolean = IndexedBoolean.of(value, index);
        
        assertEquals(value, indexedBoolean.value());
        assertEquals(index, indexedBoolean.longIndex());
    }

    @Test
    public void testOf_WithLongIndex_Zero() {
        boolean value = false;
        long index = 0L;
        
        IndexedBoolean indexedBoolean = IndexedBoolean.of(value, index);
        
        assertEquals(value, indexedBoolean.value());
        assertEquals(index, indexedBoolean.index());
    }

    @Test
    public void testOf_WithLongIndex_MaxValue() {
        boolean value = false;
        long index = Long.MAX_VALUE;
        
        IndexedBoolean indexedBoolean = IndexedBoolean.of(value, index);
        
        assertEquals(value, indexedBoolean.value());
        assertEquals(index, indexedBoolean.longIndex());
    }

    @Test
    public void testOf_WithLongIndex_NegativeIndex() {
        boolean value = true;
        long index = -1L;
        
        assertThrows(IllegalArgumentException.class, () -> IndexedBoolean.of(value, index));
    }

    @Test
    public void testValue() {
        IndexedBoolean indexedTrue = IndexedBoolean.of(true, 0);
        IndexedBoolean indexedFalse = IndexedBoolean.of(false, 0);
        
        assertTrue(indexedTrue.value());
        assertFalse(indexedFalse.value());
    }

    @Test
    public void testHashCode() {
        IndexedBoolean indexedBoolean1 = IndexedBoolean.of(true, 5);
        IndexedBoolean indexedBoolean2 = IndexedBoolean.of(true, 5);
        
        assertEquals(indexedBoolean1.hashCode(), indexedBoolean2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        int index = 5;
        
        IndexedBoolean indexedTrue = IndexedBoolean.of(true, index);
        IndexedBoolean indexedFalse = IndexedBoolean.of(false, index);
        
        assertNotEquals(indexedTrue.hashCode(), indexedFalse.hashCode());
    }

    @Test
    public void testHashCode_DifferentIndices() {
        boolean value = true;
        
        IndexedBoolean indexedBoolean1 = IndexedBoolean.of(value, 5);
        IndexedBoolean indexedBoolean2 = IndexedBoolean.of(value, 6);
        
        assertNotEquals(indexedBoolean1.hashCode(), indexedBoolean2.hashCode());
    }

    @Test
    public void testHashCode_SpecificValues() {
        // Test the specific formula: (int) index + (value ? 0 : 31)
        IndexedBoolean indexedTrue = IndexedBoolean.of(true, 10);
        IndexedBoolean indexedFalse = IndexedBoolean.of(false, 10);
        
        assertEquals(10, indexedTrue.hashCode());
        assertEquals(41, indexedFalse.hashCode()); // 10 + 31
    }

    @Test
    public void testEquals_SameObject() {
        IndexedBoolean indexedBoolean = IndexedBoolean.of(true, 5);
        
        assertTrue(indexedBoolean.equals(indexedBoolean));
    }

    @Test
    public void testEquals_EqualObjects() {
        boolean value = true;
        int index = 5;
        
        IndexedBoolean indexedBoolean1 = IndexedBoolean.of(value, index);
        IndexedBoolean indexedBoolean2 = IndexedBoolean.of(value, index);
        
        assertTrue(indexedBoolean1.equals(indexedBoolean2));
        assertTrue(indexedBoolean2.equals(indexedBoolean1));
    }

    @Test
    public void testEquals_DifferentValues() {
        int index = 5;
        
        IndexedBoolean indexedTrue = IndexedBoolean.of(true, index);
        IndexedBoolean indexedFalse = IndexedBoolean.of(false, index);
        
        assertFalse(indexedTrue.equals(indexedFalse));
        assertFalse(indexedFalse.equals(indexedTrue));
    }

    @Test
    public void testEquals_DifferentIndices() {
        boolean value = true;
        
        IndexedBoolean indexedBoolean1 = IndexedBoolean.of(value, 5);
        IndexedBoolean indexedBoolean2 = IndexedBoolean.of(value, 6);
        
        assertFalse(indexedBoolean1.equals(indexedBoolean2));
        assertFalse(indexedBoolean2.equals(indexedBoolean1));
    }

    @Test
    public void testEquals_Null() {
        IndexedBoolean indexedBoolean = IndexedBoolean.of(true, 5);
        
        assertFalse(indexedBoolean.equals(null));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedBoolean indexedBoolean = IndexedBoolean.of(true, 5);
        
        assertFalse(indexedBoolean.equals("not an IndexedBoolean"));
        assertFalse(indexedBoolean.equals(true));
        assertFalse(indexedBoolean.equals(5));
    }

    @Test
    public void testEquals_AllCombinations() {
        IndexedBoolean trueFive = IndexedBoolean.of(true, 5);
        IndexedBoolean trueSix = IndexedBoolean.of(true, 6);
        IndexedBoolean falseFive = IndexedBoolean.of(false, 5);
        IndexedBoolean falseSix = IndexedBoolean.of(false, 6);
        
        // Same value and index
        assertTrue(trueFive.equals(IndexedBoolean.of(true, 5)));
        assertTrue(falseFive.equals(IndexedBoolean.of(false, 5)));
        
        // Different combinations
        assertFalse(trueFive.equals(trueSix));
        assertFalse(trueFive.equals(falseFive));
        assertFalse(trueFive.equals(falseSix));
    }

    @Test
    public void testToString() {
        IndexedBoolean indexedTrue = IndexedBoolean.of(true, 5);
        IndexedBoolean indexedFalse = IndexedBoolean.of(false, 10);
        
        assertEquals("[5]=true", indexedTrue.toString());
        assertEquals("[10]=false", indexedFalse.toString());
    }

    @Test
    public void testToString_LongIndex() {
        IndexedBoolean indexedBoolean = IndexedBoolean.of(true, 1000000L);
        
        assertEquals("[1000000]=true", indexedBoolean.toString());
    }

    @Test
    public void testToString_EdgeCases() {
        IndexedBoolean zeroIndexTrue = IndexedBoolean.of(true, 0);
        IndexedBoolean zeroIndexFalse = IndexedBoolean.of(false, 0);
        IndexedBoolean maxIndexTrue = IndexedBoolean.of(true, Long.MAX_VALUE);
        
        assertEquals("[0]=true", zeroIndexTrue.toString());
        assertEquals("[0]=false", zeroIndexFalse.toString());
        assertEquals("[" + Long.MAX_VALUE + "]=true", maxIndexTrue.toString());
    }
}