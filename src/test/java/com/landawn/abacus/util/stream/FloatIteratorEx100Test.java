package com.landawn.abacus.util.stream;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;

@Tag("new-test")
public class FloatIteratorEx100Test extends TestBase {

    @Test
    public void testEmpty() {
        FloatIteratorEx iter = FloatIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
        Assertions.assertEquals(0, iter.count());
        Assertions.assertArrayEquals(new float[0], iter.toArray());
        iter.close();
    }

    @Test
    public void testEmptyConstant() {
        FloatIteratorEx iter1 = FloatIteratorEx.EMPTY;
        FloatIteratorEx iter2 = FloatIteratorEx.empty();
        Assertions.assertSame(iter1, iter2);
    }

    @Test
    public void testOfArray() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        FloatIteratorEx iter = FloatIteratorEx.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1.1f, iter.nextFloat());
        Assertions.assertEquals(2.2f, iter.nextFloat());
        Assertions.assertEquals(3.3f, iter.nextFloat());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    public void testOfEmptyArray() {
        float[] array = {};
        FloatIteratorEx iter = FloatIteratorEx.of(array);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(FloatIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndices() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatIteratorEx iter = FloatIteratorEx.of(array, 1, 4);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(2.0f, iter.nextFloat());
        Assertions.assertEquals(3.0f, iter.nextFloat());
        Assertions.assertEquals(4.0f, iter.nextFloat());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        float[] array = { 1.0f, 2.0f, 3.0f };
        FloatIteratorEx iter = FloatIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertSame(FloatIteratorEx.EMPTY, iter);
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        float[] array = { 1.0f, 2.0f, 3.0f };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> FloatIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> FloatIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> FloatIteratorEx.of(array, 0, 4));
    }

    @Test
    public void testOfFloatIterator() {
        FloatIterator baseIter = new FloatIterator() {
            private int index = 0;
            private float[] data = { 1.5f, 2.5f, 3.5f };

            @Override
            public boolean hasNext() {
                return index < data.length;
            }

            @Override
            public float nextFloat() {
                return data[index++];
            }
        };

        FloatIteratorEx iter = FloatIteratorEx.of(baseIter);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1.5f, iter.nextFloat());
        Assertions.assertEquals(2.5f, iter.nextFloat());
        Assertions.assertEquals(3.5f, iter.nextFloat());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfFloatIteratorNull() {
        FloatIterator nullIterator = null;
        FloatIteratorEx iter = FloatIteratorEx.of(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfFloatIteratorAlreadyFloatIteratorEx() {
        FloatIteratorEx original = FloatIteratorEx.of(1.0f, 2.0f, 3.0f);
        FloatIteratorEx wrapped = FloatIteratorEx.of(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testFromIterator() {
        List<Float> list = Arrays.asList(1.1f, 2.2f, 3.3f);
        FloatIteratorEx iter = FloatIteratorEx.from(list.iterator());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(1.1f, iter.nextFloat());
        Assertions.assertEquals(2.2f, iter.nextFloat());
        Assertions.assertEquals(3.3f, iter.nextFloat());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromIteratorNull() {
        Iterator<Float> nullIterator = null;
        FloatIteratorEx iter = FloatIteratorEx.from(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testFromObjIteratorEx() {
        ObjIteratorEx<Float> objIter = ObjIteratorEx.of(1.0f, 2.0f, 3.0f);
        FloatIteratorEx iter = FloatIteratorEx.from(objIter);

        iter.advance(1);
        Assertions.assertEquals(2.0f, iter.nextFloat());

        Assertions.assertEquals(1, iter.count());

        iter.close();
    }

    @Test
    public void testAdvance() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatIteratorEx iter = FloatIteratorEx.of(array);

        iter.advance(2);
        Assertions.assertEquals(3.0f, iter.nextFloat());

        iter.advance(1);
        Assertions.assertEquals(5.0f, iter.nextFloat());

        iter.advance(10);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        float[] array = { 1.0f, 2.0f, 3.0f };
        FloatIteratorEx iter = FloatIteratorEx.of(array);

        iter.advance(0);
        Assertions.assertEquals(1.0f, iter.nextFloat());
    }

    @Test
    public void testAdvanceNegative() {
        FloatIteratorEx iter = FloatIteratorEx.of(1.0f, 2.0f, 3.0f);
        iter.advance(-1);
    }

    @Test
    public void testCount() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatIteratorEx iter = FloatIteratorEx.of(array, 1, 4);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        FloatIteratorEx iter = FloatIteratorEx.of(array);

        float[] result = iter.toArray();
        Assertions.assertArrayEquals(array, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayPartial() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatIteratorEx iter = FloatIteratorEx.of(array, 1, 4);

        float[] result = iter.toArray();
        Assertions.assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f }, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToList() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        FloatIteratorEx iter = FloatIteratorEx.of(array);

        FloatList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1.1f, result.get(0));
        Assertions.assertEquals(2.2f, result.get(1));
        Assertions.assertEquals(3.3f, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToListPartial() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatIteratorEx iter = FloatIteratorEx.of(array, 1, 4);

        FloatList result = iter.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(2.0f, result.get(0));
        Assertions.assertEquals(3.0f, result.get(1));
        Assertions.assertEquals(4.0f, result.get(2));
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testClose() {
        FloatIteratorEx iter = FloatIteratorEx.of(1.0f, 2.0f, 3.0f);
        iter.close();
    }

    @Test
    public void testFloatSpecialValues() {
        float[] array = { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f, -0.0f };
        FloatIteratorEx iter = FloatIteratorEx.of(array);

        Assertions.assertTrue(Float.isNaN(iter.nextFloat()));
        Assertions.assertEquals(Float.POSITIVE_INFINITY, iter.nextFloat());
        Assertions.assertEquals(Float.NEGATIVE_INFINITY, iter.nextFloat());
        Assertions.assertEquals(0.0f, iter.nextFloat());
        Assertions.assertEquals(-0.0f, iter.nextFloat());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testLargeArrayAdvance() {
        float[] array = new float[1000];
        for (int i = 0; i < array.length; i++) {
            array[i] = i * 1.0f;
        }

        FloatIteratorEx iter = FloatIteratorEx.of(array);
        iter.advance(500);
        Assertions.assertEquals(500.0f, iter.nextFloat());

        iter.advance(498);
        Assertions.assertEquals(999.0f, iter.nextFloat());
        Assertions.assertFalse(iter.hasNext());
    }
}
