package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IndexedShort100Test extends TestBase {

    @Test
    public void testOf_WithIntIndex() {
        short value = (short) 42;
        int index = 5;
        
        IndexedShort indexedShort = IndexedShort.of(value, index);
        
        assertEquals(value, indexedShort.value());
        assertEquals(index, indexedShort.index());
    }

    @Test
    public void testOf_WithIntIndex_Zero() {
        short value = (short) 0;
        int index = 0;
        
        IndexedShort indexedShort = IndexedShort.of(value, index);
        
        assertEquals(value, indexedShort.value());
        assertEquals(index, indexedShort.index());
    }

    @Test
    public void testOf_WithIntIndex_MaxValue() {
        short value = Short.MAX_VALUE;
        int index = Integer.MAX_VALUE;
        
        IndexedShort indexedShort = IndexedShort.of(value, index);
        
        assertEquals(value, indexedShort.value());
        assertEquals(index, indexedShort.index());
    }

    @Test
    public void testOf_WithIntIndex_NegativeIndex() {
        short value = (short) 42;
        int index = -1;
        
        assertThrows(IllegalArgumentException.class, () -> IndexedShort.of(value, index));
    }

    @Test
    public void testOf_WithLongIndex() {
        short value = (short) 42;
        long index = 5000000000L;
        
        IndexedShort indexedShort = IndexedShort.of(value, index);
        
        assertEquals(value, indexedShort.value());
        assertEquals(index, indexedShort.longIndex());
    }

    @Test
    public void testOf_WithLongIndex_Zero() {
        short value = (short) -100;
        long index = 0L;
        
        IndexedShort indexedShort = IndexedShort.of(value, index);
        
        assertEquals(value, indexedShort.value());
        assertEquals(index, indexedShort.index());
    }

    @Test
    public void testOf_WithLongIndex_MaxValue() {
        short value = Short.MIN_VALUE;
        long index = Long.MAX_VALUE;
        
        IndexedShort indexedShort = IndexedShort.of(value, index);
        
        assertEquals(value, indexedShort.value());
        assertEquals(index, indexedShort.longIndex());
    }

    @Test
    public void testOf_WithLongIndex_NegativeIndex() {
        short value = (short) 42;
        long index = -1L;
        
        assertThrows(IllegalArgumentException.class, () -> IndexedShort.of(value, index));
    }

    @Test
    public void testValue() {
        short[] testValues = {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE};
        
        for (short value : testValues) {
            IndexedShort indexedShort = IndexedShort.of(value, 0);
            assertEquals(value, indexedShort.value());
        }
    }

    @Test
    public void testHashCode() {
        short value = (short) 42;
        int index = 5;
        
        IndexedShort indexedShort1 = IndexedShort.of(value, index);
        IndexedShort indexedShort2 = IndexedShort.of(value, index);
        
        assertEquals(indexedShort1.hashCode(), indexedShort2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        int index = 5;
        
        IndexedShort indexedShort1 = IndexedShort.of((short) 42, index);
        IndexedShort indexedShort2 = IndexedShort.of((short) 43, index);
        
        assertNotEquals(indexedShort1.hashCode(), indexedShort2.hashCode());
    }

    @Test
    public void testHashCode_DifferentIndices() {
        short value = (short) 42;
        
        IndexedShort indexedShort1 = IndexedShort.of(value, 5);
        IndexedShort indexedShort2 = IndexedShort.of(value, 6);
        
        assertNotEquals(indexedShort1.hashCode(), indexedShort2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        IndexedShort indexedShort = IndexedShort.of((short) 42, 5);
        
        assertTrue(indexedShort.equals(indexedShort));
    }

    @Test
    public void testEquals_EqualObjects() {
        short value = (short) 42;
        int index = 5;
        
        IndexedShort indexedShort1 = IndexedShort.of(value, index);
        IndexedShort indexedShort2 = IndexedShort.of(value, index);
        
        assertTrue(indexedShort1.equals(indexedShort2));
        assertTrue(indexedShort2.equals(indexedShort1));
    }

    @Test
    public void testEquals_DifferentValues() {
        int index = 5;
        
        IndexedShort indexedShort1 = IndexedShort.of((short) 42, index);
        IndexedShort indexedShort2 = IndexedShort.of((short) 43, index);
        
        assertFalse(indexedShort1.equals(indexedShort2));
        assertFalse(indexedShort2.equals(indexedShort1));
    }

    @Test
    public void testEquals_DifferentIndices() {
        short value = (short) 42;
        
        IndexedShort indexedShort1 = IndexedShort.of(value, 5);
        IndexedShort indexedShort2 = IndexedShort.of(value, 6);
        
        assertFalse(indexedShort1.equals(indexedShort2));
        assertFalse(indexedShort2.equals(indexedShort1));
    }

    @Test
    public void testEquals_Null() {
        IndexedShort indexedShort = IndexedShort.of((short) 42, 5);
        
        assertFalse(indexedShort.equals(null));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedShort indexedShort = IndexedShort.of((short) 42, 5);
        
        assertFalse(indexedShort.equals("not an IndexedShort"));
        assertFalse(indexedShort.equals(42));
    }

    @Test
    public void testToString() {
        short value = (short) 42;
        int index = 5;
        
        IndexedShort indexedShort = IndexedShort.of(value, index);
        
        assertEquals("[5]=42", indexedShort.toString());
    }

    @Test
    public void testToString_NegativeValue() {
        short value = (short) -42;
        int index = 10;
        
        IndexedShort indexedShort = IndexedShort.of(value, index);
        
        assertEquals("[10]=-42", indexedShort.toString());
    }

    @Test
    public void testToString_LongIndex() {
        short value = (short) 100;
        long index = 1000000L;
        
        IndexedShort indexedShort = IndexedShort.of(value, index);
        
        assertEquals("[1000000]=100", indexedShort.toString());
    }
}