package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IndexedFloatTest extends TestBase {

    @Test
    public void testOfIntIndex() {
        IndexedFloat indexed = IndexedFloat.of(3.14f, 5);
        assertEquals(3.14f, indexed.value(), 0.001f);
        assertEquals(5, indexed.index());
    }

    @Test
    public void testOfLongIndex() {
        IndexedFloat indexed = IndexedFloat.of(2.71f, 1000000000L);
        assertEquals(2.71f, indexed.value(), 0.001f);
        assertEquals(1000000000L, indexed.longIndex());
    }

    @Test
    public void testNaN() {
        IndexedFloat indexed = IndexedFloat.of(Float.NaN, 0);
        assertTrue(Float.isNaN(indexed.value()));
    }

    @Test
    public void testOf_WithIntIndex() {
        float value = 3.14f;
        int index = 5;

        IndexedFloat indexedFloat = IndexedFloat.of(value, index);

        assertEquals(value, indexedFloat.value());
        assertEquals(index, indexedFloat.index());
    }

    @Test
    public void testOf_WithLongIndex() {
        float value = 3.14f;
        long index = 5000000000L;

        IndexedFloat indexedFloat = IndexedFloat.of(value, index);

        assertEquals(value, indexedFloat.value());
        assertEquals(index, indexedFloat.longIndex());
    }

    @Test
    public void testZeroValue() {
        IndexedFloat indexed = IndexedFloat.of(0.0f, 0);
        assertEquals(0.0f, indexed.value(), 0.0f);
    }

    @Test
    public void testOf_WithIntIndex_Zero() {
        float value = 0.0f;
        int index = 0;

        IndexedFloat indexedFloat = IndexedFloat.of(value, index);

        assertEquals(value, indexedFloat.value());
        assertEquals(index, indexedFloat.index());
    }

    @Test
    public void testOf_WithIntIndex_MaxValue() {
        float value = Float.MAX_VALUE;
        int index = Integer.MAX_VALUE;

        IndexedFloat indexedFloat = IndexedFloat.of(value, index);

        assertEquals(value, indexedFloat.value());
        assertEquals(index, indexedFloat.index());
    }

    @Test
    public void testOf_WithLongIndex_Zero() {
        float value = -100.5f;
        long index = 0L;

        IndexedFloat indexedFloat = IndexedFloat.of(value, index);

        assertEquals(value, indexedFloat.value());
        assertEquals(index, indexedFloat.index());
    }

    @Test
    public void testOf_WithLongIndex_MaxValue() {
        float value = Float.MIN_VALUE;
        long index = Long.MAX_VALUE;

        IndexedFloat indexedFloat = IndexedFloat.of(value, index);

        assertEquals(value, indexedFloat.value());
        assertEquals(index, indexedFloat.longIndex());
    }

    @Test
    public void testOfNegativeIntIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedFloat.of(1.0f, -1);
        });
    }

    @Test
    public void testOf_WithIntIndex_NegativeIndex() {
        float value = 3.14f;
        int index = -1;

        assertThrows(IllegalArgumentException.class, () -> IndexedFloat.of(value, index));
    }

    @Test
    public void testOf_WithLongIndex_NegativeIndex() {
        float value = 3.14f;
        long index = -1L;

        assertThrows(IllegalArgumentException.class, () -> IndexedFloat.of(value, index));
    }

    @Test
    public void testValue() {
        IndexedFloat indexed = IndexedFloat.of(123.456f, 0);
        assertEquals(123.456f, indexed.value(), 0.001f);
    }

    @Test
    public void testHashCode() {
        IndexedFloat indexed1 = IndexedFloat.of(3.14f, 5);
        IndexedFloat indexed2 = IndexedFloat.of(3.14f, 5);
        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        int index = 5;

        IndexedFloat indexedFloat1 = IndexedFloat.of(3.14f, index);
        IndexedFloat indexedFloat2 = IndexedFloat.of(2.71f, index);

        assertNotEquals(indexedFloat1.hashCode(), indexedFloat2.hashCode());
    }

    @Test
    public void testHashCode_DifferentIndices() {
        float value = 3.14f;

        IndexedFloat indexedFloat1 = IndexedFloat.of(value, 5);
        IndexedFloat indexedFloat2 = IndexedFloat.of(value, 6);

        assertNotEquals(indexedFloat1.hashCode(), indexedFloat2.hashCode());
    }

    @Test
    public void testHashCode_SpecialValues() {
        IndexedFloat nanIndexed1 = IndexedFloat.of(Float.NaN, 0);
        IndexedFloat nanIndexed2 = IndexedFloat.of(Float.NaN, 0);
        assertEquals(nanIndexed1.hashCode(), nanIndexed2.hashCode());

        IndexedFloat posInf = IndexedFloat.of(Float.POSITIVE_INFINITY, 0);
        IndexedFloat negInf = IndexedFloat.of(Float.NEGATIVE_INFINITY, 0);
        assertNotEquals(posInf.hashCode(), negInf.hashCode());
    }

    @Test
    public void testEquals() {
        IndexedFloat indexed1 = IndexedFloat.of(3.14f, 5);
        IndexedFloat indexed2 = IndexedFloat.of(3.14f, 5);
        IndexedFloat indexed3 = IndexedFloat.of(2.71f, 5);

        assertTrue(indexed1.equals(indexed2));
        assertFalse(indexed1.equals(indexed3));
    }

    @Test
    public void testEquals_EqualObjects() {
        float value = 3.14f;
        int index = 5;

        IndexedFloat indexedFloat1 = IndexedFloat.of(value, index);
        IndexedFloat indexedFloat2 = IndexedFloat.of(value, index);

        assertTrue(indexedFloat1.equals(indexedFloat2));
        assertTrue(indexedFloat2.equals(indexedFloat1));
    }

    @Test
    public void testEquals_DifferentValues() {
        int index = 5;

        IndexedFloat indexedFloat1 = IndexedFloat.of(3.14f, index);
        IndexedFloat indexedFloat2 = IndexedFloat.of(2.71f, index);

        assertFalse(indexedFloat1.equals(indexedFloat2));
        assertFalse(indexedFloat2.equals(indexedFloat1));
    }

    @Test
    public void testEquals_DifferentIndices() {
        float value = 3.14f;

        IndexedFloat indexedFloat1 = IndexedFloat.of(value, 5);
        IndexedFloat indexedFloat2 = IndexedFloat.of(value, 6);

        assertFalse(indexedFloat1.equals(indexedFloat2));
        assertFalse(indexedFloat2.equals(indexedFloat1));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedFloat indexedFloat = IndexedFloat.of(3.14f, 5);

        assertFalse(indexedFloat.equals("not an IndexedFloat"));
        assertFalse(indexedFloat.equals(3.14f));
    }

    @Test
    public void testEquals_SameObject() {
        IndexedFloat indexedFloat = IndexedFloat.of(3.14f, 5);

        assertTrue(indexedFloat.equals(indexedFloat));
    }

    @Test
    public void testEquals_Null() {
        IndexedFloat indexedFloat = IndexedFloat.of(3.14f, 5);

        assertFalse(indexedFloat.equals(null));
    }

    @Test
    public void testEquals_SpecialValues() {
        IndexedFloat nan1 = IndexedFloat.of(Float.NaN, 5);
        IndexedFloat nan2 = IndexedFloat.of(Float.NaN, 5);
        assertTrue(nan1.equals(nan2));

        IndexedFloat posZero = IndexedFloat.of(0.0f, 5);
        IndexedFloat negZero = IndexedFloat.of(-0.0f, 5);
        assertFalse(posZero.equals(negZero));

        IndexedFloat posInf1 = IndexedFloat.of(Float.POSITIVE_INFINITY, 5);
        IndexedFloat posInf2 = IndexedFloat.of(Float.POSITIVE_INFINITY, 5);
        IndexedFloat negInf = IndexedFloat.of(Float.NEGATIVE_INFINITY, 5);

        assertTrue(posInf1.equals(posInf2));
        assertFalse(posInf1.equals(negInf));
    }

    @Test
    public void testToString() {
        IndexedFloat indexed = IndexedFloat.of(3.14f, 5);
        assertTrue(indexed.toString().contains("[5]="));
        assertTrue(indexed.toString().contains("3.14"));
    }

    @Test
    public void testToString_LongIndex() {
        float value = 100.001f;
        long index = 1000000L;

        IndexedFloat indexedFloat = IndexedFloat.of(value, index);

        assertEquals("[1000000]=100.001", indexedFloat.toString());
    }

    @Test
    public void testToString_NegativeValue() {
        float value = -42.42f;
        int index = 10;

        IndexedFloat indexedFloat = IndexedFloat.of(value, index);

        assertEquals("[10]=-42.42", indexedFloat.toString());
    }

    @Test
    public void testToString_SpecialValues() {
        IndexedFloat nanIndexed = IndexedFloat.of(Float.NaN, 0);
        IndexedFloat posInfIndexed = IndexedFloat.of(Float.POSITIVE_INFINITY, 1);
        IndexedFloat negInfIndexed = IndexedFloat.of(Float.NEGATIVE_INFINITY, 2);

        assertEquals("[0]=NaN", nanIndexed.toString());
        assertEquals("[1]=Infinity", posInfIndexed.toString());
        assertEquals("[2]=-Infinity", negInfIndexed.toString());
    }

}
