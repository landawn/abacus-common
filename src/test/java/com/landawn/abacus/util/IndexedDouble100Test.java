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
public class IndexedDouble100Test extends TestBase {

    @Test
    public void testOf_WithIntIndex() {
        double value = 3.14159;
        int index = 5;

        IndexedDouble indexedDouble = IndexedDouble.of(value, index);

        assertEquals(value, indexedDouble.value());
        assertEquals(index, indexedDouble.index());
    }

    @Test
    public void testOf_WithIntIndex_Zero() {
        double value = 0.0;
        int index = 0;

        IndexedDouble indexedDouble = IndexedDouble.of(value, index);

        assertEquals(value, indexedDouble.value());
        assertEquals(index, indexedDouble.index());
    }

    @Test
    public void testOf_WithIntIndex_MaxValue() {
        double value = Double.MAX_VALUE;
        int index = Integer.MAX_VALUE;

        IndexedDouble indexedDouble = IndexedDouble.of(value, index);

        assertEquals(value, indexedDouble.value());
        assertEquals(index, indexedDouble.index());
    }

    @Test
    public void testOf_WithIntIndex_NegativeIndex() {
        double value = 3.14159;
        int index = -1;

        assertThrows(IllegalArgumentException.class, () -> IndexedDouble.of(value, index));
    }

    @Test
    public void testOf_WithLongIndex() {
        double value = 3.14159;
        long index = 5000000000L;

        IndexedDouble indexedDouble = IndexedDouble.of(value, index);

        assertEquals(value, indexedDouble.value());
        assertEquals(index, indexedDouble.longIndex());
    }

    @Test
    public void testOf_WithLongIndex_Zero() {
        double value = -100.5;
        long index = 0L;

        IndexedDouble indexedDouble = IndexedDouble.of(value, index);

        assertEquals(value, indexedDouble.value());
        assertEquals(index, indexedDouble.index());
    }

    @Test
    public void testOf_WithLongIndex_MaxValue() {
        double value = Double.MIN_VALUE;
        long index = Long.MAX_VALUE;

        IndexedDouble indexedDouble = IndexedDouble.of(value, index);

        assertEquals(value, indexedDouble.value());
        assertEquals(index, indexedDouble.longIndex());
    }

    @Test
    public void testOf_WithLongIndex_NegativeIndex() {
        double value = 3.14159;
        long index = -1L;

        assertThrows(IllegalArgumentException.class, () -> IndexedDouble.of(value, index));
    }

    @Test
    public void testValue() {
        double[] testValues = { Double.MIN_VALUE, -1.0, 0.0, 1.0, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };

        for (double value : testValues) {
            IndexedDouble indexedDouble = IndexedDouble.of(value, 0);
            if (Double.isNaN(value)) {
                assertTrue(Double.isNaN(indexedDouble.value()));
            } else {
                assertEquals(value, indexedDouble.value());
            }
        }
    }

