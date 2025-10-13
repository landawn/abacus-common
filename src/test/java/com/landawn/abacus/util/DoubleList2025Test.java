package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleStream;

@Tag("2025")
public class DoubleList2025Test extends TestBase {

    private DoubleList list;
    private static final double DELTA = 0.000001;

    @BeforeEach
    public void setUp() {
        list = new DoubleList();
    }

    @Test
    public void testDefaultConstructor() {
        DoubleList newList = new DoubleList();
        assertTrue(newList.isEmpty());
        assertEquals(0, newList.size());
    }

    @Test
    public void testConstructorWithCapacity() {
        DoubleList newList = new DoubleList(10);
        assertTrue(newList.isEmpty());
        assertEquals(0, newList.size());
    }

    @Test
    public void testConstructorWithZeroCapacity() {
        DoubleList newList = new DoubleList(0);
        assertTrue(newList.isEmpty());
        assertEquals(0, newList.size());
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new DoubleList(-1));
    }

    @Test
    public void testConstructorWithArray() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList newList = new DoubleList(array);
        assertEquals(5, newList.size());
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], newList.get(i), DELTA);
        }
    }

    @Test
    public void testConstructorWithEmptyArray() {
        double[] array = {};
        DoubleList newList = new DoubleList(array);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new DoubleList(null));
    }

    @Test
    public void testConstructorWithArrayAndSize() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList newList = new DoubleList(array, 3);
        assertEquals(3, newList.size());
        assertEquals(1.1, newList.get(0), DELTA);
        assertEquals(2.2, newList.get(1), DELTA);
        assertEquals(3.3, newList.get(2), DELTA);
    }

    @Test
    public void testConstructorWithArrayAndSizeZero() {
        double[] array = { 1.1, 2.2, 3.3 };
        DoubleList newList = new DoubleList(array, 0);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testConstructorWithArrayAndSizeTooLarge() {
        double[] array = { 1.1, 2.2, 3.3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new DoubleList(array, 4));
    }

    @Test
    public void testConstructorWithArrayAndNegativeSize() {
        double[] array = { 1.1, 2.2, 3.3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new DoubleList(array, -1));
    }

    @Test
    public void testOf() {
        DoubleList newList = DoubleList.of(1.1, 2.2, 3.3, 4.4, 5.5);
        assertEquals(5, newList.size());
        assertEquals(1.1, newList.get(0), DELTA);
        assertEquals(5.5, newList.get(4), DELTA);
    }

    @Test
    public void testOfWithEmptyArray() {
        DoubleList newList = DoubleList.of();
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testOfWithNull() {
        DoubleList newList = DoubleList.of((double[]) null);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testOfWithSingleElement() {
        DoubleList newList = DoubleList.of(42.42);
        assertEquals(1, newList.size());
        assertEquals(42.42, newList.get(0), DELTA);
    }

    @Test
    public void testOfWithArrayAndSize() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList newList = DoubleList.of(array, 3);
        assertEquals(3, newList.size());
        assertEquals(1.1, newList.get(0), DELTA);
        assertEquals(2.2, newList.get(1), DELTA);
        assertEquals(3.3, newList.get(2), DELTA);
    }

    @Test
    public void testOfWithArrayAndSizeZero() {
        double[] array = { 1.1, 2.2, 3.3 };
        DoubleList newList = DoubleList.of(array, 0);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testOfWithArrayAndSizeTooLarge() {
        double[] array = { 1.1, 2.2, 3.3 };
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.of(array, 4));
    }

    @Test
    public void testCopyOf() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList newList = DoubleList.copyOf(array);
        assertEquals(5, newList.size());
        array[0] = 100.0;
        assertEquals(1.1, newList.get(0), DELTA);
    }

    @Test
    public void testCopyOfNull() {
        DoubleList newList = DoubleList.copyOf(null);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testCopyOfRange() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList newList = DoubleList.copyOf(array, 1, 4);
        assertEquals(3, newList.size());
        assertEquals(2.2, newList.get(0), DELTA);
        assertEquals(3.3, newList.get(1), DELTA);
        assertEquals(4.4, newList.get(2), DELTA);
    }

    @Test
    public void testCopyOfRangeEmptyRange() {
        double[] array = { 1.1, 2.2, 3.3 };
        DoubleList newList = DoubleList.copyOf(array, 1, 1);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testRepeat() {
        DoubleList newList = DoubleList.repeat(7.7, 5);
        assertEquals(5, newList.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(7.7, newList.get(i), DELTA);
        }
    }

    @Test
    public void testRepeatZeroTimes() {
        DoubleList newList = DoubleList.repeat(7.7, 0);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testRepeatNegativeTimes() {
        assertThrows(IllegalArgumentException.class, () -> DoubleList.repeat(7.7, -1));
    }

    @Test
    public void testRepeatSpecialValues() {
        DoubleList nanList = DoubleList.repeat(Double.NaN, 3);
        assertEquals(3, nanList.size());
        assertTrue(Double.isNaN(nanList.get(0)));

        DoubleList infList = DoubleList.repeat(Double.POSITIVE_INFINITY, 2);
        assertEquals(2, infList.size());
        assertEquals(Double.POSITIVE_INFINITY, infList.get(0), DELTA);
    }

    @Test
    public void testRandom() {
        DoubleList newList = DoubleList.random(10);
        assertEquals(10, newList.size());
        for (int i = 0; i < 10; i++) {
            double value = newList.get(i);
            assertTrue(value >= 0.0 && value < 1.0);
        }
    }

    @Test
    public void testRandomZeroLength() {
        DoubleList newList = DoubleList.random(0);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testGet() {
        list.add(10.5);
        list.add(20.5);
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(20.5, list.get(1), DELTA);
    }

    @Test
    public void testGetThrowsException() {
        list.add(10.5);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    @Test
    public void testGetNegativeIndex() {
        list.add(10.5);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    public void testSet() {
        list.add(10.5);
        list.add(20.5);
        double oldValue = list.set(1, 30.5);
        assertEquals(20.5, oldValue, DELTA);
        assertEquals(30.5, list.get(1), DELTA);
    }

    @Test
    public void testSetThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, 10.5));
    }

    @Test
    public void testSetNegativeIndex() {
        list.add(10.5);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, 20.5));
    }

    @Test
    public void testSetSpecialValues() {
        list.add(1.1);
        list.set(0, Double.NaN);
        assertTrue(Double.isNaN(list.get(0)));

        list.set(0, Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, list.get(0), DELTA);

        list.set(0, Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, list.get(0), DELTA);

        list.set(0, 0.0);
        assertEquals(0.0, list.get(0), DELTA);

        list.set(0, -0.0);
        assertEquals(-0.0, list.get(0), DELTA);
    }

    @Test
    public void testAdd() {
        list.add(10.5);
        assertEquals(1, list.size());
        assertEquals(10.5, list.get(0), DELTA);

        list.add(20.5);
        assertEquals(2, list.size());
        assertEquals(20.5, list.get(1), DELTA);
    }

    @Test
    public void testAddMultiple() {
        for (int i = 0; i < 100; i++) {
            list.add(i * 1.1);
        }
        assertEquals(100, list.size());
        assertEquals(0.0, list.get(0), DELTA);
        assertEquals(99 * 1.1, list.get(99), DELTA);
    }

    @Test
    public void testAddAtIndex() {
        list.add(10.5);
        list.add(30.5);
        list.add(1, 20.5);

        assertEquals(3, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(20.5, list.get(1), DELTA);
        assertEquals(30.5, list.get(2), DELTA);
    }

    @Test
    public void testAddAtBeginning() {
        list.add(20.5);
        list.add(0, 10.5);
        assertEquals(2, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(20.5, list.get(1), DELTA);
    }

    @Test
    public void testAddAtEnd() {
        list.add(10.5);
        list.add(1, 20.5);
        assertEquals(2, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(20.5, list.get(1), DELTA);
    }

    @Test
    public void testAddAtIndexThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 10.5));
    }

    @Test
    public void testAddAtNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 10.5));
    }

    @Test
    public void testAddAllDoubleList() {
        list.add(1.1);
        list.add(2.2);

        DoubleList other = DoubleList.of(3.3, 4.4, 5.5);
        boolean result = list.addAll(other);

        assertTrue(result);
        assertEquals(5, list.size());
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testAddAllEmpty() {
        list.add(1.1);
        DoubleList empty = new DoubleList();
        boolean result = list.addAll(empty);

        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAllNull() {
        list.add(1.1);
        boolean result = list.addAll((DoubleList) null);
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAllAtIndex() {
        list.add(1.1);
        list.add(4.4);

        DoubleList other = DoubleList.of(2.2, 3.3);
        boolean result = list.addAll(1, other);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(4.4, list.get(3), DELTA);
    }

    @Test
    public void testAddAllAtIndexThrowsException() {
        list.add(1.1);
        DoubleList other = DoubleList.of(2.2, 3.3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(2, other));
    }

    @Test
    public void testAddAllArray() {
        list.add(1.1);
        double[] array = { 2.2, 3.3, 4.4 };
        boolean result = list.addAll(array);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(4.4, list.get(3), DELTA);
    }

    @Test
    public void testAddAllArrayEmpty() {
        list.add(1.1);
        double[] array = {};
        boolean result = list.addAll(array);

        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAllArrayNull() {
        list.add(1.1);
        boolean result = list.addAll((double[]) null);
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAllArrayAtIndex() {
        list.add(1.1);
        list.add(4.4);
        double[] array = { 2.2, 3.3 };
        boolean result = list.addAll(1, array);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testRemove() {
        list.add(10.5);
        list.add(20.5);
        list.add(30.5);

        boolean result = list.remove(20.5);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(30.5, list.get(1), DELTA);
    }

    @Test
    public void testRemoveNotFound() {
        list.add(10.5);
        boolean result = list.remove(20.5);
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveFirstOccurrence() {
        list.add(10.5);
        list.add(20.5);
        list.add(10.5);

        boolean result = list.remove(10.5);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(20.5, list.get(0), DELTA);
        assertEquals(10.5, list.get(1), DELTA);
    }

    @Test
    public void testRemoveSpecialValues() {
        list.add(Double.NaN);
        list.add(1.1);
        boolean result = list.remove(Double.NaN);
        assertTrue(result);
        assertEquals(1, list.size());

        list.add(Double.POSITIVE_INFINITY);
        result = list.remove(Double.POSITIVE_INFINITY);
        assertTrue(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveAllOccurrences() {
        list.add(10.5);
        list.add(20.5);
        list.add(10.5);
        list.add(30.5);
        list.add(10.5);

        boolean result = list.removeAllOccurrences(10.5);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(20.5, list.get(0), DELTA);
        assertEquals(30.5, list.get(1), DELTA);
    }

    @Test
    public void testRemoveAllOccurrencesNoneFound() {
        list.add(10.5);
        list.add(20.5);
        boolean result = list.removeAllOccurrences(30.5);
        assertFalse(result);
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveAllDoubleList() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList toRemove = DoubleList.of(2.2, 4.4);

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(3.3, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);
    }

    @Test
    public void testRemoveAllDoubleListEmpty() {
        list.add(1.1);
        DoubleList toRemove = new DoubleList();
        boolean result = list.removeAll(toRemove);
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveAllArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        double[] toRemove = { 2.2, 4.4 };

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        boolean result = list.removeIf(x -> x > 3.0);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testRemoveIfNoneMatch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        boolean result = list.removeIf(x -> x > 10.0);
        assertFalse(result);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIfAllMatch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        boolean result = list.removeIf(x -> x > 0.0);
        assertTrue(result);
        assertEquals(0, list.size());
    }

    @Test
    public void testRemoveDuplicates() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3, 3.3, 3.3, 4.4 });

        boolean result = list.removeDuplicates();
        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(4.4, list.get(3), DELTA);
    }

    @Test
    public void testRemoveDuplicatesNone() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        boolean result = list.removeDuplicates();
        assertFalse(result);
        assertEquals(4, list.size());
    }

    @Test
    public void testRemoveDuplicatesEmptyList() {
        boolean result = list.removeDuplicates();
        assertFalse(result);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRemoveDuplicatesSingleElement() {
        list.add(1.1);
        boolean result = list.removeDuplicates();
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testRetainAll() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList toRetain = DoubleList.of(2.2, 4.4, 6.6);

        boolean result = list.retainAll(toRetain);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(2.2, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
    }

    @Test
    public void testRetainAllEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList toRetain = new DoubleList();
        boolean result = list.retainAll(toRetain);
        assertTrue(result);
        assertEquals(0, list.size());
    }

    @Test
    public void testRetainAllNoneMatch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList toRetain = DoubleList.of(4.4, 5.5);
        boolean result = list.retainAll(toRetain);
        assertTrue(result);
        assertEquals(0, list.size());
    }

    @Test
    public void testRetainAllArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        double[] toRetain = { 2.2, 4.4, 6.6 };

        boolean result = list.retainAll(toRetain);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(2.2, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
    }

    @Test
    public void testDelete() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });

        double deleted = list.delete(1);
        assertEquals(20.5, deleted, DELTA);
        assertEquals(2, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(30.5, list.get(1), DELTA);
    }

    @Test
    public void testDeleteFirst() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        double deleted = list.delete(0);
        assertEquals(10.5, deleted, DELTA);
        assertEquals(2, list.size());
    }

    @Test
    public void testDeleteLast() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        double deleted = list.delete(2);
        assertEquals(30.5, deleted, DELTA);
        assertEquals(2, list.size());
    }

    @Test
    public void testDeleteThrowsException() {
        list.add(10.5);
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(1));
    }

    @Test
    public void testDeleteAllByIndices() {
        list.addAll(new double[] { 10.5, 20.5, 30.5, 40.5, 50.5 });

        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(30.5, list.get(1), DELTA);
        assertEquals(50.5, list.get(2), DELTA);
    }

    @Test
    public void testDeleteAllByIndicesEmpty() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        list.deleteAllByIndices();
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteAllByIndicesDuplicates() {
        list.addAll(new double[] { 10.5, 20.5, 30.5, 40.5 });
        list.deleteAllByIndices(1, 1, 2);
        assertEquals(2, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(40.5, list.get(1), DELTA);
    }

    @Test
    public void testDeleteRange() {
        list.addAll(new double[] { 10.5, 20.5, 30.5, 40.5, 50.5 });

        list.deleteRange(1, 3);
        assertEquals(3, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(40.5, list.get(1), DELTA);
        assertEquals(50.5, list.get(2), DELTA);
    }

    @Test
    public void testDeleteRangeEmpty() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        list.deleteRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRangeAll() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        list.deleteRange(0, 3);
        assertEquals(0, list.size());
    }

    @Test
    public void testDeleteRangeThrowsException() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(1, 4));
    }

    @Test
    public void testMoveRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.moveRange(1, 3, 0);
        assertEquals(2.2, list.get(0), DELTA);
        assertEquals(3.3, list.get(1), DELTA);
        assertEquals(1.1, list.get(2), DELTA);
    }

    @Test
    public void testReplaceRangeDoubleList() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList replacement = DoubleList.of(10.1, 10.2);
        list.replaceRange(1, 3, replacement);

        assertEquals(5, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(10.1, list.get(1), DELTA);
        assertEquals(10.2, list.get(2), DELTA);
        assertEquals(4.4, list.get(3), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testReplaceRangeDoubleListEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        DoubleList replacement = new DoubleList();
        list.replaceRange(1, 3, replacement);

        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
    }

    @Test
    public void testReplaceRangeArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        double[] replacement = { 10.1, 10.2 };
        list.replaceRange(1, 3, replacement);

        assertEquals(5, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(10.1, list.get(1), DELTA);
        assertEquals(10.2, list.get(2), DELTA);
        assertEquals(4.4, list.get(3), DELTA);
    }

    @Test
    public void testReplaceRangeArrayEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        double[] replacement = {};
        list.replaceRange(1, 3, replacement);

        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
    }

    @Test
    public void testReplaceAllValues() {
        list.addAll(new double[] { 1.1, 2.2, 1.1, 3.3, 1.1 });
        int count = list.replaceAll(1.1, 9.9);

        assertEquals(3, count);
        assertEquals(9.9, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(9.9, list.get(2), DELTA);
        assertEquals(3.3, list.get(3), DELTA);
        assertEquals(9.9, list.get(4), DELTA);
    }

    @Test
    public void testReplaceAllValuesNoneFound() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        int count = list.replaceAll(4.4, 9.9);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllOperator() {
        list.addAll(new double[] { 1.0, 2.0, 3.0, 4.0 });
        list.replaceAll(x -> x * 2.0);

        assertEquals(2.0, list.get(0), DELTA);
        assertEquals(4.0, list.get(1), DELTA);
        assertEquals(6.0, list.get(2), DELTA);
        assertEquals(8.0, list.get(3), DELTA);
    }

    @Test
    public void testReplaceIf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        boolean result = list.replaceIf(x -> x > 3.0, 99.9);

        assertTrue(result);
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(99.9, list.get(2), DELTA);
        assertEquals(99.9, list.get(3), DELTA);
        assertEquals(99.9, list.get(4), DELTA);
    }

    @Test
    public void testReplaceIfNoneMatch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        boolean result = list.replaceIf(x -> x > 10.0, 99.9);
        assertFalse(result);
    }

    @Test
    public void testFill() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        list.fill(9.9);

        assertEquals(4, list.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(9.9, list.get(i), DELTA);
        }
    }

    @Test
    public void testFillRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.fill(1, 4, 9.9);

        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(9.9, list.get(1), DELTA);
        assertEquals(9.9, list.get(2), DELTA);
        assertEquals(9.9, list.get(3), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testFillRangeEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.fill(1, 1, 9.9);
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testContains() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertTrue(list.contains(2.2));
        assertFalse(list.contains(4.4));
    }

    @Test
    public void testContainsEmpty() {
        assertFalse(list.contains(1.1));
    }

    @Test
    public void testContainsSpecialValues() {
        list.add(Double.NaN);
        assertTrue(list.contains(Double.NaN));

        list.add(Double.POSITIVE_INFINITY);
        assertTrue(list.contains(Double.POSITIVE_INFINITY));

        list.add(0.0);
        assertTrue(list.contains(0.0));
    }

    @Test
    public void testContainsAnyDoubleList() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = DoubleList.of(3.3, 4.4, 5.5);
        assertTrue(list.containsAny(other));

        DoubleList noMatch = DoubleList.of(6.6, 7.7);
        assertFalse(list.containsAny(noMatch));
    }

    @Test
    public void testContainsAnyArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double[] other = { 3.3, 4.4, 5.5 };
        assertTrue(list.containsAny(other));

        double[] noMatch = { 6.6, 7.7 };
        assertFalse(list.containsAny(noMatch));
    }

    @Test
    public void testContainsAllDoubleList() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        DoubleList subset = DoubleList.of(2.2, 3.3);
        assertTrue(list.containsAll(subset));

        DoubleList notSubset = DoubleList.of(2.2, 5.5);
        assertFalse(list.containsAll(notSubset));
    }

    @Test
    public void testContainsAllEmptySubset() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList empty = new DoubleList();
        assertTrue(list.containsAll(empty));
    }

    @Test
    public void testContainsAllArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        double[] subset = { 2.2, 3.3 };
        assertTrue(list.containsAll(subset));

        double[] notSubset = { 2.2, 5.5 };
        assertFalse(list.containsAll(notSubset));
    }

    @Test
    public void testDisjointDoubleList() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = DoubleList.of(4.4, 5.5, 6.6);
        assertTrue(list.disjoint(other));

        DoubleList overlapping = DoubleList.of(3.3, 4.4);
        assertFalse(list.disjoint(overlapping));
    }

    @Test
    public void testDisjointArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double[] other = { 4.4, 5.5, 6.6 };
        assertTrue(list.disjoint(other));

        double[] overlapping = { 3.3, 4.4 };
        assertFalse(list.disjoint(overlapping));
    }

    @Test
    public void testDisjointEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList empty = new DoubleList();
        assertTrue(list.disjoint(empty));
    }

    @Test
    public void testIntersection() {
        list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
        DoubleList other = DoubleList.of(1.1, 2.2, 2.2, 4.4);

        DoubleList result = list.intersection(other);
        assertEquals(2, result.size());
        assertEquals(1.1, result.get(0), DELTA);
        assertEquals(2.2, result.get(1), DELTA);
    }

    @Test
    public void testIntersectionEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = new DoubleList();

        DoubleList result = list.intersection(other);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionArray() {
        list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
        double[] other = { 1.1, 2.2, 2.2, 4.4 };

        DoubleList result = list.intersection(other);
        assertEquals(2, result.size());
        assertEquals(1.1, result.get(0), DELTA);
        assertEquals(2.2, result.get(1), DELTA);
    }

    @Test
    public void testDifference() {
        list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
        DoubleList other = DoubleList.of(1.1, 4.4);

        DoubleList result = list.difference(other);
        assertEquals(3, result.size());
        assertEquals(1.1, result.get(0), DELTA);
        assertEquals(2.2, result.get(1), DELTA);
        assertEquals(3.3, result.get(2), DELTA);
    }

    @Test
    public void testDifferenceEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = new DoubleList();

        DoubleList result = list.difference(other);
        assertEquals(3, result.size());
    }

    @Test
    public void testDifferenceArray() {
        list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
        double[] other = { 1.1, 4.4 };

        DoubleList result = list.difference(other);
        assertEquals(3, result.size());
    }

    @Test
    public void testSymmetricDifference() {
        list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
        DoubleList other = DoubleList.of(2.2, 3.3, 3.3, 4.4);

        DoubleList result = list.symmetricDifference(other);
        assertEquals(4, result.size());
        assertEquals(DoubleList.of(1.1, 1.1, 3.3, 4.4), result);
    }

    @Test
    public void testSymmetricDifferenceEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = new DoubleList();

        DoubleList result = list.symmetricDifference(other);
        assertEquals(3, result.size());
    }

    @Test
    public void testSymmetricDifferenceArray() {
        list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
        double[] other = { 2.2, 3.3, 3.3, 4.4 };

        DoubleList result = list.symmetricDifference(other);
        assertEquals(4, result.size());
        assertEquals(DoubleList.of(1.1, 1.1, 3.3, 4.4), result);
    }

    @Test
    public void testIndexOf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 4.4 });
        assertEquals(1, list.indexOf(2.2));
        assertEquals(-1, list.indexOf(5.5));
    }

    @Test
    public void testIndexOfEmpty() {
        assertEquals(-1, list.indexOf(1.1));
    }

    @Test
    public void testIndexOfFromIndex() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 4.4 });
        assertEquals(3, list.indexOf(2.2, 2));
        assertEquals(-1, list.indexOf(2.2, 4));
    }

    @Test
    public void testIndexOfFromIndexNegative() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(1, list.indexOf(2.2, -1));
    }

    @Test
    public void testLastIndexOf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 4.4 });
        assertEquals(3, list.lastIndexOf(2.2));
        assertEquals(-1, list.lastIndexOf(5.5));
    }

    @Test
    public void testLastIndexOfEmpty() {
        assertEquals(-1, list.lastIndexOf(1.1));
    }

    @Test
    public void testLastIndexOfFromBack() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 4.4 });
        assertEquals(1, list.lastIndexOf(2.2, 2));
        assertEquals(-1, list.lastIndexOf(2.2, 0));
    }

    @Test
    public void testOccurrencesOf() {
        list.addAll(new double[] { 1.1, 2.2, 1.1, 3.3, 1.1 });
        assertEquals(3, list.occurrencesOf(1.1));
        assertEquals(1, list.occurrencesOf(2.2));
        assertEquals(0, list.occurrencesOf(4.4));
    }

    @Test
    public void testOccurrencesOfEmpty() {
        assertEquals(0, list.occurrencesOf(1.1));
    }

    @Test
    public void testMin() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 2.2 });
        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1.1, min.get(), DELTA);
    }

    @Test
    public void testMinEmpty() {
        OptionalDouble min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMinRange() {
        list.addAll(new double[] { 5.5, 3.3, 1.1, 4.4, 2.2 });
        OptionalDouble min = list.min(1, 4);
        assertTrue(min.isPresent());
        assertEquals(1.1, min.get(), DELTA);
    }

    @Test
    public void testMinRangeEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        OptionalDouble min = list.min(1, 1);
        assertFalse(min.isPresent());
    }

    @Test
    public void testMax() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 2.2 });
        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(4.4, max.get(), DELTA);
    }

    @Test
    public void testMaxEmpty() {
        OptionalDouble max = list.max();
        assertFalse(max.isPresent());
    }

    @Test
    public void testMaxRange() {
        list.addAll(new double[] { 1.1, 3.3, 5.5, 4.4, 2.2 });
        OptionalDouble max = list.max(1, 4);
        assertTrue(max.isPresent());
        assertEquals(5.5, max.get(), DELTA);
    }

    @Test
    public void testMedian() {
        list.addAll(new double[] { 3.3, 1.1, 5.5, 2.2, 4.4 });
        OptionalDouble median = list.median();
        assertTrue(median.isPresent());
        assertEquals(3.3, median.get(), DELTA);
    }

    @Test
    public void testMedianEmpty() {
        OptionalDouble median = list.median();
        assertFalse(median.isPresent());
    }

    @Test
    public void testMedianRange() {
        list.addAll(new double[] { 1.1, 3.3, 5.5, 2.2, 4.4 });
        OptionalDouble median = list.median(1, 4);
        assertTrue(median.isPresent());
    }

    @Test
    public void testForEach() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        List<Double> collected = new ArrayList<>();
        list.forEach(collected::add);

        assertEquals(3, collected.size());
        assertEquals(1.1, collected.get(0), DELTA);
        assertEquals(2.2, collected.get(1), DELTA);
        assertEquals(3.3, collected.get(2), DELTA);
    }

    @Test
    public void testForEachRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        List<Double> collected = new ArrayList<>();
        list.forEach(1, 4, collected::add);

        assertEquals(3, collected.size());
        assertEquals(2.2, collected.get(0), DELTA);
        assertEquals(3.3, collected.get(1), DELTA);
        assertEquals(4.4, collected.get(2), DELTA);
    }

    @Test
    public void testFirst() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        OptionalDouble first = list.first();
        assertTrue(first.isPresent());
        assertEquals(1.1, first.get(), DELTA);
    }

    @Test
    public void testFirstEmpty() {
        OptionalDouble first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        OptionalDouble last = list.last();
        assertTrue(last.isPresent());
        assertEquals(3.3, last.get(), DELTA);
    }

    @Test
    public void testLastEmpty() {
        OptionalDouble last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testGetFirst() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(1.1, list.getFirst(), DELTA);
    }

    @Test
    public void testGetFirstEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(3.3, list.getLast(), DELTA);
    }

    @Test
    public void testGetLastEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testAddFirst() {
        list.addAll(new double[] { 2.2, 3.3 });
        list.addFirst(1.1);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
    }

    @Test
    public void testAddLast() {
        list.addAll(new double[] { 1.1, 2.2 });
        list.addLast(3.3);
        assertEquals(3, list.size());
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testRemoveFirst() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double removed = list.removeFirst();
        assertEquals(1.1, removed, DELTA);
        assertEquals(2, list.size());
        assertEquals(2.2, list.get(0), DELTA);
    }

    @Test
    public void testRemoveFirstEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double removed = list.removeLast();
        assertEquals(3.3, removed, DELTA);
        assertEquals(2, list.size());
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testRemoveLastEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void testDistinct() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3, 3.3, 3.3, 4.4 });
        DoubleList distinct = list.distinct(0, list.size());

        assertEquals(4, distinct.size());
        assertEquals(1.1, distinct.get(0), DELTA);
        assertEquals(2.2, distinct.get(1), DELTA);
        assertEquals(3.3, distinct.get(2), DELTA);
        assertEquals(4.4, distinct.get(3), DELTA);
    }

    @Test
    public void testHasDuplicates() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3 });
        assertTrue(list.hasDuplicates());

        DoubleList noDupes = DoubleList.of(1.1, 2.2, 3.3);
        assertFalse(noDupes.hasDuplicates());
    }

    @Test
    public void testHasDuplicatesEmpty() {
        assertFalse(list.hasDuplicates());
    }

    @Test
    public void testIsSorted() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        assertTrue(list.isSorted());

        DoubleList unsorted = DoubleList.of(1.1, 3.3, 2.2);
        assertFalse(unsorted.isSorted());
    }

    @Test
    public void testIsSortedEmpty() {
        assertTrue(list.isSorted());
    }

    @Test
    public void testSort() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 2.2 });
        list.sort();

        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(4.4, list.get(3), DELTA);
    }

    @Test
    public void testSortEmpty() {
        list.sort();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testParallelSort() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 2.2 });
        list.parallelSort();

        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(4.4, list.get(3), DELTA);
    }

    @Test
    public void testReverseSort() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 2.2 });
        list.reverseSort();

        assertEquals(4.4, list.get(0), DELTA);
        assertEquals(3.3, list.get(1), DELTA);
        assertEquals(2.2, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
    }

    @Test
    public void testBinarySearch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        assertEquals(2, list.binarySearch(3.3));
        assertTrue(list.binarySearch(2.5) < 0);
    }

    @Test
    public void testBinarySearchRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        assertEquals(2, list.binarySearch(1, 4, 3.3));
    }

    @Test
    public void testReverse() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        list.reverse();

        assertEquals(4.4, list.get(0), DELTA);
        assertEquals(3.3, list.get(1), DELTA);
        assertEquals(2.2, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
    }

    @Test
    public void testReverseEmpty() {
        list.reverse();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testReverseRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.reverse(1, 4);

        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(2.2, list.get(3), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testRotate() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        list.rotate(1);

        assertEquals(4.4, list.get(0), DELTA);
        assertEquals(1.1, list.get(1), DELTA);
        assertEquals(2.2, list.get(2), DELTA);
        assertEquals(3.3, list.get(3), DELTA);
    }

    @Test
    public void testRotateNegative() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        list.rotate(-1);

        assertEquals(2.2, list.get(0), DELTA);
        assertEquals(3.3, list.get(1), DELTA);
        assertEquals(4.4, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
    }

    @Test
    public void testShuffle() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.shuffle();

        assertEquals(5, list.size());
        assertTrue(list.contains(1.1));
        assertTrue(list.contains(5.5));
    }

    @Test
    public void testShuffleWithRandom() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        Random rnd = new Random(42);
        list.shuffle(rnd);

        assertEquals(5, list.size());
        assertTrue(list.contains(1.1));
    }

    @Test
    public void testSwap() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.swap(0, 2);

        assertEquals(3.3, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(1.1, list.get(2), DELTA);
    }

    @Test
    public void testSwapSameIndex() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.swap(1, 1);
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testCopy() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList copy = list.copy();

        assertEquals(3, copy.size());
        assertEquals(1.1, copy.get(0), DELTA);

        list.set(0, 99.9);
        assertEquals(1.1, copy.get(0), DELTA);
    }

    @Test
    public void testCopyRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList copy = list.copy(1, 4);

        assertEquals(3, copy.size());
        assertEquals(2.2, copy.get(0), DELTA);
        assertEquals(3.3, copy.get(1), DELTA);
        assertEquals(4.4, copy.get(2), DELTA);
    }

    @Test
    public void testCopyWithStep() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList copy = list.copy(0, 5, 2);

        assertEquals(3, copy.size());
        assertEquals(1.1, copy.get(0), DELTA);
        assertEquals(3.3, copy.get(1), DELTA);
        assertEquals(5.5, copy.get(2), DELTA);
    }

    @Test
    public void testSplit() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5, 6.6 });
        List<DoubleList> chunks = list.split(0, 6, 2);

        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(1.1, chunks.get(0).get(0), DELTA);
        assertEquals(2.2, chunks.get(0).get(1), DELTA);
    }

    @Test
    public void testSplitUnevenChunks() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        List<DoubleList> chunks = list.split(0, 5, 2);

        assertEquals(3, chunks.size());
        assertEquals(1, chunks.get(2).size());
        assertEquals(5.5, chunks.get(2).get(0), DELTA);
    }

    @Test
    public void testTrimToSize() {
        list = new DoubleList(100);
        list.add(1.1);
        list.add(2.2);
        list.trimToSize();

        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
    }

    @Test
    public void testClear() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.clear();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(list.isEmpty());

        list.add(1.1);
        assertFalse(list.isEmpty());

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());

        list.add(1.1);
        assertEquals(1, list.size());

        list.add(2.2);
        assertEquals(2, list.size());

        list.delete(0);
        assertEquals(1, list.size());
    }

    @Test
    public void testBoxed() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        List<Double> boxed = list.boxed();

        assertEquals(3, boxed.size());
        assertEquals(1.1, boxed.get(0), DELTA);
        assertEquals(2.2, boxed.get(1), DELTA);
        assertEquals(3.3, boxed.get(2), DELTA);
    }

    @Test
    public void testBoxedRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        List<Double> boxed = list.boxed(1, 4);

        assertEquals(3, boxed.size());
        assertEquals(2.2, boxed.get(0), DELTA);
        assertEquals(3.3, boxed.get(1), DELTA);
        assertEquals(4.4, boxed.get(2), DELTA);
    }

    @Test
    public void testToArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double[] array = list.toArray();

        assertEquals(3, array.length);
        assertEquals(1.1, array[0], DELTA);
        assertEquals(2.2, array[1], DELTA);
        assertEquals(3.3, array[2], DELTA);
    }

    @Test
    public void testToArrayEmpty() {
        double[] array = list.toArray();
        assertEquals(0, array.length);
    }

    @Test
    public void testToCollection() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        List<Double> collection = list.toCollection(0, 3, ArrayList::new);

        assertEquals(3, collection.size());
        assertEquals(1.1, collection.get(0), DELTA);
    }

    @Test
    public void testToMultiset() {
        list.addAll(new double[] { 1.1, 2.2, 1.1, 3.3 });
        Multiset<Double> multiset = list.toMultiset(0, 4, Multiset::new);

        assertEquals(2, multiset.count(1.1));
        assertEquals(1, multiset.count(2.2));
    }

    @Test
    public void testIterator() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleIterator iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(1.1, iterator.nextDouble(), DELTA);
        assertTrue(iterator.hasNext());
        assertEquals(2.2, iterator.nextDouble(), DELTA);
        assertTrue(iterator.hasNext());
        assertEquals(3.3, iterator.nextDouble(), DELTA);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testStream() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleStream stream = list.stream();

        assertNotNull(stream);
        assertEquals(3, stream.count());
    }

    @Test
    public void testStreamRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleStream stream = list.stream(1, 4);

        assertNotNull(stream);
        assertEquals(3, stream.count());
    }

    @Test
    public void testHashCode() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList list2 = DoubleList.of(1.1, 2.2, 3.3);

        assertEquals(list.hashCode(), list2.hashCode());
    }

    @Test
    public void testHashCodeEmpty() {
        DoubleList list2 = new DoubleList();
        assertEquals(list.hashCode(), list2.hashCode());
    }

    @Test
    public void testEquals() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList list2 = DoubleList.of(1.1, 2.2, 3.3);

        assertTrue(list.equals(list2));
        assertTrue(list2.equals(list));
    }

    @Test
    public void testEqualsSameObject() {
        assertTrue(list.equals(list));
    }

    @Test
    public void testEqualsNull() {
        assertFalse(list.equals(null));
    }

    @Test
    public void testEqualsDifferentType() {
        list.add(1.1);
        assertFalse(list.equals("not a DoubleList"));
    }

    @Test
    public void testEqualsDifferentSize() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList list2 = DoubleList.of(1.1, 2.2);

        assertFalse(list.equals(list2));
    }

    @Test
    public void testEqualsDifferentContent() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList list2 = DoubleList.of(1.1, 2.2, 4.4);

        assertFalse(list.equals(list2));
    }

    @Test
    public void testToString() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        String str = list.toString();

        assertNotNull(str);
        assertTrue(str.contains("1.1"));
        assertTrue(str.contains("2.2"));
        assertTrue(str.contains("3.3"));
    }

    @Test
    public void testToStringEmpty() {
        String str = list.toString();
        assertNotNull(str);
    }

    @Test
    public void testSpecialValuesNaN() {
        list.add(Double.NaN);
        list.add(1.1);
        list.add(Double.NaN);

        assertEquals(3, list.size());
        assertTrue(Double.isNaN(list.get(0)));
        assertTrue(Double.isNaN(list.get(2)));
    }

    @Test
    public void testSpecialValuesInfinity() {
        list.add(Double.POSITIVE_INFINITY);
        list.add(1.1);
        list.add(Double.NEGATIVE_INFINITY);

        assertEquals(3, list.size());
        assertEquals(Double.POSITIVE_INFINITY, list.get(0), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, list.get(2), DELTA);
    }

    @Test
    public void testSpecialValuesZero() {
        list.add(0.0);
        list.add(-0.0);

        assertEquals(2, list.size());
        assertEquals(0.0, list.get(0), DELTA);
        assertEquals(-0.0, list.get(1), DELTA);
    }

    @Test
    public void testLargeList() {
        for (int i = 0; i < 10000; i++) {
            list.add(i * 1.1);
        }

        assertEquals(10000, list.size());
        assertEquals(0.0, list.get(0), DELTA);
        assertEquals(9999 * 1.1, list.get(9999), DELTA);
    }

    @Test
    public void testArrayMethod() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double[] array = list.array();
        assertNotNull(array);
        assertTrue(array.length >= 3);
    }
}
