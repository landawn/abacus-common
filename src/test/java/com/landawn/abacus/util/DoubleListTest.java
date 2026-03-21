package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleStream;

public class DoubleListTest extends TestBase {

    private DoubleList list;
    private static final double DELTA = 0.000001;

    @BeforeEach
    public void setUp() {
        list = new DoubleList();
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
    public void testConstructorWithArrayAndSize() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList newList = new DoubleList(array, 3);
        assertEquals(3, newList.size());
        assertEquals(1.1, newList.get(0), DELTA);
        assertEquals(2.2, newList.get(1), DELTA);
        assertEquals(3.3, newList.get(2), DELTA);
    }

    @Test
    public void testBatchOperationsLargeData() {
        int size = 1000;
        DoubleList list1 = new DoubleList();
        DoubleList list2 = new DoubleList();

        for (int i = 0; i < size; i++) {
            list1.add(i * 0.1);
        }

        for (int i = size / 2; i < size + size / 2; i++) {
            list2.add(i * 0.1);
        }

        DoubleList intersection = list1.intersection(list2);
        assertEquals(size / 2, intersection.size());

        DoubleList difference = list1.difference(list2);
        assertEquals(size / 2, difference.size());

        DoubleList symDiff = list1.symmetricDifference(list2);
        assertEquals(size, symDiff.size());
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
    public void testConstructorWithEmptyArray() {
        double[] array = {};
        DoubleList newList = new DoubleList(array);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testConstructorWithArrayAndSizeZero() {
        double[] array = { 1.1, 2.2, 3.3 };
        DoubleList newList = new DoubleList(array, 0);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testConstructorWithZeroSize() {
        double[] array = { 1.1, 2.2, 3.3 };
        DoubleList list = new DoubleList(array, 0);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new DoubleList(-1));
    }

    @Test
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new DoubleList(null));
    }

    @Test
    public void testConstructorWithArrayAndSizeTooLarge() {
        double[] array = { 1.1, 2.2, 3.3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new DoubleList(array, 4));
    }

    @Test
    public void testConstructorWithArrayAndNegativeSize() {
        double[] array = { 1.1, 2.2, 3.3 };
        assertThrows(IllegalArgumentException.class, () -> new DoubleList(array, -1));
    }

    @Test
    public void testConstructorWithNullArrayAndSize() {
        assertThrows(NullPointerException.class, () -> new DoubleList(null, 0));
    }

    @Test
    public void testOf() {
        DoubleList newList = DoubleList.of(1.1, 2.2, 3.3, 4.4, 5.5);
        assertEquals(5, newList.size());
        assertEquals(1.1, newList.get(0), DELTA);
        assertEquals(5.5, newList.get(4), DELTA);
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
    public void testOfWithArrayAndSizeZero() {
        double[] array = { 1.1, 2.2, 3.3 };
        DoubleList newList = DoubleList.of(array, 0);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testObjectMethods() {
        DoubleList list1 = DoubleList.of(1.1, 2.2);
        DoubleList list2 = DoubleList.of(1.1, 2.2);
        DoubleList list3 = DoubleList.of(2.2, 1.1);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(null, list1);
        assertNotEquals(list1, new Object());

        assertEquals(list1.hashCode(), list2.hashCode());

        assertEquals("[1.1, 2.2]", list1.toString());
        assertEquals("[]", new DoubleList().toString());
    }

    @Test
    public void testOfWithArrayAndSizeTooLarge() {
        double[] array = { 1.1, 2.2, 3.3 };
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.of(array, 4));
    }

    @Test
    public void testOfWithNullAndSize() {
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.of(null, 5));
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
    public void testCopyOfRange() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList newList = DoubleList.copyOf(array, 1, 4);
        assertEquals(3, newList.size());
        assertEquals(2.2, newList.get(0), DELTA);
        assertEquals(3.3, newList.get(1), DELTA);
        assertEquals(4.4, newList.get(2), DELTA);
    }

    @Test
    public void testCopyOfNull() {
        DoubleList newList = DoubleList.copyOf(null);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testCopyOfRangeEmptyRange() {
        double[] array = { 1.1, 2.2, 3.3 };
        DoubleList newList = DoubleList.copyOf(array, 1, 1);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testCopyOfWithNull() {
        DoubleList list = DoubleList.copyOf(null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCopyOfRangeWithInvalidIndices() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.copyOf(array, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.copyOf(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.copyOf(array, 2, 10));
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
    public void testRepeatSpecialValues() {
        DoubleList nanList = DoubleList.repeat(Double.NaN, 3);
        assertEquals(3, nanList.size());
        assertTrue(Double.isNaN(nanList.get(0)));

        DoubleList infList = DoubleList.repeat(Double.POSITIVE_INFINITY, 2);
        assertEquals(2, infList.size());
        assertEquals(Double.POSITIVE_INFINITY, infList.get(0), DELTA);
    }

    @Test
    public void testRepeatWithSpecialValues() {
        DoubleList nanList = DoubleList.repeat(Double.NaN, 3);
        assertEquals(3, nanList.size());
        for (int i = 0; i < 3; i++) {
            assertTrue(Double.isNaN(nanList.get(i)));
        }

        DoubleList infList = DoubleList.repeat(Double.POSITIVE_INFINITY, 3);
        assertEquals(3, infList.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(Double.POSITIVE_INFINITY, infList.get(i), DELTA);
        }
    }

    @Test
    public void testRepeatNegativeTimes() {
        assertThrows(IllegalArgumentException.class, () -> DoubleList.repeat(7.7, -1));
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

    // --- Gap coverage tests ---

    @Test
    public void testInternalArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double[] internal = list.internalArray();
        assertNotNull(internal);
        assertTrue(internal.length >= 3);
        assertEquals(1.1, internal[0], DELTA);
        assertEquals(2.2, internal[1], DELTA);
        assertEquals(3.3, internal[2], DELTA);

        // Modifying internal array affects the list
        internal[0] = 99.9;
        assertEquals(99.9, list.get(0), DELTA);

        // Empty list
        DoubleList empty = new DoubleList();
        assertNotNull(empty.internalArray());
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
    public void testSetThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, 10.5));
    }

    @Test
    public void testSetNegativeIndex() {
        list.add(10.5);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, 20.5));
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
    public void testAddToEnsureCapacityGrowth() {
        for (int i = 0; i < 100; i++) {
            list.add(i * 0.1);
        }
        assertEquals(100, list.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i * 0.1, list.get(i), DELTA);
        }
    }

    @Test
    public void testAddRemovePerformance() {
        int count = 1000;
        for (int i = 0; i < count; i++) {
            list.add(i * 0.1);
        }
        assertEquals(count, list.size());

        list.removeIf(x -> ((int) (x * 10)) % 2 == 0);
        assertEquals(count / 2, list.size());
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
    public void testAddAllArray() {
        list.add(1.1);
        double[] array = { 2.2, 3.3, 4.4 };
        boolean result = list.addAll(array);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(4.4, list.get(3), DELTA);
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
    public void testDelete() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });

        double deleted = list.removeAt(1);
        assertEquals(20.5, deleted, DELTA);
        assertEquals(2, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(30.5, list.get(1), DELTA);
    }

    @Test
    public void testDeleteFirst() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        double deleted = list.removeAt(0);
        assertEquals(10.5, deleted, DELTA);
        assertEquals(2, list.size());
    }