    @Test
    public void testHashCode() {
        double value = 3.14159;
        int index = 5;

        IndexedDouble indexedDouble1 = IndexedDouble.of(value, index);
        IndexedDouble indexedDouble2 = IndexedDouble.of(value, index);

        assertEquals(indexedDouble1.hashCode(), indexedDouble2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        int index = 5;

        IndexedDouble indexedDouble1 = IndexedDouble.of(3.14159, index);
        IndexedDouble indexedDouble2 = IndexedDouble.of(2.71828, index);

        assertNotEquals(indexedDouble1.hashCode(), indexedDouble2.hashCode());
    }

    @Test
    public void testHashCode_DifferentIndices() {
        double value = 3.14159;

        IndexedDouble indexedDouble1 = IndexedDouble.of(value, 5);
        IndexedDouble indexedDouble2 = IndexedDouble.of(value, 6);

        assertNotEquals(indexedDouble1.hashCode(), indexedDouble2.hashCode());
    }

    @Test
    public void testHashCode_SpecialValues() {
        IndexedDouble nanIndexed1 = IndexedDouble.of(Double.NaN, 0);
        IndexedDouble nanIndexed2 = IndexedDouble.of(Double.NaN, 0);
        assertEquals(nanIndexed1.hashCode(), nanIndexed2.hashCode());

        IndexedDouble posInf = IndexedDouble.of(Double.POSITIVE_INFINITY, 0);
        IndexedDouble negInf = IndexedDouble.of(Double.NEGATIVE_INFINITY, 0);
        assertNotEquals(posInf.hashCode(), negInf.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        IndexedDouble indexedDouble = IndexedDouble.of(3.14159, 5);

        assertTrue(indexedDouble.equals(indexedDouble));
    }

    @Test
    public void testEquals_EqualObjects() {
        double value = 3.14159;
        int index = 5;

        IndexedDouble indexedDouble1 = IndexedDouble.of(value, index);
        IndexedDouble indexedDouble2 = IndexedDouble.of(value, index);

        assertTrue(indexedDouble1.equals(indexedDouble2));
        assertTrue(indexedDouble2.equals(indexedDouble1));
    }

    @Test
    public void testEquals_DifferentValues() {
        int index = 5;

        IndexedDouble indexedDouble1 = IndexedDouble.of(3.14159, index);
        IndexedDouble indexedDouble2 = IndexedDouble.of(2.71828, index);

        assertFalse(indexedDouble1.equals(indexedDouble2));
        assertFalse(indexedDouble2.equals(indexedDouble1));
    }

    @Test
    public void testEquals_DifferentIndices() {
        double value = 3.14159;

        IndexedDouble indexedDouble1 = IndexedDouble.of(value, 5);
        IndexedDouble indexedDouble2 = IndexedDouble.of(value, 6);

        assertFalse(indexedDouble1.equals(indexedDouble2));
        assertFalse(indexedDouble2.equals(indexedDouble1));
    }

    @Test
    public void testEquals_Null() {
        IndexedDouble indexedDouble = IndexedDouble.of(3.14159, 5);

        assertFalse(indexedDouble.equals(null));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedDouble indexedDouble = IndexedDouble.of(3.14159, 5);

        assertFalse(indexedDouble.equals("not an IndexedDouble"));
        assertFalse(indexedDouble.equals(3.14159));
    }

    @Test
    public void testEquals_SpecialValues() {
        IndexedDouble nan1 = IndexedDouble.of(Double.NaN, 5);
        IndexedDouble nan2 = IndexedDouble.of(Double.NaN, 5);
        assertTrue(nan1.equals(nan2));

        IndexedDouble posZero = IndexedDouble.of(0.0, 5);
        IndexedDouble negZero = IndexedDouble.of(-0.0, 5);
        assertFalse(posZero.equals(negZero));

        IndexedDouble posInf1 = IndexedDouble.of(Double.POSITIVE_INFINITY, 5);
        IndexedDouble posInf2 = IndexedDouble.of(Double.POSITIVE_INFINITY, 5);
        IndexedDouble negInf = IndexedDouble.of(Double.NEGATIVE_INFINITY, 5);

        assertTrue(posInf1.equals(posInf2));
        assertFalse(posInf1.equals(negInf));
    }

    @Test
    public void testToString() {
        double value = 3.14159;
        int index = 5;

        IndexedDouble indexedDouble = IndexedDouble.of(value, index);

        assertEquals("[5]=3.14159", indexedDouble.toString());
    }

    @Test
    public void testToString_NegativeValue() {
        double value = -42.42;
        int index = 10;

        IndexedDouble indexedDouble = IndexedDouble.of(value, index);

        assertEquals("[10]=-42.42", indexedDouble.toString());
    }

    @Test
    public void testToString_LongIndex() {
        double value = 100.001;
        long index = 1000000L;

        IndexedDouble indexedDouble = IndexedDouble.of(value, index);

        assertEquals("[1000000]=100.001", indexedDouble.toString());
    }

    @Test
    public void testToString_SpecialValues() {
        IndexedDouble nanIndexed = IndexedDouble.of(Double.NaN, 0);
        IndexedDouble posInfIndexed = IndexedDouble.of(Double.POSITIVE_INFINITY, 1);
        IndexedDouble negInfIndexed = IndexedDouble.of(Double.NEGATIVE_INFINITY, 2);

        assertEquals("[0]=NaN", nanIndexed.toString());
        assertEquals("[1]=Infinity", posInfIndexed.toString());
        assertEquals("[2]=-Infinity", negInfIndexed.toString());
    }
}
