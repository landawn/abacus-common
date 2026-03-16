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
public class IndexedBooleanTest extends TestBase {

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
            indexed.index(); // Should throw ArithmeticException
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
        IndexedBoolean indexedTrue = IndexedBoolean.of(true, 10);
        IndexedBoolean indexedFalse = IndexedBoolean.of(false, 10);

        assertEquals(10, indexedTrue.hashCode());
        assertEquals(41, indexedFalse.hashCode());
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

        assertTrue(trueFive.equals(IndexedBoolean.of(true, 5)));
        assertTrue(falseFive.equals(IndexedBoolean.of(false, 5)));

        assertFalse(trueFive.equals(trueSix));
        assertFalse(trueFive.equals(falseFive));
        assertFalse(trueFive.equals(falseSix));
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