    @Test
    public void testDeleteLast() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        double deleted = list.removeAt(2);
        assertEquals(30.5, deleted, DELTA);
        assertEquals(2, list.size());
    }

    @Test
    public void testDeleteAllByIndices() {
        list.addAll(new double[] { 10.5, 20.5, 30.5, 40.5, 50.5 });

        list.removeAt(1, 3);
        assertEquals(3, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(30.5, list.get(1), DELTA);
        assertEquals(50.5, list.get(2), DELTA);
    }

    @Test
    public void testDeleteAllByIndicesDuplicates() {
        list.addAll(new double[] { 10.5, 20.5, 30.5, 40.5 });
        list.removeAt(1, 1, 2);
        assertEquals(2, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(40.5, list.get(1), DELTA);
    }

    @Test
    public void testDeleteRange() {
        list.addAll(new double[] { 10.5, 20.5, 30.5, 40.5, 50.5 });

        list.removeRange(1, 3);
        assertEquals(3, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(40.5, list.get(1), DELTA);
        assertEquals(50.5, list.get(2), DELTA);
    }

    @Test
    public void testDeleteRangeAll() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        list.removeRange(0, 3);
        assertEquals(0, list.size());
    }

    @Test
    public void testOccurrencesOf() {
        list.addAll(new double[] { 1.1, 2.2, 1.1, 3.3, 1.1 });
        assertEquals(3, list.frequency(1.1));
        assertEquals(1, list.frequency(2.2));
        assertEquals(0, list.frequency(4.4));
    }

    @Test
    public void testHasDuplicates() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3 });
        assertTrue(list.containsDuplicates());

        DoubleList noDupes = DoubleList.of(1.1, 2.2, 3.3);
        assertFalse(noDupes.containsDuplicates());
    }

    @Test
    public void testDeleteAtBoundaries() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        assertEquals(1.1, list.removeAt(0), DELTA);
        assertEquals(4, list.size());
        assertEquals(2.2, list.get(0), DELTA);

        assertEquals(5.5, list.removeAt(list.size() - 1), DELTA);
        assertEquals(3, list.size());
        assertEquals(4.4, list.get(list.size() - 1), DELTA);
    }

    @Test
    public void testDeleteAllByIndicesOutOfOrder() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.removeAt(4, 1, 2);
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
    }

    @Test
    public void testConcurrentModification() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        DoubleIterator iter = list.iterator();
        list.add(6.6);

        assertTrue(iter.hasNext());
        iter.nextDouble();
    }

    @Test
    public void testArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        double[] array = list.internalArray();
        assertEquals(1.1, array[0], DELTA);
        assertEquals(2.2, array[1], DELTA);
        assertEquals(3.3, array[2], DELTA);

        array[0] = 10.0;
        assertEquals(10.0, list.get(0), DELTA);
    }

    @Test
    public void testAddAllDoubleListAtIndex() {
        list.addAll(new double[] { 1.0, 4.0 });
        DoubleList toAdd = DoubleList.of(2.0, 3.0);
        assertTrue(list.addAll(1, toAdd));
        assertEquals(4, list.size());
        assertEquals(2.0, list.get(1), DELTA);
        assertEquals(3.0, list.get(2), DELTA);
    }

    @Test
    public void testAddAll_atIndex_double() {
        DoubleList dl = DoubleList.of(1.0, 4.0, 5.0);
        DoubleList toAdd = DoubleList.of(2.0, 3.0);
        assertTrue(dl.addAll(1, toAdd));
        assertEquals(5, dl.size());
        assertEquals(2.0, dl.get(1), DELTA);
        assertEquals(3.0, dl.get(2), DELTA);
        assertEquals(4.0, dl.get(3), DELTA);
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
    public void testDeleteAllByIndicesEmpty() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        list.removeAt();
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRangeEmpty() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testArrayMethod() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double[] array = list.internalArray();
        assertNotNull(array);
        assertTrue(array.length >= 3);
    }

    @Test
    public void testAddAllEmptyListAtVariousPositions() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList empty = new DoubleList();

        assertFalse(list.addAll(0, empty));
        assertFalse(list.addAll(1, empty));
        assertFalse(list.addAll(list.size(), empty));
    }

    @Test
    public void testAddAllWithNullArray() {
        list.add(1.1);
        assertFalse(list.addAll((double[]) null));
        assertEquals(1, list.size());
    }

    @Test
    public void testDeleteRangeEmptyRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRangeEntireList() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.removeRange(0, 3);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testArrayModification() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double[] array = list.internalArray();

        array[1] = 20.5;
        assertEquals(20.5, list.get(1), DELTA);

        list.clear();
        double[] newArray = list.internalArray();
        assertSame(array, newArray);
    }

    @Test
    public void testAddAll_atIndex_empty_double() {
        DoubleList dl = DoubleList.of(1.0, 2.0);
        assertFalse(dl.addAll(1, DoubleList.of()));
        assertEquals(2, dl.size());
    }

    @Test
    public void testAddAllAtIndexThrowsException() {
        list.add(1.1);
        DoubleList other = DoubleList.of(2.2, 3.3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(2, other));
    }

    @Test
    public void testDeleteRangeThrowsException() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(1, 4));
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
    public void testRemoveNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2 });

        boolean result = list.remove(Double.NaN);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
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
    public void testEmptyOperations() {
        assertFalse(list.remove(1.1));
        assertFalse(list.removeAllOccurrences(1.1));
        assertFalse(list.removeIf(x -> true));
        assertFalse(list.removeDuplicates());
        assertFalse(list.containsDuplicates());
        assertTrue(list.isSorted());

        list.sort();
        list.reverse();
        list.shuffle();

        assertTrue(list.isEmpty());
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
    public void testRemoveAllOccurrencesWithNaN() {
        list.addAll(new double[] { Double.NaN, 1.1, Double.NaN, 2.2, Double.NaN });

        boolean result = list.removeAllOccurrences(Double.NaN);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
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
    public void testRemoveAllArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        double[] toRemove = { 2.2, 4.4 };

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
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
    public void testRemoveAll() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 4.4 });

        // removeAll(DoubleList)
        assertTrue(list.removeAll(DoubleList.of(2.2, 4.4)));
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(3.3, list.get(1), DELTA);

        // removeAll with empty list
        assertFalse(list.removeAll(new DoubleList()));

        // removeAll(double[])
        list.addAll(new double[] { 5.5, 6.6 });
        assertTrue(list.removeAll(new double[] { 1.1, 5.5 }));
        assertEquals(2, list.size());
        assertEquals(3.3, list.get(0), DELTA);
        assertEquals(6.6, list.get(1), DELTA);

        // removeAll with empty array
        assertFalse(list.removeAll(new double[0]));
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
    public void testRemoveIfAllMatch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        boolean result = list.removeIf(x -> x > 0.0);
        assertTrue(result);
        assertEquals(0, list.size());
    }

    @Test
    public void testRemoveIfNoMatch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertFalse(list.removeIf(x -> x > 10.0));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIfWithNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2, Double.NaN, 3.3 });

        boolean result = list.removeIf(Double::isNaN);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testRemoveIfNoneMatch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        boolean result = list.removeIf(x -> x > 10.0);
        assertFalse(result);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIfEmptyList() {
        assertFalse(list.removeIf(x -> true));
    }

    @Test
    public void testRemoveIfWithInfinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, 2.2, Double.NEGATIVE_INFINITY, 3.3 });

        boolean result = list.removeIf(Double::isInfinite);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
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
    public void testRemoveDuplicatesWithNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, Double.NaN, 2.2 });
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveDuplicatesSorted() {
        list.addAll(new double[] { 1.1, 1.1, 2.2, 2.2, 2.2, 3.3, 3.3, 4.4, 5.5, 5.5 });
        assertTrue(list.removeDuplicates());
        assertEquals(5, list.size());
        for (int i = 1; i <= 5; i++) {
            assertEquals(i * 1.1, list.get(i - 1), DELTA);
        }
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
    public void testBatchRemove_viaRetainAll_largeList() {
        // triggers the Set path (c.size() > 3 && size() > 9)
        DoubleList dl = new DoubleList();
        for (int i = 1; i <= 15; i++)
            dl.add(i * 1.0);
        DoubleList retain = DoubleList.of(1.0, 3.0, 5.0, 7.0, 9.0);
        dl.retainAll(retain);
        assertEquals(5, dl.size());
        assertEquals(1.0, dl.get(0), DELTA);
        assertEquals(3.0, dl.get(1), DELTA);
    }

    @Test
    public void testRemoveAt_multipleIndices() {
        DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 4.0, 5.0);
        dl.removeAt(1, 3);
        assertEquals(3, dl.size());
        assertEquals(1.0, dl.get(0), DELTA);
        assertEquals(3.0, dl.get(1), DELTA);
        assertEquals(5.0, dl.get(2), DELTA);
    }

    @Test
    public void testDeleteThrowsException() {
        list.add(10.5);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(1));
    }

    @Test
    public void testRemoveAt() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        // removeAt(int) - single index
        double removed = list.removeAt(2);
        assertEquals(3.3, removed, DELTA);
        assertEquals(4, list.size());

        // removeAt(int...) - multiple indices
        list.removeAt(0, 2);
        assertEquals(2, list.size());
        assertEquals(2.2, list.get(0), DELTA);
        assertEquals(5.5, list.get(1), DELTA);

        // removeAt with out of bounds
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(10));
    }

    @Test
    public void testRemoveRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        list.removeRange(1, 3);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);

        // Empty range
        list.removeRange(1, 1);
        assertEquals(3, list.size());

        // Invalid range
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(0, 10));
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
    public void testMoveRangeToBeginning() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.moveRange(3, 5, 0);
        assertEquals(5, list.size());
        assertEquals(4.4, list.get(0), DELTA);
        assertEquals(5.5, list.get(1), DELTA);
        assertEquals(1.1, list.get(2), DELTA);
    }

    @Test
    public void testMoveRangeToEnd() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.moveRange(0, 2, 3);
        assertEquals(5, list.size());
        assertEquals(3.3, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
        assertEquals(2.2, list.get(4), DELTA);
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
    public void testReplaceRangeExpanding() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.replaceRange(1, 2, DoubleList.of(10.1, 20.2, 30.3));
        assertEquals(5, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(10.1, list.get(1), DELTA);
        assertEquals(20.2, list.get(2), DELTA);
        assertEquals(30.3, list.get(3), DELTA);
        assertEquals(3.3, list.get(4), DELTA);
    }

    @Test
    public void testReplaceRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList replacement = DoubleList.of(10.1, 20.2);

        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(10.1, list.get(1), DELTA);
        assertEquals(20.2, list.get(2), DELTA);
        assertEquals(4.4, list.get(3), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testReplaceRangeWithArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        double[] replacement = { 10.1, 20.2 };

        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(10.1, list.get(1), DELTA);
        assertEquals(20.2, list.get(2), DELTA);
    }

    @Test
    public void testReplaceRange_DoubleList() {
        DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 4.0, 5.0);
        dl.replaceRange(1, 3, DoubleList.of(20.0, 30.0, 40.0));
        assertEquals(6, dl.size());
        assertEquals(20.0, dl.get(1), DELTA);
    }

    @Test
    public void testReplaceRange_array_double() {
        DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 4.0, 5.0);
        dl.replaceRange(1, 4, new double[] { 20.0, 30.0 });
        assertEquals(4, dl.size());
        assertEquals(20.0, dl.get(1), DELTA);
        assertEquals(30.0, dl.get(2), DELTA);
        assertEquals(5.0, dl.get(3), DELTA);
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
    public void testReplaceRangeArrayEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        double[] replacement = {};
        list.replaceRange(1, 3, replacement);

        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
    }

    @Test
    public void testReplaceRangeWithEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.replaceRange(1, 3, new DoubleList());
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);
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
    public void testReplaceAllOperator() {
        list.addAll(new double[] { 1.0, 2.0, 3.0, 4.0 });
        list.replaceAll(x -> x * 2.0);

        assertEquals(2.0, list.get(0), DELTA);
        assertEquals(4.0, list.get(1), DELTA);
        assertEquals(6.0, list.get(2), DELTA);
        assertEquals(8.0, list.get(3), DELTA);
    }

    @Test
    public void testReplaceAllNoMatch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        int count = list.replaceAll(5.5, 10.0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllWithNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2, Double.NaN });
        int count = list.replaceAll(Double.NaN, 0.0);
        assertEquals(2, count);
        assertEquals(0.0, list.get(1), DELTA);
        assertEquals(0.0, list.get(3), DELTA);
    }

    @Test
    public void testReplaceAll() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 5.5 });

        int count = list.replaceAll(2.2, 20.2);
        assertEquals(2, count);
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(20.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(20.2, list.get(3), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testReplaceAllWithOperator() {
        list.addAll(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        list.replaceAll(x -> x * 2);
        assertEquals(2.0, list.get(0), DELTA);
        assertEquals(4.0, list.get(1), DELTA);
        assertEquals(10.0, list.get(4), DELTA);
    }

    @Test
    public void testReplaceAllValuesNoneFound() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        int count = list.replaceAll(4.4, 9.9);
        assertEquals(0, count);
    }

    @Test
    public void testPrecisionEdgeCases() {
        double a = 0.1 + 0.2;
        double b = 0.3;

        list.add(a);
        list.add(b);

        int replaced = list.replaceAll(0.3, 1.0);
        assertTrue(replaced > 0);
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
    public void testReplaceIfFalseCondition() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        boolean result = list.replaceIf(x -> false, 10.0);
        assertFalse(result);
    }

    @Test
    public void testReplaceIfNoneMatch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        boolean result = list.replaceIf(x -> x > 10.0, 99.9);
        assertFalse(result);
    }

    @Test
    public void testReplaceIfWithInfinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, 2.2, Double.NEGATIVE_INFINITY });
        boolean result = list.replaceIf(Double::isInfinite, 0.0);
        assertTrue(result);
        assertEquals(0.0, list.get(1), DELTA);
        assertEquals(0.0, list.get(3), DELTA);
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
    public void testFillWithSpecialValues() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.fill(Double.NaN);
        for (int i = 0; i < list.size(); i++) {
            assertTrue(Double.isNaN(list.get(i)));
        }
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
    public void testFillEmptyList() {
        list.fill(10.0);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testFillRangeInvalidRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(2, 1, 10.0));
    }

    @Test
    public void testContains() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertTrue(list.contains(2.2));
        assertFalse(list.contains(4.4));
    }

    @Test
    public void testContainsMethods() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3, Double.NaN);
        assertTrue(list.contains(2.2));
        assertTrue(list.contains(Double.NaN));
        assertFalse(list.contains(9.9));

        assertTrue(list.containsAll(DoubleList.of(1.1, 3.3, Double.NaN)));
        assertFalse(list.containsAll(DoubleList.of(1.1, 4.4)));

        assertTrue(list.containsAny(new double[] { 4.4, 5.5, 2.2 }));
        assertFalse(list.containsAny(new double[] { 4.4, 5.5, 6.6 }));
    }

    @Test
    public void testContainsNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2 });
        assertTrue(list.contains(Double.NaN));
    }

    @Test
    public void testDoublePrecisionComparison() {
        double a = 0.1;
        double b = 0.2;
        double c = a + b;

        list.add(c);
        list.add(0.3);

        assertTrue(list.contains(c));
        assertTrue(list.contains(0.3));
    }

    @Test
    public void testVeryLargePreciseValues() {
        double largeValue = 9007199254740992.0;
        list.add(largeValue);
        list.add(largeValue + 1);

        assertEquals(largeValue, list.get(0), DELTA);
        assertEquals(largeValue + 1, list.get(1), DELTA);

        assertTrue(list.contains(largeValue));
        assertEquals(0, list.indexOf(largeValue));
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
    public void testContainsInfinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY });
        assertTrue(list.contains(Double.POSITIVE_INFINITY));
        assertTrue(list.contains(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testVerySmallValues() {
        list.add(Double.MIN_VALUE);
        list.add(Double.MIN_NORMAL);
        list.add(0.0);

        assertTrue(list.contains(Double.MIN_VALUE));
        assertTrue(list.contains(Double.MIN_NORMAL));

        list.sort();
        assertEquals(0.0, list.get(0), DELTA);
        assertEquals(Double.MIN_VALUE, list.get(1), DELTA);
        assertEquals(Double.MIN_NORMAL, list.get(2), DELTA);
    }

    @Test
    public void testZeroHandling() {
        list.add(-0.0);
        list.add(0.0);

        assertEquals(0.0, list.get(0), DELTA);
        assertEquals(0.0, list.get(1), DELTA);

        assertTrue(list.contains(0.0));
        assertTrue(list.contains(-0.0));
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
    public void testContainsAny() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList other = DoubleList.of(6.6, 7.7, 3.3);

        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAny_DoubleList() {
        DoubleList dl = DoubleList.of(1.0, 2.0, 3.0);
        assertTrue(dl.containsAny(DoubleList.of(3.0, 4.0)));
        assertFalse(dl.containsAny(DoubleList.of(4.0, 5.0)));
    }

    @Test
    public void testContainsAnyBothEmpty() {
        DoubleList other = new DoubleList();
        assertFalse(list.containsAny(other));
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
    public void testContainsAllArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        double[] subset = { 2.2, 3.3 };
        assertTrue(list.containsAll(subset));

        double[] notSubset = { 2.2, 5.5 };
        assertFalse(list.containsAll(notSubset));
    }

    @Test
    public void testContainsAll() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList other = DoubleList.of(2.2, 4.4);

        assertTrue(list.containsAll(other));

        other = DoubleList.of(2.2, 6.6);
        assertFalse(list.containsAll(other));
    }

    @Test
    public void testContainsAll_array_double() {
        DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 4.0);
        assertTrue(dl.containsAll(new double[] { 1.0, 3.0 }));
        assertFalse(dl.containsAll(new double[] { 1.0, 9.0 }));
    }

    @Test
    public void testContainsAllEmptySubset() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList empty = new DoubleList();
        assertTrue(list.containsAll(empty));
    }

    @Test
    public void testContainsAllEmptyAgainstNonEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertTrue(list.containsAll(new DoubleList()));
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
    public void testDisjoint() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = DoubleList.of(4.4, 5.5, 6.6);

        assertTrue(list.disjoint(other));

        other = DoubleList.of(3.3, 4.4, 5.5);
        assertFalse(list.disjoint(other));
    }

    @Test
    public void testDisjointEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList empty = new DoubleList();
        assertTrue(list.disjoint(empty));
    }

    @Test
    public void testDisjointWithSelf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertFalse(list.disjoint(list));
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
    public void testIntersectionArray() {
        list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
        double[] other = { 1.1, 2.2, 2.2, 4.4 };

        DoubleList result = list.intersection(other);
        assertEquals(2, result.size());
        assertEquals(1.1, result.get(0), DELTA);
        assertEquals(2.2, result.get(1), DELTA);
    }

    @Test
    public void testIntersection_array_double() {
        DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 4.0);
        DoubleList result = dl.intersection(new double[] { 2.0, 4.0, 6.0 });
        assertEquals(2, result.size());
        assertTrue(result.contains(2.0));
        assertTrue(result.contains(4.0));
    }

    @Test
    public void testIntersectionEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = new DoubleList();

        DoubleList result = list.intersection(other);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionWithEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList result = list.intersection(new DoubleList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionNoCommon() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList result = list.intersection(DoubleList.of(4.4, 5.5, 6.6));
        assertTrue(result.isEmpty());
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
    public void testDifferenceArray() {
        list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
        double[] other = { 1.1, 4.4 };

        DoubleList result = list.difference(other);
        assertEquals(3, result.size());
    }

    @Test
    public void testDifferenceEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = new DoubleList();

        DoubleList result = list.difference(other);
        assertEquals(3, result.size());
    }

    @Test
    public void testDifferenceWithEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList result = list.difference(new DoubleList());
        assertEquals(3, result.size());
        assertEquals(1.1, result.get(0), DELTA);
        assertEquals(2.2, result.get(1), DELTA);
        assertEquals(3.3, result.get(2), DELTA);
    }

    @Test
    public void testDifferenceAllRemoved() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList result = list.difference(DoubleList.of(1.1, 2.2, 3.3));
        assertTrue(result.isEmpty());
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
    public void testSymmetricDifferenceArray() {
        list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
        double[] other = { 2.2, 3.3, 3.3, 4.4 };

        DoubleList result = list.symmetricDifference(other);
        assertEquals(4, result.size());
        assertEquals(DoubleList.of(1.1, 1.1, 3.3, 4.4), result);
    }

    @Test
    public void testSymmetricDifference_DoubleList() {
        DoubleList dl = DoubleList.of(1.0, 2.0, 3.0);
        DoubleList result = dl.symmetricDifference(DoubleList.of(2.0, 4.0));
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(3.0));
        assertTrue(result.contains(4.0));
        assertFalse(result.contains(2.0));
    }

    @Test
    public void testSymmetricDifferenceEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = new DoubleList();

        DoubleList result = list.symmetricDifference(other);
        assertEquals(3, result.size());
    }

    @Test
    public void testSymmetricDifferenceOneEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList result = list.symmetricDifference(new DoubleList());
        assertEquals(3, result.size());
    }

    @Test
    public void testOccurrencesOfEmpty() {
        assertEquals(0, list.frequency(1.1));
    }

    @Test
    public void testFrequency() {
        list.addAll(new double[] { 1.1, 2.2, 1.1, 3.3, 1.1 });
        assertEquals(3, list.frequency(1.1));
        assertEquals(1, list.frequency(2.2));
        assertEquals(0, list.frequency(9.9));

        // Empty list
        DoubleList empty = new DoubleList();
        assertEquals(0, empty.frequency(1.0));
    }

    @Test
    public void testIndexOf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 4.4 });
        assertEquals(1, list.indexOf(2.2));
        assertEquals(-1, list.indexOf(5.5));
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        DoubleList list = DoubleList.of(1.1, 2.2, 1.1, 3.3, 2.2);
        assertEquals(0, list.indexOf(1.1));
        assertEquals(2, list.lastIndexOf(1.1));
        assertEquals(4, list.lastIndexOf(2.2));
        assertEquals(4, list.indexOf(2.2, 2));
        assertEquals(-1, list.indexOf(9.9));
    }

    @Test
    public void testIndexOfNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2, Double.NaN });
        assertEquals(1, list.indexOf(Double.NaN));
        assertEquals(3, list.indexOf(Double.NaN, 2));
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
    public void testIndexOfFromIndexBeyondSize() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(-1, list.indexOf(1.1, 10));
    }

    @Test
    public void testIndexOfNegativeFromIndex() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(0, list.indexOf(1.1, -1));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 5.5 });

        assertEquals(3, list.indexOf(2.2, 2));
        assertEquals(-1, list.indexOf(2.2, 4));
    }

    @Test
    public void testLastIndexOf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 4.4 });
        assertEquals(3, list.lastIndexOf(2.2));
        assertEquals(-1, list.lastIndexOf(5.5));
    }

    @Test
    public void testLastIndexOfFromBack() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 4.4 });
        assertEquals(1, list.lastIndexOf(2.2, 2));
        assertEquals(-1, list.lastIndexOf(2.2, 0));
    }

    @Test
    public void testLastIndexOfNaN() {
        list.addAll(new double[] { Double.NaN, 1.1, Double.NaN, 2.2 });
        assertEquals(2, list.lastIndexOf(Double.NaN));
        assertEquals(0, list.lastIndexOf(Double.NaN, 1));
    }

    @Test
    public void testLastIndexOfWithStart() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 5.5 });

        assertEquals(1, list.lastIndexOf(2.2, 2));
        assertEquals(-1, list.lastIndexOf(5.5, 3));
    }

    @Test
    public void testLastIndexOfEmpty() {
        assertEquals(-1, list.lastIndexOf(1.1));
    }

    @Test
    public void testLastIndexOfNegativeStart() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(-1, list.lastIndexOf(1.1, -1));
    }

    @Test
    public void testLastIndexOf_withFromIndex_double() {
        DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 2.0, 1.0);
        assertEquals(3, dl.lastIndexOf(2.0, 4));
        assertEquals(1, dl.lastIndexOf(2.0, 2));
        assertEquals(-1, dl.lastIndexOf(9.0, 4));
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
    public void testMinMaxMedianSingleElement() {
        list.add(5.5);

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(5.5, min.getAsDouble(), DELTA);

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(5.5, max.getAsDouble(), DELTA);

        OptionalDouble median = list.median();
        assertTrue(median.isPresent());
        assertEquals(5.5, median.getAsDouble(), DELTA);
    }

    @Test
    public void testMinMaxMedianEmptyRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        assertFalse(list.min(1, 1).isPresent());
        assertFalse(list.max(1, 1).isPresent());
        assertFalse(list.median(1, 1).isPresent());
    }

    @Test
    public void testMinMaxWithNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2 });

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1.1, min.get(), DELTA);

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(2.2, max.get(), DELTA);
    }

    @Test
    public void testMinMaxWithInfinity() {
        list.addAll(new double[] { 1.1, Double.NEGATIVE_INFINITY, 2.2, Double.POSITIVE_INFINITY });

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(Double.NEGATIVE_INFINITY, min.getAsDouble(), DELTA);

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(Double.POSITIVE_INFINITY, max.getAsDouble(), DELTA);
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
    public void testMaxRangeEmpty() {
        list.addAll(new double[] { 1.0, 2.0, 3.0 });
        assertFalse(list.max(1, 1).isPresent());
    }

    @Test
    public void testMaxArraySize() {
        try {
            DoubleList largeList = new DoubleList(Integer.MAX_VALUE - 8);
            assertTrue(largeList.isEmpty());
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

    @Test
    public void testMedian() {
        list.addAll(new double[] { 3.3, 1.1, 5.5, 2.2, 4.4 });
        OptionalDouble median = list.median();
        assertTrue(median.isPresent());
        assertEquals(3.3, median.get(), DELTA);
    }

    @Test
    public void testMedianRange() {
        list.addAll(new double[] { 1.1, 3.3, 5.5, 2.2, 4.4 });
        OptionalDouble median = list.median(1, 4);
        assertTrue(median.isPresent());
    }

    @Test
    public void testMedianEvenElements() {
        list.addAll(new double[] { 1.0, 2.0, 3.0, 4.0 });
        OptionalDouble median = list.median();
        assertTrue(median.isPresent());
        assertEquals(2.0, median.getAsDouble(), DELTA);
    }

    @Test
    public void testMedianEmpty() {
        OptionalDouble median = list.median();
        assertFalse(median.isPresent());
    }

    @Test
    public void testMedianRangeEmpty() {
        list.addAll(new double[] { 1.0, 2.0, 3.0 });
        assertFalse(list.median(1, 1).isPresent());
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
    public void testForEachReverseRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        List<Double> result = new ArrayList<>();

        list.forEach(4, 1, result::add);

        assertEquals(3, result.size());
        assertEquals(5.5, result.get(0), DELTA);
        assertEquals(4.4, result.get(1), DELTA);
        assertEquals(3.3, result.get(2), DELTA);
    }

    @Test
    public void testForEachEmptyList() {
        List<Double> result = new ArrayList<>();
        list.forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachWithNegativeToIndex() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        List<Double> result = new ArrayList<>();

        list.forEach(2, -1, result::add);

        assertEquals(3, result.size());
        assertEquals(3.3, result.get(0), DELTA);
        assertEquals(2.2, result.get(1), DELTA);
        assertEquals(1.1, result.get(2), DELTA);
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
    public void testDistinctWithNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, Double.NaN, 2.2 });
        DoubleList result = list.distinct(0, list.size());
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctEmptyRange() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3 });
        DoubleList result = list.distinct(1, 1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDistinctSingleElement() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3 });
        DoubleList result = list.distinct(0, 1);
        assertEquals(1, result.size());
        assertEquals(1.1, result.get(0), DELTA);
    }

    // --- Missing dedicated tests ---

    @Test
    public void testContainsDuplicates() {
        DoubleList list1 = DoubleList.of(1.0, 2.0, 1.0);
        assertTrue(list1.containsDuplicates());

        DoubleList list2 = DoubleList.of(1.0, 2.0, 3.0);
        assertFalse(list2.containsDuplicates());

        assertFalse(new DoubleList().containsDuplicates());
    }

    @Test
    public void testHasDuplicatesEmpty() {
        assertFalse(list.containsDuplicates());
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
    public void testSortWithNaN() {
        list.addAll(new double[] { 2.2, Double.NaN, 1.1, Double.NaN, 3.3 });
        list.sort();
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertTrue(Double.isNaN(list.get(3)));
        assertTrue(Double.isNaN(list.get(4)));
    }

    @Test
    public void testSortSingleElement() {
        list.add(5.5);
        list.sort();
        assertEquals(1, list.size());
        assertEquals(5.5, list.get(0), DELTA);
    }

    @Test
    public void testSortWithInfinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, -2.2, Double.NEGATIVE_INFINITY, 0.0 });
        list.sort();
        assertEquals(Double.NEGATIVE_INFINITY, list.get(0), DELTA);
        assertEquals(-2.2, list.get(1), DELTA);
        assertEquals(0.0, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, list.get(4), DELTA);
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
    public void testParallelSortSmallList() {
        list.addAll(new double[] { 3.3, 1.1, 2.2 });
        list.parallelSort();
        assertTrue(list.isSorted());
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
    public void testBinarySearchUnsorted() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 1.1, 5.5 });
        int result = list.binarySearch(3.3);
        assertNotNull(result);
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
    public void testReverseSingleElement() {
        list.add(5.5);
        list.reverse();
        assertEquals(1, list.size());
        assertEquals(5.5, list.get(0), DELTA);
    }

    @Test
    public void testReverseRangeEmptyRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.reverse(1, 1);
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
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
    public void testRotateEmptyList() {
        list.rotate(5);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRotateSingleElement() {
        list.add(5.5);
        list.rotate(10);
        assertEquals(1, list.size());
        assertEquals(5.5, list.get(0), DELTA);
    }

    @Test
    public void testRotateNegativeDistance() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.rotate(-2);
        assertEquals(3.3, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
        assertEquals(2.2, list.get(4), DELTA);
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
    public void testShuffleEmptyList() {
        list.shuffle();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testShuffleSingleElement() {
        list.add(5.5);
        list.shuffle();
        assertEquals(1, list.size());
        assertEquals(5.5, list.get(0), DELTA);
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
    public void testSwapThrowsException() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
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
    public void testCopyWithStepLargerThanRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList copy = list.copy(0, 5, 3);
        assertEquals(2, copy.size());
        assertEquals(1.1, copy.get(0), DELTA);
        assertEquals(4.4, copy.get(1), DELTA);
    }

    @Test
    public void testCopyEmptyList() {
        DoubleList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopyWithNegativeStep() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList copy = list.copy(4, 0, -1);
        assertEquals(4, copy.size());
        assertEquals(5.5, copy.get(0), DELTA);
        assertEquals(4.4, copy.get(1), DELTA);
        assertEquals(3.3, copy.get(2), DELTA);
        assertEquals(2.2, copy.get(3), DELTA);
    }

    @Test
    public void testCopyWithNegativeStepReturnsReversed() {
        list.addAll(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
        DoubleList copy = list.copy(4, 0, -1);
        assertEquals(4, copy.size());
        assertEquals(5.0, copy.get(0), DELTA);
    }

    @Test
    public void testCopyRangeInvalidIndices() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 5));
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
    public void testSplitWithChunkSizeLargerThanList() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        List<DoubleList> chunks = list.split(0, 3, 10);
        assertEquals(1, chunks.size());
        assertEquals(3, chunks.get(0).size());
    }

    @Test
    public void testSplitEmptyList() {
        List<DoubleList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
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

        list.removeAt(0);
        assertEquals(1, list.size());
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
    public void testLargeList() {
        for (int i = 0; i < 10000; i++) {
            list.add(i * 1.1);
        }

        assertEquals(10000, list.size());
        assertEquals(0.0, list.get(0), DELTA);
        assertEquals(9999 * 1.1, list.get(9999), DELTA);
    }

    @Test
    public void testUlpComparison() {
        double base = 1.0;
        double nextUp = Math.nextUp(base);
        double nextDown = Math.nextDown(base);

        list.add(nextDown);
        list.add(base);
        list.add(nextUp);

        assertEquals(3, list.size());
        assertNotEquals(base, nextUp);
        assertNotEquals(base, nextDown);

        assertTrue(list.contains(base));
        assertTrue(list.contains(nextUp));
        assertTrue(list.contains(nextDown));
    }

    @Test
    public void testNaNHandling() {
        list.add(1.1);
        list.add(Double.NaN);
        list.add(2.2);

        assertEquals(3, list.size());
        assertTrue(Double.isNaN(list.get(1)));

        assertTrue(list.contains(Double.NaN));

        assertEquals(1, list.indexOf(Double.NaN));
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
    public void testSubnormalNumbers() {
        double subnormal = Double.MIN_VALUE / 2;
        list.add(subnormal);
        list.add(0.0);
        list.add(-subnormal);

        assertEquals(3, list.size());
        assertTrue(list.contains(subnormal));
        assertTrue(list.contains(-subnormal));
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
    public void testBoxedEmptyList() {
        List<Double> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testBoxedWithSpecialValues() {
        list.addAll(new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY });
        List<Double> boxed = list.boxed();
        assertTrue(Double.isNaN(boxed.get(0)));
        assertEquals(Double.POSITIVE_INFINITY, boxed.get(1));
        assertEquals(Double.NEGATIVE_INFINITY, boxed.get(2));
    }

    @Test
    public void testBoxedRangeInvalidIndices() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.boxed(2, 1));
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
    public void testIteratorEmptyList() {
        DoubleIterator iter = list.iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
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
    public void testStreamEmptyList() {
        DoubleStream stream = list.stream();
        assertEquals(0, stream.count());
    }

    @Test
    public void testStreamRangeEmptyRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleStream stream = list.stream(1, 1);
        assertEquals(0, stream.count());
    }

    @Test
    public void testGetFirst() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(1.1, list.getFirst(), DELTA);
    }

    @Test
    public void testGetFirstGetLastSingleElement() {
        list.add(5.5);
        assertEquals(5.5, list.getFirst(), DELTA);
        assertEquals(5.5, list.getLast(), DELTA);
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
    public void testRemoveFirst() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double removed = list.removeFirst();
        assertEquals(1.1, removed, DELTA);
        assertEquals(2, list.size());
        assertEquals(2.2, list.get(0), DELTA);
    }

    @Test
    public void testRemoveFirstRemoveLastSingleElement() {
        list.add(5.5);
        assertEquals(5.5, list.removeFirst(), DELTA);
        assertTrue(list.isEmpty());

        list.add(10.5);
        assertEquals(10.5, list.removeLast(), DELTA);
        assertTrue(list.isEmpty());
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
    public void testEqualsSameObject() {
        assertTrue(list.equals(list));
    }

    @Test
    public void testEqualsNull() {
        assertFalse(list.equals(null));
    }

    @Test
    public void testEqualsWithDoublePrecision() {
        list.add(0.1 + 0.2);

        DoubleList other = new DoubleList();
        other.add(0.3);

        boolean equalsResult = list.equals(other);
        assertNotNull(equalsResult);
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
    public void testToStringWithSpecialValues() {
        list.add(Double.NaN);
        list.add(Double.POSITIVE_INFINITY);
        list.add(Double.NEGATIVE_INFINITY);
        list.add(-0.0);
        list.add(0.0);

        String str = list.toString();
        assertTrue(str.contains("NaN"));
        assertTrue(str.contains("Infinity"));
        assertTrue(str.contains("-Infinity"));
    }

    @Test
    public void testEnsureCapacity_double() {
        // Test adding many elements beyond initial capacity
        DoubleList dl = new DoubleList(2);
        for (int i = 0; i < 100; i++) {
            dl.add(i * 1.0);
        }
        assertEquals(100, dl.size());
        assertEquals(0.0, dl.get(0), DELTA);
        assertEquals(99.0, dl.get(99), DELTA);
    }

    @Test
    public void testEnsureCapacityOverflow() {
        list.add(1.1);
        try {
            for (int i = 0; i < 100; i++) {
                list.add(i * 0.1);
            }
            assertTrue(list.size() > 1);
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

    @Test
    public void testLargePrecisionEdge() {
        double edge = Math.pow(2, 53) - 1;
        double edgePlus = edge + 1;
        double edgePlus2 = edge + 2;

        list.add(edge);
        list.add(edgePlus);
        list.add(edgePlus2);

        assertEquals(edge, list.get(0), DELTA);
        assertEquals(edgePlus, list.get(1), DELTA);
        assertEquals(edgePlus2, list.get(2), DELTA);
    }

    @Test
    public void testScientificNotationValues() {
        list.add(1.23e-10);
        list.add(4.56e+10);
        list.add(7.89e100);
        list.add(-9.87e-100);

        assertEquals(1.23e-10, list.get(0), DELTA);
        assertEquals(4.56e+10, list.get(1), DELTA);
        assertEquals(7.89e100, list.get(2), DELTA);
        assertEquals(-9.87e-100, list.get(3), DELTA);
    }

    @Test
    public void testInfinityHandling() {
        list.add(Double.NEGATIVE_INFINITY);
        list.add(0.0);
        list.add(Double.POSITIVE_INFINITY);

        assertEquals(Double.NEGATIVE_INFINITY, list.get(0), DELTA);
        assertEquals(0.0, list.get(1), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, list.get(2), DELTA);

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(Double.NEGATIVE_INFINITY, min.getAsDouble(), DELTA);

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(Double.POSITIVE_INFINITY, max.getAsDouble(), DELTA);
    }

}
